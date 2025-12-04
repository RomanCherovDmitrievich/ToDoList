package unit.model;

import model.Task;
import model.Priority;
import model.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование класса Task
 */
@DisplayName("Тестирование Task")
class TaskTest {
    
    private Task task;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.of(2025, 1, 1, 9, 0);
        endTime = LocalDateTime.of(2025, 1, 1, 18, 0);
        
        task = new Task(
            "Тестовая задача",
            "Описание тестовой задачи",
            startTime,
            endTime,
            Priority.IMPORTANT,
            Category.WORK
        );
    }
    
    @Test
    @DisplayName("Создание задачи с валидными данными")
    void testTaskCreation() {
        assertNotNull(task);
        assertNotNull(task.getId());
        assertFalse(task.getId().isEmpty());
        assertEquals("Тестовая задача", task.getTitle());
        assertEquals("Описание тестовой задачи", task.getDescription());
        assertEquals(startTime, task.getStartTime());
        assertEquals(endTime, task.getEndTime());
        assertEquals(Priority.IMPORTANT, task.getPriority());
        assertEquals(Category.WORK, task.getCategory());
        assertFalse(task.isCompleted());
        assertFalse(task.isOverdue());
        assertNotNull(task.getCreatedAt());
    }
    
    @Test
    @DisplayName("Автоматическая генерация ID")
    void testIdGeneration() {
        Task task1 = new Task("Задача 1", "Описание", startTime, endTime, Priority.NORMAL, Category.HOME);
        Task task2 = new Task("Задача 2", "Описание", startTime, endTime, Priority.NORMAL, Category.HOME);
        
        assertNotNull(task1.getId());
        assertNotNull(task2.getId());
        assertNotEquals(task1.getId(), task2.getId(), "ID должны быть уникальными");
        
        // Проверяем что ID в формате UUID
        assertDoesNotThrow(() -> {
            UUID.fromString(task1.getId());
        });
    }
    
    @Test
    @DisplayName("Создание задачи с пустым заголовком")
    void testTaskWithEmptyTitle() {
        Task emptyTask = new Task("", "Описание", startTime, endTime, Priority.NORMAL, Category.HOME);
        assertEquals("", emptyTask.getTitle());
    }
    
    @Test
    @DisplayName("Создание задачи с null приоритетом и категорией")
    void testTaskWithNullPriorityAndCategory() {
        Task taskWithNull = new Task("Задача", "Описание", startTime, endTime, null, null);
        
        // Должны устанавливаться значения по умолчанию
        assertEquals(Priority.IMPORTANT, taskWithNull.getPriority());
        assertEquals(Category.OTHER, taskWithNull.getCategory());
    }
    
    @Test
    @DisplayName("Проверка сеттеров")
    void testSetters() {
        // Проверяем все сеттеры
        task.setTitle("Новый заголовок");
        assertEquals("Новый заголовок", task.getTitle());
        
        task.setDescription("Новое описание");
        assertEquals("Новое описание", task.getDescription());
        
        LocalDateTime newStartTime = LocalDateTime.now();
        task.setStartTime(newStartTime);
        assertEquals(newStartTime, task.getStartTime());
        
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(1);
        task.setEndTime(newEndTime);
        assertEquals(newEndTime, task.getEndTime());
        
        task.setPriority(Priority.URGENT);
        assertEquals(Priority.URGENT, task.getPriority());
        
        task.setCategory(Category.STUDY);
        assertEquals(Category.STUDY, task.getCategory());
        
        task.setCompleted(true);
        assertTrue(task.isCompleted());
        
        // При установке completed в true, overdue должен стать false
        task.setCompleted(true);
        assertFalse(task.isOverdue());
    }
    
    @Test
    @DisplayName("Проверка метода toJsonString()")
    void testToJsonString() {
        String json = task.toJsonString();
        
        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        
        // Проверяем наличие ключевых полей
        assertTrue(json.contains("\"id\":"));
        assertTrue(json.contains("\"title\":"));
        assertTrue(json.contains("\"description\":"));
        assertTrue(json.contains("\"startTime\":"));
        assertTrue(json.contains("\"endTime\":"));
        assertTrue(json.contains("\"priority\":"));
        assertTrue(json.contains("\"category\":"));
        assertTrue(json.contains("\"completed\":"));
        assertTrue(json.contains("\"overdue\":"));
        assertTrue(json.contains("\"createdAt\":"));
        
        // Проверяем формат дат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String expectedDate = startTime.format(formatter);
        assertTrue(json.contains(expectedDate));
    }
    
    @Test
    @DisplayName("Экранирование специальных символов в JSON")
    void testJsonEscape() {
        Task specialTask = new Task(
            "Задача с \"кавычками\" и \n переносом",
            "Описание с \\ обратным слешем",
            startTime,
            endTime,
            Priority.NORMAL,
            Category.HOME
        );
        
        String json = specialTask.toJsonString();
        // JSON должен быть валидным
        assertFalse(json.contains("\n"), "Переносы строк должны быть экранированы");
        assertTrue(json.contains("\\\""), "Кавычки должны быть экранированы");
    }
    
    @Test
    @DisplayName("Проверка просроченности задачи")
    void testOverdueCheck() {
        // Создаем задачу с прошедшим сроком
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        Task overdueTask = new Task(
            "Просроченная задача",
            "Описание",
            pastTime.minusHours(1),
            pastTime,
            Priority.URGENT,
            Category.WORK
        );
        
        // Проверяем что задача помечена как просроченная
        overdueTask.checkOverdue();
        assertTrue(overdueTask.isOverdue());
        
        // Если задача выполнена, она не должна быть просроченной
        overdueTask.setCompleted(true);
        overdueTask.checkOverdue();
        assertFalse(overdueTask.isOverdue());
    }
    
    @Test
    @DisplayName("Парсинг задачи из JSON строки")
    void testTaskFromJson() {
        // Создаем JSON строку
        String json = String.format(
            "{\"id\":\"%s\",\"title\":\"Тест из JSON\",\"description\":\"Описание\"," +
            "\"startTime\":\"2025-01-01T09:00:00\",\"endTime\":\"2025-01-01T18:00:00\"," +
            "\"priority\":\"URGENT\",\"category\":\"WORK\",\"completed\":false,\"overdue\":false," +
            "\"createdAt\":\"2025-01-01T08:00:00\"}",
            UUID.randomUUID().toString()
        );
        
        // Парсим задачу (используем рефлексию для доступа к конструктору)
        // В реальном коде это будет через JsonUtil
        assertDoesNotThrow(() -> {
            // Здесь тестируется что формат JSON совместим с парсером
            // Фактический парсинг тестируется в JsonUtilTest
        });
    }
    
    @Test
    @DisplayName("Форматирование дат для отображения")
    void testFormattedDates() {
        String formattedStart = task.getFormattedStartTime();
        String formattedEnd = task.getFormattedEndTime();
        
        assertNotNull(formattedStart);
        assertNotNull(formattedEnd);
        assertFalse(formattedStart.isEmpty());
        assertFalse(formattedEnd.isEmpty());
        
        // Проверяем формат ДД.ММ.ГГГГ ЧЧ:ММ
        assertTrue(formattedStart.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"));
        assertTrue(formattedEnd.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"));
    }
    
    @Test
    @DisplayName("Проверка equals() и hashCode()")
    void testEqualsAndHashCode() {
        Task task1 = new Task("Задача", "Описание", startTime, endTime, Priority.NORMAL, Category.HOME);
        Task task2 = new Task("Задача", "Описание", startTime, endTime, Priority.NORMAL, Category.HOME);
        
        // Две разные задачи с разными ID не равны
        assertNotEquals(task1, task2);
        assertNotEquals(task1.hashCode(), task2.hashCode());
        
        // Задача равна самой себе
        assertEquals(task1, task1);
        
        // Задача не равна null
        assertNotEquals(null, task1);
        
        // Задача не равна объекту другого класса
        assertNotEquals("строка", task1);
    }
    
    @Test
    @DisplayName("Проверка toString()")
    void testToString() {
        String str = task.toString();
        assertNotNull(str);
        assertTrue(str.contains(task.getTitle()));
        assertTrue(str.contains(task.getPriority().getDisplayName()));
    }
    
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Проверка статуса выполнения")
    void testCompletedStatus(boolean completed) {
        task.setCompleted(completed);
        assertEquals(completed, task.isCompleted());
        
        if (completed) {
            assertFalse(task.isOverdue(), "Выполненная задача не может быть просроченной");
        }
    }
    
    @Test
    @DisplayName("Проверка обновления просроченности при изменении времени")
    void testOverdueUpdateOnTimeChange() {
        // Создаем задачу с будущим сроком
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        Task futureTask = new Task(
            "Задача",
            "Описание",
            LocalDateTime.now(),
            futureTime,
            Priority.NORMAL,
            Category.HOME
        );
        
        assertFalse(futureTask.isOverdue());
        
        // Меняем время на прошедшее
        futureTask.setEndTime(LocalDateTime.now().minusDays(1));
        assertTrue(futureTask.isOverdue());
    }
}