package util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EmailNotifier {
    public boolean sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return false;
        }

        boolean sent = false;
        PlatformUtil.OsFamily os = PlatformUtil.detectOs();

        if (os == PlatformUtil.OsFamily.WINDOWS) {
            sent = sendViaPowerShell(to, subject, body);
        } else {
            sent = sendViaMail(to, subject, body);
        }

        if (!sent) {
            writeOutbox(to, subject, body);
        }

        return sent;
    }

    private boolean sendViaMail(String to, String subject, String body) {
        if (!isCommandAvailable("mail")) {
            return false;
        }

        ProcessBuilder builder = new ProcessBuilder(
            "bash", "-c",
            "echo \"" + escape(body) + "\" | mail -s \"" + escape(subject) + "\" " + escape(to)
        );

        try {
            Process process = builder.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendViaPowerShell(String to, String subject, String body) {
        if (!isCommandAvailable("powershell")) {
            return false;
        }

        String command = "Send-MailMessage -To \"" + escape(to) + "\" -Subject \"" +
            escape(subject) + "\" -Body \"" + escape(body) + "\"";

        ProcessBuilder builder = new ProcessBuilder("powershell", "-Command", command);
        try {
            Process process = builder.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            if (PlatformUtil.detectOs() == PlatformUtil.OsFamily.WINDOWS) {
                Process process = new ProcessBuilder("cmd", "/c", "where " + command).start();
                return process.waitFor() == 0;
            }
            Process process = new ProcessBuilder("bash", "-c", "command -v " + command).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeOutbox(String to, String subject, String body) {
        Path outbox = PathResolver.getNotificationsLog();
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
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
