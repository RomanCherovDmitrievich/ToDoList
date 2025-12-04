package util;

import model.Task;
import model.Priority;
import model.Category;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
/**
 * Тестирование работы с JSON
 */
public class JsonUtilTest {
    
    @TempDir
    static Path tempDir;
    
    private Path testDataPath;
    private String originalDataDir;
    private String originalTasksFile;
    
    @BeforeEach
    void setUp() {
        // Сохраняем оригинальные настройки
        originalDataDir = "data";
        originalTasksFile = "tasks.json";
        
        // Создаем тестовую директорию
        testDataPath = tempDir.resolve("data");
        try {
            Files.createDirectories(testDataPath);
        } catch (IOException e) {
            fail("Не удалось создать тестовую директорию: " + e.getMessage());
        }
        
        // Устанавливаем тестовый путь
        JsonUtil.setDataPath(testDataPath.toString(), "tasks.json");
    }
    
    @AfterEach
    void tearDown() {
        // Восстанавливаем оригинальные настройки
        JsonUtil.setDataPath(originalDataDir, originalTasksFile);
    }
    
    @Test
    void testSaveAndLoadSingleTask() {
        // Создаем тестовую задачу
        Task task = new Task(
            "Тестовая задача",
            "Описание тестовой задачи",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.IMPORTANT,
            Category.WORK
        );
        
        // Сохраняем
        boolean saved = JsonUtil.saveTasks(Collections.singletonList(task));
        assertTrue(saved, "Задача должна успешно сохраниться");
        
        // Загружаем
        List<Task> loadedTasks = JsonUtil.loadTasks();
        
        // Проверяем
        assertNotNull(loadedTasks, "Список задач не должен быть null");
        assertEquals(1, loadedTasks.size(), "Должна загрузиться одна задача");
        assertEquals(task.getTitle(), loadedTasks.get(0).getTitle(), "Названия задач должны совпадать");
    }
    
    @Test
    void testRemoveTaskFromFile() {
        // Создаем тестовые задачи
        Task task1 = new Task("Задача 1", "Описание 1", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            Priority.URGENT, Category.WORK);
        
        Task task2 = new Task("Задача 2", "Описание 2", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(2),
            Priority.NORMAL, Category.HOME);
        
        // Сохраняем
        List<Task> tasks = Arrays.asList(task1, task2);
        JsonUtil.saveTasks(tasks);
        
        // Удаляем одну задачу
        boolean removed = JsonUtil.removeTask(task1.getId());
        assertTrue(removed, "Задача должна быть удалена");
        
        // Проверяем
        List<Task> remainingTasks = JsonUtil.loadTasks();
        assertEquals(1, remainingTasks.size(), "Должна остаться одна задача");
        assertEquals(task2.getTitle(), remainingTasks.get(0).getTitle());
    }
    
    @Test
    void testUpdateNonexistentTask() {
        // Создаем тестовую задачу
        Task task = new Task("Задача", "Описание", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            Priority.IMPORTANT, Category.WORK);
        
        // Сохраняем
        JsonUtil.saveTasks(Collections.singletonList(task));
        
        // Пытаемся обновить несуществующую задачу
        Task nonExistentTask = new Task(
            "Несуществующая",
            "Нет такой",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.NORMAL,
            Category.OTHER
        );
        
        boolean updated = JsonUtil.updateTask(nonExistentTask);
        assertFalse(updated, "Несуществующая задача не должна обновиться");
    }
    
    @Test
    void testClearTaskFile() {
        // Создаем тестовые задачи
        Task task1 = new Task("Задача 1", "Описание 1", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            Priority.URGENT, Category.WORK);
        
        Task task2 = new Task("Задача 2", "Описание 2", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(2),
            Priority.NORMAL, Category.HOME);
        
        // Сохраняем
        JsonUtil.saveTasks(Arrays.asList(task1, task2));
        
        // Очищаем
        boolean cleared = JsonUtil.clearAllTasks();
        assertTrue(cleared, "Файл должен успешно очиститься");
        
        // Проверяем
        List<Task> tasks = JsonUtil.loadTasks();
        assertTrue(tasks.isEmpty(), "Список задач должен быть пустым после очистки");
    }
    
    @Test
    void testUpdateTaskInFile() {
        // Создаем тестовую задачу
        Task originalTask = new Task("Оригинал", "Описание", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            Priority.IMPORTANT, Category.WORK);
        
        // Сохраняем
        JsonUtil.saveTasks(Collections.singletonList(originalTask));
        
        // Обновляем задачу
        Task updatedTask = new Task(
            originalTask.getId(),
            "Обновленная",
            "Новое описание",
            originalTask.getStartTime().toString(),
            originalTask.getEndTime().plusDays(1).toString(),
            "URGENT",
            "STUDY",
            true,
            false,
            originalTask.getCreatedAt().toString()
        );
        
        boolean updated = JsonUtil.updateTask(updatedTask);
        assertTrue(updated, "Задача должна успешно обновиться");
        
        // Проверяем
        List<Task> tasks = JsonUtil.loadTasks();
        assertEquals(1, tasks.size(), "Должна остаться одна задача");
        assertEquals("Обновленная", tasks.get(0).getTitle());
        assertEquals("URGENT", tasks.get(0).getPriority().name());
    }
    
    @Test
    void testLoadInvalidJson() {
        // Создаем файл с некорректным JSON
        Path tasksFile = testDataPath.resolve("tasks.json");
        try {
            Files.writeString(tasksFile, "это не json {");
            
            // Должен вернуться пустой список, не выбрасывать исключение
            List<Task> tasks = JsonUtil.loadTasks();
            assertNotNull(tasks, "Список задач не должен быть null");
            assertTrue(tasks.isEmpty() || tasks.size() == 0, 
                "Для некорректного JSON должен возвращаться пустой список или корректно обработанные задачи");
            
        } catch (IOException e) {
            fail("Не удалось создать тестовый файл: " + e.getMessage());
        }
    }
    
    @Test
    void testLoadFromNonexistentFile() {
        // Удаляем файл если он существует
        Path tasksFile = testDataPath.resolve("tasks.json");
        try {
            Files.deleteIfExists(tasksFile);
        } catch (IOException e) {
            fail("Не удалось удалить файл: " + e.getMessage());
        }
        
        // Загружаем из несуществующего файла
        List<Task> tasks = JsonUtil.loadTasks();
        
        assertNotNull(tasks, "Список задач не должен быть null");
        assertTrue(tasks.isEmpty(), "Для несуществующего файла должен возвращаться пустой список");
    }
    
    @Test
    void testLoadEmptyFile() {
        // Создаем пустой файл
        Path tasksFile = testDataPath.resolve("tasks.json");
        try {
            Files.createFile(tasksFile);
            
            // Загружаем
            List<Task> tasks = JsonUtil.loadTasks();
            
            assertNotNull(tasks, "Список задач не должен быть null");
            assertTrue(tasks.isEmpty(), "Для пустого файла должен возвращаться пустой список");
            
        } catch (IOException e) {
            fail("Не удалось создать пустой файл: " + e.getMessage());
        }
    }
    
    @Test
    void testLoadEmptyJsonArray() {
        // Создаем файл с пустым JSON массивом
        Path tasksFile = testDataPath.resolve("tasks.json");
        try {
            Files.writeString(tasksFile, "[]");
            
            // Загружаем
            List<Task> tasks = JsonUtil.loadTasks();
            
            assertNotNull(tasks, "Список задач не должен быть null");
            assertTrue(tasks.isEmpty(), "Для пустого JSON массива должен возвращаться пустой список");
            
        } catch (IOException e) {
            fail("Не удалось создать тестовый файл: " + e.getMessage());
        }
    }
    
    @Test
    void testSpecialCharactersInJson() {
        // Создаем задачу со специальными символами
        Task task = new Task(
            "Задача с \"кавычками\" и \nпереносами",
            "Описание с \\обратным\\ слешем и табуляцией\tздесь",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.IMPORTANT,
            Category.OTHER
        );
        
        // Сохраняем
        boolean saved = JsonUtil.saveTasks(Collections.singletonList(task));
        assertTrue(saved, "Задача со специальными символами должна сохраниться");
        
        // Загружаем
        List<Task> loadedTasks = JsonUtil.loadTasks();
        
        // Проверяем
        assertEquals(1, loadedTasks.size(), "Должна загрузиться одна задача");
        assertEquals("Задача с \"кавычками\" и \nпереносами", loadedTasks.get(0).getTitle());
        assertEquals("Описание с \\обратным\\ слешем и табуляцией\tздесь", 
                     loadedTasks.get(0).getDescription());
    }
    
    @Test
    void testAddSingleTask() {
        // Создаем тестовую задачу
        Task task = new Task(
            "Новая задача",
            "Добавляемая задача",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.NORMAL,
            Category.HOME
        );
        
        // Добавляем
        boolean added = JsonUtil.addTask(task);
        assertTrue(added, "Задача должна успешно добавиться");
        
        // Проверяем
        List<Task> tasks = JsonUtil.loadTasks();
        assertEquals(1, tasks.size(), "Должна быть одна задача");
        assertEquals(task.getTitle(), tasks.get(0).getTitle());
    }
    
    @Test
    void testExportTasks() {
        // Создаем тестовые задачи
        Task task1 = new Task("Задача 1", "Описание 1", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            Priority.URGENT, Category.WORK);
        
        Task task2 = new Task("Задача 2", "Описание 2", 
            LocalDateTime.now(), LocalDateTime.now().plusDays(2),
            Priority.NORMAL, Category.HOME);
        
        List<Task> tasks = Arrays.asList(task1, task2);
        
        // Экспортируем в другой файл
        Path exportPath = testDataPath.resolve("export.json");
        boolean exported = JsonUtil.exportTasks(exportPath.toString(), tasks);
        
        assertTrue(exported, "Задачи должны успешно экспортироваться");
        assertTrue(Files.exists(exportPath), "Файл экспорта должен существовать");
        
        // Проверяем содержимое
        try {
            String content = Files.readString(exportPath);
            assertTrue(content.contains("Задача 1"), "Экспортированный файл должен содержать задачи");
            assertTrue(content.contains("Задача 2"), "Экспортированный файл должен содержать задачи");
        } catch (IOException e) {
            fail("Не удалось прочитать экспортированный файл: " + e.getMessage());
        }
    }
    
    @Test
    void testTaskSerialization() {
        // Создаем тестовую задачу
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task(
            "test-id-123",
            "Тест сериализации",
            "Проверка toJsonString()",
            now,
            now.plusDays(1),
            Priority.IMPORTANT,
            Category.STUDY
        );
        
        // Получаем JSON строку
        String json = task.toJsonString();
        
        // Проверяем основные поля
        assertTrue(json.contains("\"id\":\"test-id-123\""), "JSON должен содержать ID");
        assertTrue(json.contains("\"title\":\"Тест сериализации\""), "JSON должен содержать заголовок");
        assertTrue(json.contains("\"priority\":\"IMPORTANT\""), "JSON должен содержать приоритет");
        assertTrue(json.contains("\"category\":\"STUDY\""), "JSON должен содержать категорию");
        assertTrue(json.contains("\"completed\":false"), "JSON должен содержать статус выполнения");
        
        // Проверяем структуру
        assertTrue(json.startsWith("{"), "JSON должен начинаться с {");
        assertTrue(json.endsWith("}"), "JSON должен заканчиваться }");
    }
}