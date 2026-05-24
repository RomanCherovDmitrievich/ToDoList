package model;

import java.time.LocalDateTime;

/**
 * Данные учетной записи пользователя.
 */
public class User {
    private final String id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String passwordSalt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String resetCode;
    private final LocalDateTime resetCodeExpiresAt;

    public User(String id, String username, String email) {
        this(id, username, email, "", "", LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    public User(
        String id,
        String username,
        String email,
        String passwordHash,
        String passwordSalt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String resetCode,
        LocalDateTime resetCodeExpiresAt
    ) {
        this.id = id;
        this.username = username == null ? "" : username.trim();
        this.email = email == null ? "" : email.trim();
        this.passwordHash = passwordHash == null ? "" : passwordHash;
        this.passwordSalt = passwordSalt == null ? "" : passwordSalt;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.resetCode = resetCode;
        this.resetCodeExpiresAt = resetCodeExpiresAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getResetCode() {
        return resetCode;
    }

    public LocalDateTime getResetCodeExpiresAt() {
        return resetCodeExpiresAt;
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    public String getDisplayName() {
        return username == null || username.isBlank() ? "user" : username;
    }
}
