package unit.viewmodel;

import viewmodel.TaskViewModel;
import model.Task;
import model.Priority;
import model.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование класса TaskViewModel
 */
@DisplayName("Тестирование TaskViewModel")
class TaskViewModelTest {
    
    private Task task;
    private TaskViewModel viewModel;
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
        
        viewModel = new TaskViewModel(task);
    }
    
    @Test
    @DisplayName("Создание ViewModel из модели Task")
    void testViewModelCreation() {
        assertNotNull(viewModel);
        assertEquals(task.getId(), viewModel.getId());
        assertEquals(task.getTitle(), viewModel.getTitle());
        assertEquals(task.getDescription(), viewModel.getDescription());
        assertEquals(task.getStartTime(), viewModel.getStartTime());
        assertEquals(task.getEndTime(), viewModel.getEndTime());
        assertEquals(task.getPriority(), viewModel.getPriority());
        assertEquals(task.getCategory(), viewModel.getCategory());
        assertEquals(task.isCompleted(), viewModel.isCompleted());
        assertEquals(task.isOverdue(), viewModel.isOverdue());
        assertEquals(task.getCreatedAt(), viewModel.getCreatedAt());
    }
    
    @Test
    @DisplayName("Двусторонняя привязка свойств - изменение ViewModel")
    void testTwoWayBindingViewModelToModel() {
        // Изменяем свойства в ViewModel
        viewModel.setTitle("Новый заголовок из ViewModel");
        viewModel.setDescription("Новое описание из ViewModel");
        viewModel.setPriority(Priority.URGENT);
        viewModel.setCategory(Category.STUDY);
        viewModel.setCompleted(true);
        
        // Проверяем что изменения отразились в модели
        assertEquals("Новый заголовок из ViewModel", task.getTitle());
        assertEquals("Новое описание из ViewModel", task.getDescription());
        assertEquals(Priority.URGENT, task.getPriority());
        assertEquals(Category.STUDY, task.getCategory());
        assertTrue(task.isCompleted());
    }
    
    @Test
    @DisplayName("Двусторонняя привязка свойств - изменение Model")
    void testTwoWayBindingModelToViewModel() {
        // Изменяем свойства в модели
        task.setTitle("Новый заголовок из Model");
        task.setDescription("Новое описание из Model");
        task.setPriority(Priority.NORMAL);
        task.setCategory(Category.HOME);
        task.setCompleted(true);
        
        // Обновляем ViewModel из модели
        viewModel.updateFromModel();
        
        // Проверяем что изменения отразились в ViewModel
        assertEquals("Новый заголовок из Model", viewModel.getTitle());
        assertEquals("Новое описание из Model", viewModel.getDescription());
        assertEquals(Priority.NORMAL, viewModel.getPriority());
        assertEquals(Category.HOME, viewModel.getCategory());
        assertTrue(viewModel.isCompleted());
    }
    
    @Test
    @DisplayName("Форматирование дат для отображения")
    void testFormattedDates() {
        String formattedStart = viewModel.getFormattedStartTime();
        String formattedEnd = viewModel.getFormattedEndTime();
        String formattedEndDate = viewModel.getFormattedEndDate();
        String formattedEndTimeOnly = viewModel.getFormattedEndTimeOnly();
        
        assertNotNull(formattedStart);
        assertNotNull(formattedEnd);
        assertNotNull(formattedEndDate);
        assertNotNull(formattedEndTimeOnly);
        
        // Проверяем форматы
        assertTrue(formattedStart.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"));
        assertTrue(formattedEnd.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"));
        assertTrue(formattedEndDate.matches("\\d{2}\\.\\d{2}\\.\\d{4}"));
        assertTrue(formattedEndTimeOnly.matches("\\d{2}:\\d{2}"));
        
        assertEquals("01.01.2025 09:00", formattedStart);
        assertEquals("01.01.2025 18:00", formattedEnd);
        assertEquals("01.01.2025", formattedEndDate);
        assertEquals("18:00", formattedEndTimeOnly);
    }
    
    @Test
    @DisplayName("Получение отображаемых имен приоритета и категории")
    void testDisplayNames() {
        assertEquals("Важно", viewModel.getPriorityDisplay());
        assertEquals("Работа", viewModel.getCategoryDisplay());
        
        // Проверяем для других значений
        viewModel.setPriority(Priority.URGENT);
        assertEquals("Срочно", viewModel.getPriorityDisplay());
        
        viewModel.setCategory(Category.HOME);
        assertEquals("Дом", viewModel.getCategoryDisplay());
    }
    
    @Test
    @DisplayName("Получение цветов приоритета и категории")
    void testColors() {
        assertEquals("#FFBB33", viewModel.getPriorityColor());
        assertEquals("#3D5AFE", viewModel.getCategoryColor());
        
        // Проверяем для других значений
        viewModel.setPriority(Priority.URGENT);
        assertEquals("#FF4444", viewModel.getPriorityColor());
        
        viewModel.setCategory(Category.HOME);
        assertEquals("#FF4081", viewModel.getCategoryColor());
    }
    
    @Test
    @DisplayName("Получение имени иконки приоритета")
    void testPriorityIconName() {
        assertEquals("important", viewModel.getPriorityIconName());
        
        viewModel.setPriority(Priority.URGENT);
        assertEquals("urgent", viewModel.getPriorityIconName());
        
        viewModel.setPriority(Priority.NORMAL);
        assertEquals("normal", viewModel.getPriorityIconName());
    }
    
    @Test
    @DisplayName("Расчет оставшегося времени")
    void testTimeRemaining() {
        // Задача в будущем
        LocalDateTime futureTime = LocalDateTime.now().plusDays(2);
        Task futureTask = new Task(
            "Будущая задача",
            "Описание",
            LocalDateTime.now(),
            futureTime,
            Priority.NORMAL,
            Category.HOME
        );
        TaskViewModel futureViewModel = new TaskViewModel(futureTask);
        
        String timeRemaining = futureViewModel.getTimeRemaining();
        assertNotNull(timeRemaining);
        assertTrue(timeRemaining.contains("Через") || timeRemaining.contains("Завтра") || timeRemaining.contains("Сегодня"));
        
        // Просроченная задача
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        Task pastTask = new Task(
            "Просроченная задача",
            "Описание",
            pastTime.minusHours(1),
            pastTime,
            Priority.NORMAL,
            Category.HOME
        );
        TaskViewModel pastViewModel = new TaskViewModel(pastTask);
        
        timeRemaining = pastViewModel.getTimeRemaining();
        assertNotNull(timeRemaining);
        assertTrue(timeRemaining.contains("Просрочено"));
    }
    
    @Test
    @DisplayName("Получение статуса задачи в виде строки")
    void testStatusText() {
        // Создаем задачу с просроченной датой
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        Task task = new Task(
            "Просроченная задача",
            "Должна быть просрочена",
            pastDate.minusHours(2),
            pastDate,
            Priority.URGENT,
            Category.WORK
        );
    
        TaskViewModel viewModel = new TaskViewModel(task);
    
        // Проверяем статус
        assertEquals("Просрочено", viewModel.getStatusText(), "Просроченная задача должна иметь статус 'Просрочено'");
    
        // Отмечаем как выполненную
        viewModel.setCompleted(true);
        assertEquals("Выполнено", viewModel.getStatusText(), "Выполненная задача должна иметь статус 'Выполнено'");
    
        // Создаем активную задачу
        Task activeTask = new Task(
            "Активная задача",
            "Не просрочена",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Priority.NORMAL,
            Category.HOME
        );
    
        TaskViewModel activeViewModel = new TaskViewModel(activeTask);
        assertEquals("Активно", activeViewModel.getStatusText(), "Активная задача должна иметь статус 'Активно'");
    }
    
    @Test
    @DisplayName("Проверка equals() и hashCode()")
    void testEqualsAndHashCode() {
        Task sameTask = task; // Та же задача
        TaskViewModel sameViewModel = new TaskViewModel(sameTask);
        
        // Две ViewModel для одной и той же задачи должны быть равны
        assertEquals(viewModel, sameViewModel);
        assertEquals(viewModel.hashCode(), sameViewModel.hashCode());
        
        // ViewModel для другой задачи не должна быть равна
        Task differentTask = new Task(
            "Другая задача",
            "Описание",
            startTime,
            endTime,
            Priority.NORMAL,
            Category.HOME
        );
        TaskViewModel differentViewModel = new TaskViewModel(differentTask);
        
        assertNotEquals(viewModel, differentViewModel);
        assertNotEquals(viewModel.hashCode(), differentViewModel.hashCode());
        
        // ViewModel не равна null
        assertNotEquals(null, viewModel);
        
        // ViewModel не равна объекту другого класса
        assertNotEquals("строка", viewModel);
    }
    
    @Test
    @DisplayName("Получение базовой модели задачи")
    void testGetTask() {
        Task retrievedTask = viewModel.getTask();
        assertSame(task, retrievedTask);
    }
    
    @Test
    @DisplayName("Тестирование toString()")
    void testToString() {
        String str = viewModel.toString();
        assertNotNull(str);
        assertTrue(str.contains("TaskViewModel"));
        assertTrue(str.contains(viewModel.getTitle()));
        assertTrue(str.contains(viewModel.getPriorityDisplay()));
    }
    
    @Test
    @DisplayName("Проверка свойств JavaFX")
    void testJavaFXProperties() {
        // Проверяем что все свойства доступны
        assertNotNull(viewModel.titleProperty());
        assertNotNull(viewModel.descriptionProperty());
        assertNotNull(viewModel.priorityProperty());
        assertNotNull(viewModel.categoryProperty());
        assertNotNull(viewModel.completedProperty());
        assertNotNull(viewModel.overdueProperty());
        
        // Проверяем что свойства синхронизированы с геттерами
        assertEquals(viewModel.getTitle(), viewModel.titleProperty().get());
        assertEquals(viewModel.getDescription(), viewModel.descriptionProperty().get());
        assertEquals(viewModel.getPriority(), viewModel.priorityProperty().get());
        assertEquals(viewModel.getCategory(), viewModel.categoryProperty().get());
        assertEquals(viewModel.isCompleted(), viewModel.completedProperty().get());
        assertEquals(viewModel.isOverdue(), viewModel.overdueProperty().get());
    }
    
    @Test
    @DisplayName("Обновление ViewModel при изменении статуса просроченности")
    void testOverdueUpdate() {
        // Создаем задачу с прошедшим сроком
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        Task overdueTask = new Task(
            "Задача",
            "Описание",
            pastTime.minusHours(1),
            pastTime,
            Priority.NORMAL,
            Category.HOME
        );
        TaskViewModel overdueViewModel = new TaskViewModel(overdueTask);
        
        // Проверяем что просроченность определилась
        overdueTask.checkOverdue();
        overdueViewModel.updateFromModel();
        
        assertTrue(overdueViewModel.isOverdue());
        assertEquals("Просрочено", overdueViewModel.getStatusText());
    }
}