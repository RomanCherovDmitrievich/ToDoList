package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Настройки транспорта для отправки email.
 */
public final class EmailConfig {
    private final Path configFile;
    private final String transportMode;
    private final String fromName;
    private final String fromAddress;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final boolean smtpAuth;
    private final boolean smtpStartTls;
    private final boolean smtpSsl;
    private final int timeoutMillis;

    private EmailConfig(
        Path configFile,
        String transportMode,
        String fromName,
        String fromAddress,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword,
        boolean smtpAuth,
        boolean smtpStartTls,
        boolean smtpSsl,
        int timeoutMillis
    ) {
        this.configFile = configFile;
        this.transportMode = transportMode;
        this.fromName = fromName;
        this.fromAddress = fromAddress;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.smtpAuth = smtpAuth;
        this.smtpStartTls = smtpStartTls;
        this.smtpSsl = smtpSsl;
        this.timeoutMillis = timeoutMillis;
    }

    public static EmailConfig load() {
        Path configFile = PathResolver.getEmailConfigFile();
        Properties properties = new Properties();

        if (Files.exists(configFile)) {
            try (InputStream input = Files.newInputStream(configFile)) {
                properties.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read email config: " + configFile, e);
            }
        }

        applyEnvironmentOverride(properties, "email.transport", "TODOLIST_EMAIL_TRANSPORT");
        applyEnvironmentOverride(properties, "email.fromName", "TODOLIST_EMAIL_FROM_NAME");
        applyEnvironmentOverride(properties, "email.fromAddress", "TODOLIST_EMAIL_FROM_ADDRESS");
        applyEnvironmentOverride(properties, "smtp.host", "TODOLIST_SMTP_HOST");
        applyEnvironmentOverride(properties, "smtp.port", "TODOLIST_SMTP_PORT");
        applyEnvironmentOverride(properties, "smtp.username", "TODOLIST_SMTP_USERNAME");
        applyEnvironmentOverride(properties, "smtp.password", "TODOLIST_SMTP_PASSWORD");
        applyEnvironmentOverride(properties, "smtp.auth", "TODOLIST_SMTP_AUTH");
        applyEnvironmentOverride(properties, "smtp.starttls", "TODOLIST_SMTP_STARTTLS");
        applyEnvironmentOverride(properties, "smtp.ssl", "TODOLIST_SMTP_SSL");
        applyEnvironmentOverride(properties, "smtp.timeoutMillis", "TODOLIST_SMTP_TIMEOUT_MS");

        return fromProperties(properties, configFile);
    }

    public static EmailConfig fromProperties(Properties properties, Path configFile) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(configFile, "configFile");

        String transportMode = read(properties, "email.transport", "auto")
            .trim()
            .toLowerCase(Locale.ROOT);

        return new EmailConfig(
            configFile.toAbsolutePath(),
            transportMode,
            read(properties, "email.fromName", ""),
            read(properties, "email.fromAddress", ""),
            read(properties, "smtp.host", ""),
            parseInt(read(properties, "smtp.port", ""), 587),
            read(properties, "smtp.username", ""),
            read(properties, "smtp.password", ""),
            parseBoolean(read(properties, "smtp.auth", "true"), true),
            parseBoolean(read(properties, "smtp.starttls", "true"), true),
            parseBoolean(read(properties, "smtp.ssl", "false"), false),
            parseInt(read(properties, "smtp.timeoutMillis", ""), 15000)
        );
    }

    public boolean isSmtpConfigured() {
        return !smtpHost.isBlank() && smtpPort > 0 && !resolveFromAddress().isBlank();
    }

    public String resolveFromAddress() {
        return fromAddress.isBlank() ? smtpUsername : fromAddress;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public String getFromName() {
        return fromName;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public boolean isSmtpStartTls() {
        return smtpStartTls;
    }

    public boolean isSmtpSsl() {
        return smtpSsl;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    private static void applyEnvironmentOverride(Properties properties, String key, String envName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value.trim());
        }
    }

    private static String read(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
