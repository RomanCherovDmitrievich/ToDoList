package unit.model;

import model.Priority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование перечисления Priority
 */
@DisplayName("Тестирование Priority")
class PriorityTest {
    
    @Test
    @DisplayName("Проверка всех значений перечисления")
    void testAllValuesExist() {
        // Проверяем что все ожидаемые значения существуют
        Priority[] values = Priority.values();
        assertEquals(3, values.length, "Должно быть 3 значения приоритета");
        
        assertArrayEquals(new Priority[]{Priority.URGENT, Priority.IMPORTANT, Priority.NORMAL}, values);
    }
    
    @ParameterizedTest
    @EnumSource(Priority.class)
    @DisplayName("Проверка отображаемых имен для всех приоритетов")
    void testGetDisplayName(Priority priority) {
        // Проверяем что у каждого приоритета есть читаемое имя
        assertNotNull(priority.getDisplayName());
        assertFalse(priority.getDisplayName().isEmpty());
        
        switch (priority) {
            case URGENT:
                assertEquals("Срочно", priority.getDisplayName());
                break;
            case IMPORTANT:
                assertEquals("Важно", priority.getDisplayName());
                break;
            case NORMAL:
                assertEquals("Желательно", priority.getDisplayName());
                break;
        }
    }
    
    @ParameterizedTest
    @EnumSource(Priority.class)
    @DisplayName("Проверка цветов для всех приоритетов")
    void testGetColor(Priority priority) {
        // Проверяем что у каждого приоритета есть цвет в HEX формате
        String color = priority.getColor();
        assertNotNull(color);
        assertTrue(color.startsWith("#"), "Цвет должен быть в HEX формате");
        assertEquals(7, color.length(), "HEX цвет должен быть 7 символов (#RRGGBB)");
        
        switch (priority) {
            case URGENT:
                assertEquals("#FF4444", color);
                break;
            case IMPORTANT:
                assertEquals("#FFBB33", color);
                break;
            case NORMAL:
                assertEquals("#00C851", color);
                break;
        }
    }
    
    @Test
    @DisplayName("Проверка приоритета по умолчанию")
    void testGetDefault() {
        // Проверяем что по умолчанию используется IMPORTANT
        assertEquals(Priority.IMPORTANT, Priority.getDefault());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"Срочно", "срочно", "СРОЧНО", "Важно", "важно", "Желательно", "желательно"})
    @DisplayName("Преобразование строки в Priority (регистронезависимое)")
    void testFromDisplayNameValid(String displayName) {
        // Проверяем корректное преобразование строк в Priority
        Priority result = Priority.fromDisplayName(displayName);
        assertNotNull(result);
        
        String lowerName = displayName.toLowerCase();
        if (lowerName.contains("срочно")) {
            assertEquals(Priority.URGENT, result);
        } else if (lowerName.contains("важно")) {
            assertEquals(Priority.IMPORTANT, result);
        } else if (lowerName.contains("желательно")) {
            assertEquals(Priority.NORMAL, result);
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "неизвестно", "high", "low", "123", "приоритет"})
    @DisplayName("Преобразование некорректной строки в Priority")
    void testFromDisplayNameInvalid(String invalidName) {
        // Проверяем что некорректные строки возвращают значение по умолчанию
        assertEquals(Priority.IMPORTANT, Priority.fromDisplayName(invalidName));
    }
    
    @Test
    @DisplayName("Проверка имени иконки")
    void testGetIconName() {
        assertEquals("urgent", Priority.URGENT.getIconName());
        assertEquals("important", Priority.IMPORTANT.getIconName());
        assertEquals("normal", Priority.NORMAL.getIconName());
    }
    
    @Test
    @DisplayName("Проверка сравнения приоритетов")
    void testPriorityComparison() {
        // Проверяем что приоритеты можно сравнивать
        assertTrue(Priority.URGENT.ordinal() < Priority.IMPORTANT.ordinal());
        assertTrue(Priority.IMPORTANT.ordinal() < Priority.NORMAL.ordinal());
    }
}