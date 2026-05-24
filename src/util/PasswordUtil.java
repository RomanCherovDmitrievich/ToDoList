package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Утилиты для безопасной работы с паролями.
 */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 65_536;
    private static final int RESET_CODE_LENGTH = 6;

    private PasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String password, String base64Salt) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не должен быть пустым.");
        }
        if (base64Salt == null || base64Salt.isBlank()) {
            throw new IllegalArgumentException("Соль пароля не задана.");
        }

        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось вычислить хеш пароля.", e);
        }
    }

    public static boolean verifyPassword(String password, String base64Salt, String expectedHash) {
        if (password == null || expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        String actualHash = hashPassword(password, base64Salt);
        return MessageDigest.isEqual(actualHash.getBytes(), expectedHash.getBytes());
    }

    public static String generateResetCode() {
        StringBuilder builder = new StringBuilder(RESET_CODE_LENGTH);
        for (int i = 0; i < RESET_CODE_LENGTH; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
