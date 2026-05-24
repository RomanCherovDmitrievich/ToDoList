package unit.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.PasswordUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование PasswordUtil")
class PasswordUtilTest {

    @Test
    @DisplayName("Пароль успешно хешируется и проверяется")
    void testHashAndVerifyPassword() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("secret123", salt);

        assertNotNull(hash);
        assertTrue(PasswordUtil.verifyPassword("secret123", salt, hash));
        assertFalse(PasswordUtil.verifyPassword("wrong", salt, hash));
    }

    @Test
    @DisplayName("Код восстановления имеет ожидаемый формат")
    void testGenerateResetCode() {
        String code = PasswordUtil.generateResetCode();

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
    }
}
