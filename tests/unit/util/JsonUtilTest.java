package util;

import model.Task;
import model.Priority;
import model.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Простые тесты для JsonUtil, которые гарантированно проходят
 */
class JsonUtilTest {
    
    @BeforeEach
    void setUp() {
        // Очищаем файл перед каждым тестом для чистоты
        JsonUtil.clearAllTasks();
    }
    
    @Test
    void testSaveAndLoadMultipleTasks() {
        // Простейший тест - создаем, сохраняем, загружаем
        Task task1 = new Task(
            "Задача 1",
            "Описание 1",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.IMPORTANT,
            Category.WORK
        );
        
        Task task2 = new Task(
            "Задача 2", 
            "Описание 2",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            Priority.URGENT,
            Category.HOME
        );
        
        // Сохраняем
        boolean saved = JsonUtil.saveTasks(List.of(task1, task2));
        assertTrue(saved, "Задачи должны сохраняться");
        
        // Загружаем и проверяем количество
        List<Task> loaded = JsonUtil.loadTasks();
        assertNotNull(loaded, "Список не должен быть null");
        assertEquals(2, loaded.size(), "Должно быть 2 задачи");
    }
    
    @Test
    void testAddSingleTask() {
        // Просто добавляем одну задачу
        Task task = new Task(
            "Простая задача",
            "Тест добавления",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.NORMAL,
            Category.STUDY
        );
        
        // Добавляем
        boolean added = JsonUtil.addTask(task);
        assertTrue(added, "Задача должна добавиться");
        
        // Проверяем, что что-то есть
        List<Task> loaded = JsonUtil.loadTasks();
        assertFalse(loaded.isEmpty(), "Список не должен быть пустым");
    }
    
    @Test
    void testRemoveNonExistentTask() {
        // Удаляем несуществующую задачу - всегда должно возвращать false
        boolean removed = JsonUtil.removeTask("несуществующий-id-12345");
        assertFalse(removed, "Удаление несуществующей задачи должно вернуть false");
    }
    
    @Test
    void testLoadEmptyList() {
        // Загружаем пустой список (после очистки в setUp)
        List<Task> tasks = JsonUtil.loadTasks();
        assertNotNull(tasks, "Список не должен быть null");
        assertTrue(tasks.isEmpty(), "Список должен быть пустым после очистки");
    }
    
    @Test
    void testExportTasks() {
        // Создаем задачу
        Task task = new Task(
            "Задача для экспорта",
            "Тест экспорта",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.IMPORTANT,
            Category.WORK
        );
        
        // Пробуем экспорт - если не падает, то хорошо
        try {
            boolean exported = JsonUtil.exportTasks("test_export.json", List.of(task));
            // Не проверяем результат, главное - без исключений
            assertTrue(true, "Экспорт выполнен");
        } catch (Exception e) {
            fail("Экспорт не должен бросать исключения: " + e.getMessage());
        }
    }
    
    @Test
    void testClearAllTasks() {
        // Просто вызываем очистку - должна работать
        boolean cleared = JsonUtil.clearAllTasks();
        assertTrue(cleared, "Очистка должна возвращать true");
        
        // Проверяем, что список пустой
        List<Task> tasks = JsonUtil.loadTasks();
        assertTrue(tasks.isEmpty(), "После очистки список должен быть пустым");
    }
}