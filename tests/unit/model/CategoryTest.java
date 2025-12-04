package unit.model;

import model.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование перечисления Category
 */
@DisplayName("Тестирование Category")
class CategoryTest {
    
    @Test
    @DisplayName("Проверка всех значений перечисления")
    void testAllValuesExist() {
        Category[] values = Category.values();
        assertEquals(4, values.length, "Должно быть 4 категории");
        
        assertArrayEquals(new Category[]{Category.WORK, Category.HOME, Category.STUDY, Category.OTHER}, values);
    }
    
    @ParameterizedTest
    @EnumSource(Category.class)
    @DisplayName("Проверка отображаемых имен для всех категорий")
    void testGetDisplayName(Category category) {
        assertNotNull(category.getDisplayName());
        assertFalse(category.getDisplayName().isEmpty());
        
        switch (category) {
            case WORK:
                assertEquals("Работа", category.getDisplayName());
                break;
            case HOME:
                assertEquals("Дом", category.getDisplayName());
                break;
            case STUDY:
                assertEquals("Учёба", category.getDisplayName());
                break;
            case OTHER:
                assertEquals("Другое", category.getDisplayName());
                break;
        }
    }
    
    @ParameterizedTest
    @EnumSource(Category.class)
    @DisplayName("Проверка цветов для всех категорий")
    void testGetColor(Category category) {
        String color = category.getColor();
        assertNotNull(color);
        assertTrue(color.startsWith("#"), "Цвет должен быть в HEX формате");
        assertEquals(7, color.length(), "HEX цвет должен быть 7 символов (#RRGGBB)");
        
        switch (category) {
            case WORK:
                assertEquals("#3D5AFE", color);
                break;
            case HOME:
                assertEquals("#FF4081", color);
                break;
            case STUDY:
                assertEquals("#6200EA", color);
                break;
            case OTHER:
                assertEquals("#757575", color);
                break;
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"Работа", "работа", "РАБОТА", "Дом", "дом", "Учёба", "учёба", "Другое", "другое"})
    @DisplayName("Преобразование строки в Category (регистронезависимое)")
    void testFromDisplayNameValid(String displayName) {
        Category result = Category.fromDisplayName(displayName);
        assertNotNull(result);
        
        String lowerName = displayName.toLowerCase();
        if (lowerName.contains("работа")) {
            assertEquals(Category.WORK, result);
        } else if (lowerName.contains("дом")) {
            assertEquals(Category.HOME, result);
        } else if (lowerName.contains("учёба")) {
            assertEquals(Category.STUDY, result);
        } else if (lowerName.contains("другое")) {
            assertEquals(Category.OTHER, result);
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "неизвестно", "категория", "123", "test"})
    @DisplayName("Преобразование некорректной строки в Category")
    void testFromDisplayNameInvalid(String invalidName) {
        assertEquals(Category.OTHER, Category.fromDisplayName(invalidName));
    }
    
    @Test
    @DisplayName("Проверка уникальности отображаемых имен")
    void testDisplayNameUniqueness() {
        // Проверяем что все отображаемые имена уникальны
        long uniqueCount = java.util.Arrays.stream(Category.values())
            .map(Category::getDisplayName)
            .distinct()
            .count();
        
        assertEquals(Category.values().length, uniqueCount, "Все отображаемые имена должны быть уникальными");
    }
    
    @Test
    @DisplayName("Проверка сравнения категорий")
    void testCategoryComparison() {
        // Проверяем что категории можно сравнивать по порядку
        assertTrue(Category.WORK.ordinal() < Category.HOME.ordinal());
        assertTrue(Category.HOME.ordinal() < Category.STUDY.ordinal());
        assertTrue(Category.STUDY.ordinal() < Category.OTHER.ordinal());
    }
}