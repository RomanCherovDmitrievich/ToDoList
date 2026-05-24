package unit.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.DatabaseManager;
import util.AuthService;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование AuthService")
class AuthServiceTest {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final AuthService authService = AuthService.getInstance();

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("todolist.email.mode", "disabled");
        authService.logout();
        clearDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        authService.logout();
        clearDatabase();
        System.clearProperty("todolist.email.mode");
    }

    @Test
    @DisplayName("Регистрация и вход по логину и email")
    void testRegisterAndLogin() {
        assertFalse(authService.hasUsers());

        authService.register("tester", "tester@example.com", "secret123");
        assertTrue(authService.hasUsers());

        authService.logout();
        assertDoesNotThrow(() -> authService.login("tester", "secret123"));

        authService.logout();
        assertDoesNotThrow(() -> authService.login("tester@example.com", "secret123"));
        assertThrows(IllegalArgumentException.class, () -> authService.login("tester", "wrong"));
    }

    @Test
    @DisplayName("Сброс пароля по коду восстановления")
    void testResetPasswordFlow() {
        authService.register("recover", "recover@example.com", "secret123");
        authService.logout();

        AuthService.PasswordResetRequest request = authService.requestPasswordReset("recover@example.com");
        assertTrue(request.created());
        assertNotNull(request.expiresAt());

        String resetCode = dbManager.findUserByIdentifier("recover@example.com").getResetCode();
        assertNotNull(resetCode);

        assertDoesNotThrow(() -> authService.resetPassword("recover", resetCode, "new-secret123"));
        assertThrows(IllegalArgumentException.class, () -> authService.login("recover", "secret123"));
        assertDoesNotThrow(() -> authService.login("recover", "new-secret123"));
    }

    private void clearDatabase() throws Exception {
        try (Connection connection = dbManager.getConfig().openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM task_statistics");
            statement.executeUpdate("DELETE FROM task_tags");
            statement.executeUpdate("DELETE FROM tasks");
            statement.executeUpdate("DELETE FROM users");
        }
    }
}
