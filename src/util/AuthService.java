package util;

import model.User;
import repository.DatabaseManager;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис регистрации, входа и восстановления доступа.
 */
public final class AuthService {
    public record PasswordResetRequest(
        boolean created,
        boolean delivered,
        String message,
        Path fallbackFile,
        LocalDateTime expiresAt
    ) {
    }

    private static AuthService instance;

    private final DatabaseManager dbManager;
    private final EmailNotifier emailNotifier;
    private User currentUser;

    private AuthService() {
        this.dbManager = DatabaseManager.getInstance();
        this.emailNotifier = new EmailNotifier();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public synchronized boolean hasUsers() {
        return dbManager.getUsersCount() > 0;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void logout() {
        currentUser = null;
    }

    public synchronized User login(String identifier, String password) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        validatePassword(password, false);
        User user = resolveUserForLogin(normalizedIdentifier, password);

        currentUser = user;
        return user;
    }

    public synchronized User register(String username, String email, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password, true);

        if (dbManager.findUserByUsername(normalizedUsername) != null) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        User user = dbManager.createUser(
            UUID.randomUUID().toString(),
            normalizedUsername,
            normalizedEmail,
            hash,
            salt
        );
        currentUser = user;
        return user;
    }

    public synchronized PasswordResetRequest requestPasswordReset(String identifier) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        User user = resolveUserForRecovery(normalizedIdentifier);
        if (user == null) {
            return new PasswordResetRequest(false, false, "Пользователь не найден.", null, null);
        }
        if (!user.hasEmail()) {
            return new PasswordResetRequest(
                false,
                false,
                "Для аккаунта не указана почта, поэтому отправка кода недоступна.",
                null,
                null
            );
        }

        String resetCode = PasswordUtil.generateResetCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        dbManager.saveResetCode(user.getId(), resetCode, expiresAt);

        String subject = "ToDoList: код восстановления";
        String body = """
            Для аккаунта %s был запрошен сброс пароля.

            Код восстановления: %s
            Код действует до: %s

            Если это были не вы, просто проигнорируйте письмо.
            """.formatted(user.getUsername(), resetCode, expiresAt);

        EmailNotifier.DeliveryResult delivery = emailNotifier.sendEmailDetailed(user.getEmail(), subject, body);
        if (delivery.delivered()) {
            return new PasswordResetRequest(
                true,
                true,
                delivery.message(),
                null,
                expiresAt
            );
        }

        return new PasswordResetRequest(
            true,
            false,
            delivery.message(),
            delivery.fallbackFile(),
            expiresAt
        );
    }

    public synchronized void resetPassword(String identifier, String resetCode, String newPassword) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        validatePassword(newPassword, true);

        if (resetCode == null || resetCode.isBlank()) {
            throw new IllegalArgumentException("Введите код восстановления.");
        }

        User user = resolveUserForRecovery(normalizedIdentifier);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не найден.");
        }

        LocalDateTime expiresAt = user.getResetCodeExpiresAt();
        if (user.getResetCode() == null || user.getResetCode().isBlank() || expiresAt == null) {
            throw new IllegalArgumentException("Для этого аккаунта не запрошен код восстановления.");
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new IllegalArgumentException("Срок действия кода восстановления истек.");
        }
        if (!user.getResetCode().equals(resetCode.trim())) {
            throw new IllegalArgumentException("Неверный код восстановления.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newPassword, salt);
        dbManager.updateUserCredentials(user.getId(), hash, salt);
        dbManager.clearResetCode(user.getId());
    }

    private String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Укажите логин или email.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeIdentifier(username);
        if (normalized.length() < 3) {
            throw new IllegalArgumentException("Логин должен содержать минимум 3 символа.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new IllegalArgumentException("Введите корректный email.");
        }
        return normalized;
    }

    private void validatePassword(String password, boolean strict) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не должен быть пустым.");
        }
        if (strict && password.trim().length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов.");
        }
    }

    private User resolveUserForLogin(String identifier, String password) {
        if (!looksLikeEmail(identifier)) {
            User user = dbManager.findUserByUsername(identifier);
            if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Неверный логин/email или пароль.");
            }
            return user;
        }

        List<User> users = dbManager.findUsersByEmail(identifier);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("Неверный логин/email или пароль.");
        }

        List<User> matchedUsers = users.stream()
            .filter(user -> PasswordUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash()))
            .collect(Collectors.toList());

        if (matchedUsers.isEmpty()) {
            throw new IllegalArgumentException("Неверный логин/email или пароль.");
        }
        if (matchedUsers.size() > 1) {
            throw new IllegalArgumentException(
                "На эту почту зарегистрировано несколько аккаунтов. Для входа используйте логин."
            );
        }

        return matchedUsers.get(0);
    }

    private User resolveUserForRecovery(String identifier) {
        if (!looksLikeEmail(identifier)) {
            return dbManager.findUserByUsername(identifier);
        }

        List<User> users = dbManager.findUsersByEmail(identifier);
        if (users.size() > 1) {
            throw new IllegalArgumentException(
                "На эту почту зарегистрировано несколько аккаунтов. Для восстановления укажите логин."
            );
        }
        return users.isEmpty() ? null : users.get(0);
    }

    private boolean looksLikeEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}
