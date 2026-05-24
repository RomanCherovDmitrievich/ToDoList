package unit.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.EmailConfig;
import util.EmailNotifier;
import util.PlatformUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование EmailNotifier")
class EmailNotifierTest {

    @BeforeEach
    void setUp() {
        System.clearProperty("todolist.email.mode");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("todolist.email.mode");
    }

    @Test
    @DisplayName("Отключенный режим пишет письмо в outbox")
    void testDisabledModeWritesOutbox() throws Exception {
        System.setProperty("todolist.email.mode", "disabled");
        Path tempDir = Files.createTempDirectory("email-notifier-test");
        Path outbox = tempDir.resolve("notifications_outbox.log");

        TestEmailNotifier notifier = new TestEmailNotifier(outbox, PlatformUtil.OsFamily.MAC);
        EmailNotifier.DeliveryResult result = notifier.sendEmailDetailed(
            "friend@example.com",
            "Reset",
            "Body text"
        );

        assertFalse(result.delivered());
        assertNotNull(result.fallbackFile());
        assertEquals(outbox, result.fallbackFile());
        assertTrue(Files.exists(outbox));
        String saved = Files.readString(outbox);
        assertTrue(saved.contains("friend@example.com"));
        assertTrue(saved.contains("Body text"));
    }

    @Test
    @DisplayName("На macOS при успехе используется Mail.app")
    void testMacUsesMailAppResult() {
        Path outbox = Path.of("build", "unused-notifications.log");
        TestEmailNotifier notifier = new TestEmailNotifier(outbox, PlatformUtil.OsFamily.MAC);
        notifier.macResult = new EmailNotifier.DeliveryResult(true, "Mail.app success", null);

        EmailNotifier.DeliveryResult result = notifier.sendEmailDetailed("user@example.com", "Subject", "Body");

        assertTrue(result.delivered());
        assertEquals("Mail.app success", result.message());
        assertTrue(notifier.macCalled);
        assertFalse(notifier.mailCalled);
    }

    @Test
    @DisplayName("При наличии SMTP-конфига используется SMTP")
    void testSmtpTakesPriorityInAutoMode() {
        Path outbox = Path.of("build", "unused-notifications.log");
        TestEmailNotifier notifier = new TestEmailNotifier(outbox, PlatformUtil.OsFamily.MAC);
        notifier.emailConfig = configuredSmtp();
        notifier.smtpResult = new EmailNotifier.DeliveryResult(true, "SMTP success", null);

        EmailNotifier.DeliveryResult result = notifier.sendEmailDetailed("user@example.com", "Subject", "Body");

        assertTrue(result.delivered());
        assertEquals("SMTP success", result.message());
        assertTrue(notifier.smtpCalled);
        assertFalse(notifier.macCalled);
        assertFalse(notifier.mailCalled);
    }

    private EmailConfig configuredSmtp() {
        Properties properties = new Properties();
        properties.setProperty("email.transport", "auto");
        properties.setProperty("email.fromAddress", "noreply@example.com");
        properties.setProperty("smtp.host", "smtp.example.com");
        properties.setProperty("smtp.port", "587");
        properties.setProperty("smtp.username", "noreply@example.com");
        properties.setProperty("smtp.password", "secret");
        return EmailConfig.fromProperties(properties, Path.of("email.properties"));
    }

    private static final class TestEmailNotifier extends EmailNotifier {
        private final Path outboxPath;
        private final PlatformUtil.OsFamily osFamily;
        private boolean macCalled;
        private boolean mailCalled;
        private boolean smtpCalled;
        private EmailConfig emailConfig = EmailConfig.fromProperties(new Properties(), Path.of("email.properties"));
        private DeliveryResult macResult = new DeliveryResult(false, "Mail.app unavailable", null);
        private DeliveryResult smtpResult = new DeliveryResult(false, "SMTP unavailable", null);

        private TestEmailNotifier(Path outboxPath, PlatformUtil.OsFamily osFamily) {
            this.outboxPath = outboxPath;
            this.osFamily = osFamily;
        }

        @Override
        protected PlatformUtil.OsFamily detectOs() {
            return osFamily;
        }

        @Override
        protected DeliveryResult sendViaMacMailApp(String to, String subject, String body) {
            macCalled = true;
            return macResult;
        }

        @Override
        protected DeliveryResult sendViaMail(String to, String subject, String body) {
            mailCalled = true;
            return new DeliveryResult(true, "mail success", null);
        }

        @Override
        protected DeliveryResult sendViaSmtp(EmailConfig config, String to, String subject, String body) {
            smtpCalled = true;
            return smtpResult;
        }

        @Override
        protected EmailConfig loadEmailConfig() {
            return emailConfig;
        }

        @Override
        protected Path getNotificationsLogPath() {
            return outboxPath;
        }
    }
}
