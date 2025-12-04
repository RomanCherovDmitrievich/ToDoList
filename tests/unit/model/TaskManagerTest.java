package unit.model;

import model.Task;
import model.TaskManager;
import model.Priority;
import model.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование класса TaskManager
 */
@DisplayName("Тестирование TaskManager")
class TaskManagerTest {
    
    private TaskManager taskManager;
    private Task testTask1;
    private Task testTask2;
    private Task testTask3;
    
    @BeforeEach
    void setUp() {
        // Получаем экземпляр TaskManager (синглтон)
        taskManager = TaskManager.getInstance();
        taskManager.clearAllTasks(); // Очищаем перед каждым тестом
        
        LocalDateTime now = LocalDateTime.now();
        
        testTask1 = new Task(
            "Тестовая задача 1",
            "Первая тестовая задача",
            now.minusHours(2),
            now.plusHours(2),
            Priority.URGENT,
            Category.WORK
        );
        
        testTask2 = new Task(
            "Тестовая задача 2",
            "Вторая тестовая задача",
            now.minusHours(1),
            now.minusMinutes(30), // Просроченная
            Priority.IMPORTANT,
            Category.HOME
        );
        
        testTask3 = new Task(
            "Тестовая задача 3",
            "Третья тестовая задача, выполненная",
            now.minusDays(1),
            now.minusHours(12),
            Priority.NORMAL,
            Category.STUDY
        );
        testTask3.setCompleted(true);
    }
    
    @AfterEach
    void tearDown() {
        // Очищаем после каждого теста
        taskManager.clearAllTasks();
    }
    
    @Test
    @DisplayName("Проверка паттерна Singleton")
    void testSingletonPattern() {
        TaskManager instance1 = TaskManager.getInstance();
        TaskManager instance2 = TaskManager.getInstance();
        
        assertSame(instance1, instance2, "Должен возвращаться один и тот же экземпляр");
        assertNotNull(instance1);
        assertNotNull(instance2);
    }
    
    @Test
    @DisplayName("Добавление задачи в менеджер")
    void testAddTask() {
        int initialCount = taskManager.getAllTasks().size();
        
        taskManager.addTask(testTask1);
        List<Task> tasks = taskManager.getAllTasks();
        
        assertEquals(initialCount + 1, tasks.size());
        assertTrue(tasks.contains(testTask1));
    }
    
    @Test
    @DisplayName("Добавление нескольких задач")
    void testAddMultipleTasks() {
        taskManager.addTask(testTask1);
        taskManager.addTask(testTask2);
        taskManager.addTask(testTask3);
        
        assertEquals(3, taskManager.getAllTasks().size());
    }
    
    @Test
    @DisplayName("Удаление задачи по ID")
    void testRemoveTaskById() {
        taskManager.addTask(testTask1);
        taskManager.addTask(testTask2);
        
        int initialCount = taskManager.getAllTasks().size();
        boolean removed = taskManager.removeTask(testTask1.getId());
        
        assertTrue(removed);
        assertEquals(initialCount - 1, taskManager.getAllTasks().size());
        assertFalse(taskManager.getAllTasks().contains(testTask1));
    }
    
    @Test
    @DisplayName("Удаление несуществующей задачи")
    void testRemoveNonExistentTask() {
        taskManager.addTask(testTask1);
        int initialCount = taskManager.getAllTasks().size();
        
        boolean removed = taskManager.removeTask(UUID.randomUUID().toString());
        
        assertFalse(removed);
        assertEquals(initialCount, taskManager.getAllTasks().size());
    }
    
    @Test
    @DisplayName("Поиск задач по тексту (регистронезависимый)")
    void testSearchTasks() {
        taskManager.addTask(testTask1);
        taskManager.addTask(testTask2);
        taskManager.addTask(testTask3);
        
        // Поиск по заголовку
        List<Task> foundByTitle = taskManager.searchTasks("тестовая");
        assertEquals(3, foundByTitle.size());
        
        // Поиск по описанию
        List<Task> foundByDescription = taskManager.searchTasks("первая");
        assertEquals(1, foundByDescription.size());
        assertEquals(testTask1, foundByDescription.get(0));
        
        // Поиск с разным регистром
        List<Task> foundUpperCase = taskManager.searchTasks("ТЕСТОВАЯ");
        assertEquals(3, foundUpperCase.size());
        
        // Поиск несуществующего текста
        List<Task> foundNothing = taskManager.searchTasks("несуществующий");
        assertTrue(foundNothing.isEmpty());
    }
    
    @Test
    @DisplayName("Поиск по пустому запросу")
    void testSearchWithEmptyQuery() {
        taskManager.addTask(testTask1);
        taskManager.addTask(testTask2);
        
        List<Task> found = taskManager.searchTasks("");
        assertEquals(2, found.size());
        
        found = taskManager.searchTasks("   ");
        assertEquals(2, found.size());
        
        found = taskManager.searchTasks(null);
        assertEquals(2, found.size());
    }
    
    @Test
    @DisplayName("Получение задач по статусу выполнения")
    void testGetTasksByCompletion() {
        taskManager.addTask(testTask1); // не выполнена
        taskManager.addTask(testTask2); // не выполнена
        taskManager.addTask(testTask3); // выполнена
        
        List<Task> completedTasks = taskManager.getTasksByCompletion(true);
        assertEquals(1, completedTasks.size());
        assertEquals(testTask3, completedTasks.get(0));
        
        List<Task> activeTasks = taskManager.getTasksByCompletion(false);
        assertEquals(2, activeTasks.size());
        assertTrue(activeTasks.contains(testTask1));
        assertTrue(activeTasks.contains(testTask2));
    }
    
    @Test
    @DisplayName("Отметка задачи как выполненной")
    void testMarkAsCompleted() {
        taskManager.addTask(testTask1);
        
        boolean marked = taskManager.markAsCompleted(testTask1.getId(), true);
        assertTrue(marked);
        
        List<Task> completedTasks = taskManager.getTasksByCompletion(true);
        assertEquals(1, completedTasks.size());
        assertTrue(completedTasks.get(0).isCompleted());
        
        // Отмена выполнения
        marked = taskManager.markAsCompleted(testTask1.getId(), false);
        assertTrue(marked);
        
        completedTasks = taskManager.getTasksByCompletion(true);
        assertTrue(completedTasks.isEmpty());
    }
    
    @Test
    @DisplayName("Отметка несуществующей задачи как выполненной")
    void testMarkNonExistentAsCompleted() {
        boolean marked = taskManager.markAsCompleted(UUID.randomUUID().toString(), true);
        assertFalse(marked);
    }
    
    @Test
    @DisplayName("Получение количества задач")
    void testGetTaskCount() {
        assertEquals(0, taskManager.getTaskCount(true));
        assertEquals(0, taskManager.getTaskCount(false));
        
        taskManager.addTask(testTask1); // активная
        taskManager.addTask(testTask2); // активная
        taskManager.addTask(testTask3); // выполненная
        
        assertEquals(1, taskManager.getTaskCount(true));
        assertEquals(2, taskManager.getTaskCount(false));
        
        assertEquals(3, taskManager.getAllTasks().size());
    }
    
    @Test
    @DisplayName("Получение количества просроченных задач")
    void testGetOverdueTaskCount() {
        taskManager.addTask(testTask1); // не просрочена
        taskManager.addTask(testTask2); // просрочена
        taskManager.addTask(testTask3); // выполнена (не считается просроченной)
        
        taskManager.checkAllTasksOverdue(); // Обновляем статусы
        
        int overdueCount = taskManager.getOverdueTaskCount();
        assertEquals(1, overdueCount);
        
        // Проверяем что выполненная задача не считается просроченной
        assertFalse(testTask3.isOverdue());
    }
    
    @Test
    @DisplayName("Очистка всех задач")
    void testClearAllTasks() {
        taskManager.addTask(testTask1);
        taskManager.addTask(testTask2);
        taskManager.addTask(testTask3);
        
        assertEquals(3, taskManager.getAllTasks().size());
        
        taskManager.clearAllTasks();
        
        assertTrue(taskManager.getAllTasks().isEmpty());
        assertEquals(0, taskManager.getTaskCount(true));
        assertEquals(0, taskManager.getTaskCount(false));
    }
    
    @Test
    @DisplayName("Загрузка списка задач")
    void testLoadTasks() {
        // Сначала добавляем задачи
        taskManager.addTask(testTask1);
        taskManager.addTask(testTask2);
        
        assertEquals(2, taskManager.getAllTasks().size());
        
        // Создаем новый список задач
        Task newTask = new Task(
            "Новая задача",
            "Описание",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            Priority.NORMAL,
            Category.OTHER
        );
        
        List<Task> newTasks = List.of(newTask);
        
        // Загружаем новый список (старые задачи должны быть заменены)
        taskManager.loadTasks(newTasks);
        
        List<Task> allTasks = taskManager.getAllTasks();
        assertEquals(1, allTasks.size());
        assertEquals(newTask, allTasks.get(0));
    }
    
    @Test
    @DisplayName("Автоматическая проверка просроченности")
    void testAutomaticOverdueCheck() {
        // Задача с прошедшим сроком
        Task overdueTask = new Task(
            "Просроченная",
            "Описание",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            Priority.URGENT,
            Category.WORK
        );
        
        taskManager.addTask(overdueTask);
        
        // После добавления должна быть проверена на просроченность
        taskManager.checkAllTasksOverdue();
        
        assertTrue(overdueTask.isOverdue());
        assertEquals(1, taskManager.getOverdueTaskCount());
    }
    
    @Test
    @DisplayName("Проверка неизменяемости возвращаемых списков")
    void testReturnedListsImmutability() {
        taskManager.addTask(testTask1);
        
        List<Task> tasks = taskManager.getAllTasks();
        assertEquals(1, tasks.size());
        
        // Попытка изменить возвращенный список не должна влиять на менеджер
        assertThrows(UnsupportedOperationException.class, () -> {
            tasks.add(testTask2);
        });
        
        // Но мы можем добавить через менеджер
        taskManager.addTask(testTask2);
        assertEquals(2, taskManager.getAllTasks().size());
    }
    
    @Test
    @DisplayName("Проверка обновления просроченности при изменении статуса")
    void testOverdueUpdateOnCompletion() {
        taskManager.addTask(testTask2); // Просроченная задача
        
        taskManager.checkAllTasksOverdue();
        assertTrue(testTask2.isOverdue());
        assertEquals(1, taskManager.getOverdueTaskCount());
        
        // Помечаем как выполненную
        taskManager.markAsCompleted(testTask2.getId(), true);
        
        // После выполнения задача не должна считаться просроченной
        assertFalse(testTask2.isOverdue());
        assertEquals(0, taskManager.getOverdueTaskCount());
    }
}