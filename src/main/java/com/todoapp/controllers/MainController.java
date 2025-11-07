package com.todoapp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.todoapp.models.Task;
import com.todoapp.models.TaskManager;
import com.todoapp.views.CustomTaskCell;
import com.todoapp.utils.DateUtils;
import com.todoapp.utils.FileManager;
import com.todoapp.utils.NotificationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MainController {
    @FXML private ListView<Task> taskListView;
    @FXML private DatePicker datePicker;
    @FXML private ChoiceBox<Task.Priority> priorityChoiceBox;
    @FXML private ChoiceBox<Task.Category> categoryChoiceBox;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Button addButton;
    @FXML private Button deleteButton;
    @FXML private Button markCompletedButton;
    @FXML private Button editButton;
    @FXML private Button calendarButton;
    @FXML private VBox overdueTasksBox;
    @FXML private Label overdueCountLabel;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    private TaskManager taskManager;
    private NotificationManager notificationManager;

    public void initialize() {
        taskManager = FileManager.loadTasks();
        notificationManager = new NotificationManager(taskManager);
        
        setupUI();
        updateTaskList();
        startNotificationChecker();
    }

    private void setupUI() {
        // Инициализация ChoiceBox
        priorityChoiceBox.getItems().setAll(Task.Priority.values());
        categoryChoiceBox.getItems().setAll(Task.Category.values());
        
        // Инициализация ComboBox фильтров
        filterComboBox.getItems().addAll("Все задачи", "Сегодня", "Неделя", "Просроченные", "Выполненные", "Высокий приоритет");
        filterComboBox.setValue("Все задачи");

        // Установка значений по умолчанию
        priorityChoiceBox.setValue(Task.Priority.MEDIUM);
        categoryChoiceBox.setValue(Task.Category.PERSONAL);
        datePicker.setValue(LocalDate.now());
        
        // Настройка ListView с кастомными ячейками
        taskListView.setCellFactory(lv -> new CustomTaskCell());
        
        // Настройка обработчиков
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        filterComboBox.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchTasks(newValue));
    }

    @FXML
    private void handleAddTask() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        Task.Priority priority = priorityChoiceBox.getValue();
        Task.Category category = categoryChoiceBox.getValue();
        LocalDate dueDate = datePicker.getValue();

        if (title.isEmpty() || dueDate == null) {
            showAlert("Ошибка", "Заполните название и дату выполнения");
            return;
        }

        LocalDateTime dueDateTime = dueDate.atTime(12, 0);
        
        if (taskManager.hasTimeConflict(dueDateTime, dueDateTime.plusHours(1), null)) {
            showAlert("Конфликт времени", "На это время уже запланирована другая задача");
            return;
        }

        Task task = new Task(title, description, priority, category, dueDateTime);
        taskManager.addTask(task);
        FileManager.saveTasks(taskManager);
        
        clearForm();
        updateTaskList();
        notificationManager.checkNotifications();
    }

    @FXML
    private void handleDeleteTask() {
        Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Подтверждение удаления");
            alert.setHeaderText("Удалить задачу?");
            alert.setContentText("Вы уверены, что хотите удалить задачу: " + selectedTask.getTitle());
            
            if (alert.showAndWait().get() == ButtonType.OK) {
                taskManager.removeTask(selectedTask);
                FileManager.saveTasks(taskManager);
                updateTaskList();
            }
        }
    }

    @FXML
    private void handleMarkCompleted() {
        Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            selectedTask.setCompleted(!selectedTask.isCompleted());
            FileManager.saveTasks(taskManager);
            updateTaskList();
            notificationManager.checkNotifications();
        }
    }

    @FXML
    private void handleEditTask() {
        Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            openTaskDetailDialog(selectedTask);
        }
    }

    @FXML
    private void handleShowCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/calendar.fxml"));
            Parent root = loader.load();
            
            CalendarController controller = loader.getController();
            controller.setTaskManager(taskManager);
            controller.setMainController(this);
            
            Stage stage = new Stage();
            stage.setTitle("Календарь задач");
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось открыть календарь");
        }
    }

    @FXML
    private void handleExportTasks() {
        // Экспорт задач в файл
        FileManager.exportToCSV(taskManager.getAllTasks());
        showAlert("Экспорт", "Задачи успешно экспортированы в CSV файл");
    }

    private void openTaskDetailDialog(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/task_detail.fxml"));
            Parent root = loader.load();
            
            TaskDetailController controller = loader.getController();
            controller.setTask(task);
            controller.setTaskManager(taskManager);
            
            Stage stage = new Stage();
            stage.setTitle("Редактирование задачи");
            stage.setScene(new Scene(root, 500, 400));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait();
            
            FileManager.saveTasks(taskManager);
            updateTaskList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String filter = filterComboBox.getValue();
        List<Task> filteredTasks;
        
        switch (filter) {
            case "Сегодня":
                filteredTasks = taskManager.getTasksForDate(LocalDate.now());
                break;
            case "Неделя":
                filteredTasks = taskManager.getTasksForWeek(LocalDate.now());
                break;
            case "Просроченные":
                filteredTasks = taskManager.getOverdueTasks();
                break;
            case "Выполненные":
                filteredTasks = taskManager.getAllTasks().stream()
                        .filter(Task::isCompleted)
                        .toList();
                break;
            case "Высокий приоритет":
                filteredTasks = taskManager.getTasksByPriority(Task.Priority.HIGH);
                break;
            default:
                filteredTasks = taskManager.getAllTasks();
        }
        
        taskListView.getItems().setAll(filteredTasks);
    }

    private void searchTasks(String query) {
        if (query == null || query.trim().isEmpty()) {
            applyFilter();
            return;
        }
        
        List<Task> searchResults = taskManager.getAllTasks().stream()
                .filter(task -> task.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                               task.getDescription().toLowerCase().contains(query.toLowerCase()))
                .toList();
        
        taskListView.getItems().setAll(searchResults);
    }

    private void updateTaskList() {
        applyFilter();
        
        // Обновление счетчика просроченных задач
        int overdueCount = taskManager.getOverdueTasks().size();
        overdueCountLabel.setText("Просрочено: " + overdueCount);
        
        // Визуальное обновление
        overdueCountLabel.setStyle(overdueCount > 0 ? 
            "-fx-text-fill: red; -fx-font-weight: bold;" : 
            "-fx-text-fill: green;");
    }

    private void startNotificationChecker() {
        // Проверка уведомлений каждые 30 секунд
        Thread notificationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    javafx.application.Platform.runLater(() -> {
                        notificationManager.checkNotifications();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        priorityChoiceBox.setValue(Task.Priority.MEDIUM);
        categoryChoiceBox.setValue(Task.Category.PERSONAL);
        datePicker.setValue(LocalDate.now());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshData() {
        updateTaskList();
    }
}