package util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class EmailNotifier {
    public record DeliveryResult(boolean delivered, String message, Path fallbackFile) {
    }

    public boolean sendEmail(String to, String subject, String body) {
        return sendEmailDetailed(to, subject, body).delivered();
    }

    public DeliveryResult sendEmailDetailed(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return new DeliveryResult(false, "Email адрес не указан.", null);
        }

        String emailMode = getEmailMode();
        if ("disabled".equals(emailMode) || "outbox".equals(emailMode)) {
            return fallbackResult(
                to,
                subject,
                body,
                "Автоматическая отправка писем отключена для этого режима."
            );
        }

        EmailConfig emailConfig = loadEmailConfig();
        if (shouldUseSmtp(emailMode, emailConfig)) {
            DeliveryResult smtpResult = sendViaSmtp(emailConfig, to, subject, body);
            if (smtpResult.delivered()) {
                return smtpResult;
            }
            if ("smtp".equals(emailMode)) {
                return fallbackResult(to, subject, body, smtpResult.message());
            }
        } else if ("smtp".equals(emailMode)) {
            return fallbackResult(
                to,
                subject,
                body,
                "Выбран SMTP-режим, но файл email.properties заполнен не полностью."
            );
        }

        DeliveryResult result = switch (detectOs()) {
            case MAC -> sendViaMacMailApp(to, subject, body);
            case WINDOWS -> sendViaPowerShell(to, subject, body);
            case REDOS, LINUX, OTHER -> sendViaMail(to, subject, body);
        };

        if (result.delivered()) {
            return result;
        }

        String message = result.message() == null || result.message().isBlank()
            ? "Автоматическая отправка письма не настроена."
            : result.message();
        return fallbackResult(to, subject, body, message);
    }

    protected DeliveryResult sendViaMacMailApp(String to, String subject, String body) {
        if (!isCommandAvailable("osascript")) {
            return new DeliveryResult(false, "На macOS не найден osascript для управления Mail.app.", null);
        }

        ProcessBuilder builder = new ProcessBuilder(
            "osascript",
            "-e", "on run argv",
            "-e", "set recipientAddress to item 1 of argv",
            "-e", "set subjectText to item 2 of argv",
            "-e", "set bodyText to item 3 of argv",
            "-e", "tell application \"Mail\"",
            "-e", "set newMessage to make new outgoing message with properties {subject:subjectText, content:bodyText & return & return, visible:false}",
            "-e", "tell newMessage",
            "-e", "make new to recipient at end of to recipients with properties {address:recipientAddress}",
            "-e", "send",
            "-e", "end tell",
            "-e", "end tell",
            "-e", "end run",
            "--",
            to,
            subject == null ? "" : subject,
            body == null ? "" : body
        );
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new DeliveryResult(
                    true,
                    "Письмо передано в Mail.app. Проверьте папки \"Отправленные\", \"Исходящие\" и \"Спам\".",
                    null
                );
            }
            return new DeliveryResult(false, formatTransportError("Mail.app не смогла отправить письмо.", output), null);
        } catch (Exception e) {
            return new DeliveryResult(false, "Mail.app не смогла отправить письмо: " + e.getMessage(), null);
        }
    }

    protected DeliveryResult sendViaMail(String to, String subject, String body) {
        if (!isCommandAvailable("mail")) {
            return new DeliveryResult(false, "Системная утилита mail не найдена или не настроена.", null);
        }

        ProcessBuilder builder = new ProcessBuilder("mail", "-s", subject == null ? "" : subject, to);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(body == null ? "" : body);
                writer.write(System.lineSeparator());
            }
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new DeliveryResult(
                    true,
                    "Письмо передано системной утилите mail. Доставка зависит от настройки почты на этой системе.",
                    null
                );
            }
            return new DeliveryResult(false, formatTransportError("Команда mail завершилась с ошибкой.", output), null);
        } catch (Exception e) {
            return new DeliveryResult(false, "Команда mail завершилась с ошибкой: " + e.getMessage(), null);
        }
    }

    protected DeliveryResult sendViaPowerShell(String to, String subject, String body) {
        return new DeliveryResult(
            false,
            "На Windows автоматическая отправка без отдельно настроенного SMTP или Outlook-профиля не поддерживается.",
            null
        );
    }

    protected DeliveryResult sendViaSmtp(EmailConfig config, String to, String subject, String body) {
        try {
            Properties properties = new Properties();
            properties.setProperty("mail.transport.protocol", "smtp");
            properties.setProperty("mail.smtp.host", config.getSmtpHost());
            properties.setProperty("mail.smtp.port", String.valueOf(config.getSmtpPort()));
            properties.setProperty("mail.smtp.auth", String.valueOf(config.isSmtpAuth()));
            properties.setProperty("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStartTls()));
            properties.setProperty("mail.smtp.ssl.enable", String.valueOf(config.isSmtpSsl()));
            properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(config.getTimeoutMillis()));
            properties.setProperty("mail.smtp.timeout", String.valueOf(config.getTimeoutMillis()));
            properties.setProperty("mail.smtp.writetimeout", String.valueOf(config.getTimeoutMillis()));

            Session session = Session.getInstance(properties, buildAuthenticator(config));
            MimeMessage message = new MimeMessage(session);
            message.setFrom(buildFromAddress(config));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());
            message.setText(body == null ? "" : body, StandardCharsets.UTF_8.name());

            Transport.send(message);
            return new DeliveryResult(
                true,
                "Письмо отправлено через SMTP-аккаунт " + config.resolveFromAddress() + ".",
                null
            );
        } catch (Exception e) {
            return new DeliveryResult(false, "SMTP отправка завершилась с ошибкой: " + e.getMessage(), null);
        }
    }

    protected PlatformUtil.OsFamily detectOs() {
        return PlatformUtil.detectOs();
    }

    protected boolean isCommandAvailable(String command) {
        try {
            if (detectOs() == PlatformUtil.OsFamily.WINDOWS) {
                Process process = new ProcessBuilder("cmd", "/c", "where " + command).start();
                return process.waitFor() == 0;
            }
            Process process = new ProcessBuilder("bash", "-c", "command -v " + command).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    protected String getEmailMode() {
        String mode = System.getProperty("todolist.email.mode");
        if (mode == null || mode.isBlank()) {
            mode = System.getenv("TODOLIST_EMAIL_MODE");
        }
        if (mode == null || mode.isBlank()) {
            mode = loadEmailConfig().getTransportMode();
        }
        return mode == null || mode.isBlank()
            ? "auto"
            : mode.trim().toLowerCase(Locale.ROOT);
    }

    protected EmailConfig loadEmailConfig() {
        return EmailConfig.load();
    }

    protected Path getNotificationsLogPath() {
        return PathResolver.getNotificationsLog();
    }

    private DeliveryResult fallbackResult(String to, String subject, String body, String message) {
        Path outbox = writeOutbox(to, subject, body);
        return new DeliveryResult(false, message + " Текст письма сохранен в лог уведомлений.", outbox);
    }

    private Path writeOutbox(String to, String subject, String body) {
        Path outbox = getNotificationsLogPath();
        try {
            Files.createDirectories(outbox.getParent());
            try (FileWriter writer = new FileWriter(outbox.toFile(), true)) {
                writer.write("TO: " + to + "\n");
                writer.write("SUBJECT: " + subject + "\n");
                writer.write("BODY: " + body + "\n");
                writer.write("----\n");
            }
        } catch (IOException ignored) {
        }
        return outbox;
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!output.isEmpty()) {
                    output.append(System.lineSeparator());
                }
                output.append(line);
            }
        }
        return output.toString().trim();
    }

    private String formatTransportError(String prefix, String output) {
        if (output == null || output.isBlank()) {
            return prefix;
        }
        return prefix + " " + output.trim();
    }

    private boolean shouldUseSmtp(String emailMode, EmailConfig config) {
        return ("auto".equals(emailMode) || "smtp".equals(emailMode)) && config.isSmtpConfigured();
    }

    private Authenticator buildAuthenticator(EmailConfig config) {
        if (!config.isSmtpAuth()) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
            }
        };
    }

    private InternetAddress buildFromAddress(EmailConfig config) throws Exception {
        String address = config.resolveFromAddress();
        if (config.getFromName().isBlank()) {
            return new InternetAddress(address);
        }
        return new InternetAddress(address, config.getFromName(), StandardCharsets.UTF_8.name());
    }
}
