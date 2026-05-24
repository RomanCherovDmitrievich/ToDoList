package unit.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.EmailConfig;

import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование EmailConfig")
class EmailConfigTest {

    @Test
    @DisplayName("SMTP-конфиг корректно читается из properties")
    void testFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("email.transport", "smtp");
        properties.setProperty("email.fromName", "ToDoList Service");
        properties.setProperty("email.fromAddress", "noreply@example.com");
        properties.setProperty("smtp.host", "smtp.example.com");
        properties.setProperty("smtp.port", "465");
        properties.setProperty("smtp.username", "mailer@example.com");
        properties.setProperty("smtp.password", "secret");
        properties.setProperty("smtp.auth", "true");
        properties.setProperty("smtp.starttls", "false");
        properties.setProperty("smtp.ssl", "true");
        properties.setProperty("smtp.timeoutMillis", "9000");

        EmailConfig config = EmailConfig.fromProperties(properties, Path.of("email.properties"));

        assertEquals("smtp", config.getTransportMode());
        assertEquals("ToDoList Service", config.getFromName());
        assertEquals("noreply@example.com", config.getFromAddress());
        assertEquals("smtp.example.com", config.getSmtpHost());
        assertEquals(465, config.getSmtpPort());
        assertEquals("mailer@example.com", config.getSmtpUsername());
        assertEquals("secret", config.getSmtpPassword());
        assertTrue(config.isSmtpAuth());
        assertFalse(config.isSmtpStartTls());
        assertTrue(config.isSmtpSsl());
        assertEquals(9000, config.getTimeoutMillis());
        assertTrue(config.isSmtpConfigured());
    }
}
