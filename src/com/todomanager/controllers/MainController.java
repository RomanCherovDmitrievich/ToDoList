package com.todomanager.controllers;

import com.todomanager.managers.TaskManager;
import com.todomanager.models.Task;
import com.todomanager.models.TaskPriority;
import com.todomanager.exceptions.TaskException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class MainController {
    @FXML private ListView<Task> tasksListView;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker dateFilter;
    @FXML private Label currentDateLabel;
    @FXML private VBox statsBox;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label overdueTasksLabel;
    
    private TaskManager taskManager;
    private ObservableList<Task> filteredTasks;
    private DateTimeFormatter dateFormatter;
    
    public void initialize() {
        taskManager = new TaskManager();
        filteredTasks = FXCollections.observableArrayList();
        dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        setupFilters();
        setupTasksListView();
        updateDisplay();
        updateStatistics();
        
        currentDateLabel.setText(LocalDate.now().format(dateFormatter));
    }
    
    private void setupFilters() {
        // Настройка фильтров приоритета
        priorityFilter.setItems(FXCollections.observableArrayList(
            "Все приоритеты", "Высокий", "Средний", "Низкий"
        ));
        priorityFilter.setValue("Все приоритеты");
        
        // Настройка фильтров статуса
        statusFilter.setItems(FXCollections.observableArrayList(
            "Все задачи", "Активные", "Выполненные", "Просроченные"
        ));
        statusFilter.setValue("Все задачи");
        
        // Обработчики изменений фильтров
        priorityFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
        dateFilter.setOnAction(e -> applyFilters());
    }
    
    private void setupTasksListView() {
        tasksListView.setItems(filteredTasks);
        tasksListView.setCellFactory(param -> new ListCell<Task>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                
                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    
                    // Информация о задаче
                    VBox taskInfo = new VBox(5);
                    Label titleLabel = new Label(task.getTitle());
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    Label detailsLabel = new Label(
                        String.format("%s | %s | %s", 
                            task.getDueDate().format(dateFormatter),
                            task.getPriority().getDisplayName(),
                            task.getCategory()
                        )
                    );
                    detailsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                    
                    taskInfo.getChildren().addAll(titleLabel, detailsLabel);
                    
                    // Кнопки действий
                    HBox actionsBox = new HBox(5);
                    Button completeBtn = new Button(task.isCompleted() ? "↶" : "✓");
                    completeBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                    
                    Button editBtn = new Button("✏️");
                    editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                    
                    Button deleteBtn = new Button("🗑️");
                    deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    
                    // Обработчики кнопок
                    completeBtn.setOnAction(e -> toggleTaskCompletion(task));
                    editBtn.setOnAction(e -> editTask(task));
                    deleteBtn.setOnAction(e -> deleteTask(task));
                    
                    actionsBox.getChildren().addAll(completeBtn, editBtn, deleteBtn);
                    hbox.getChildren().addAll(taskInfo, actionsBox);
                    
                    // Стиль в зависимости от статуса
                    if (task.isCompleted()) {
                        setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #999;");
                        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #999;");
                        titleLabel.setText("[ВЫПОЛНЕНО] " + task.getTitle());
                    } else if (task.isOverdue()) {
                        setStyle("-fx-background-color: #ffeaea; -fx-border-color: #e74c3c; -fx-border-width: 0 0 0 4px;");
                    } else {
                        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: " + 
                                task.getPriority().getColor() + "; -fx-border-width: 0 0 0 4px;");
                    }
                    
                    setGraphic(hbox);
                }
            }
        });
    }
    
    private void applyFilters() {
        filteredTasks.clear();
        
        // Работа с коллекциями и потоками
        taskManager.getAllTasks().stream()
            .filter(this::matchesPriorityFilter)
            .filter(this::matchesStatusFilter)
            .filter(this::matchesDateFilter)
            .forEach(filteredTasks::add);
    }
    
    private boolean matchesPriorityFilter(Task task) {
        String selectedPriority = priorityFilter.getValue();
        if (selectedPriority.equals("Все приоритеты")) return true;
        
        return task.getPriority().getDisplayName().equals(selectedPriority);
    }
    
    private boolean matchesStatusFilter(Task task) {
        String selectedStatus = statusFilter.getValue();
        if (selectedStatus.equals("Все задачи")) return true;
        
        switch (selectedStatus) {
            case "Активные": return !task.isCompleted() && !task.isOverdue();
            case "Выполненные": return task.isCompleted();
            case "Просроченные": return task.isOverdue();
            default: return true;
        }
    }
    
    private boolean matchesDateFilter(Task task) {
        LocalDate selectedDate = dateFilter.getValue();
        return selectedDate == null || task.getDueDate().equals(selectedDate);
    }
    
    @FXML
    private void addNewTask() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/todomanager/views/taskForm.fxml"));
            Parent root = loader.load();
            
            TaskFormController controller = loader.getController();
            controller.setTaskManager(taskManager);
            controller.setMainController(this);
            
            Stage stage = new Stage();
            stage.setTitle("Новая задача");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть форму создания задачи");
        }
    }
    
    private void editTask(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/todomanager/views/taskForm.fxml"));
            Parent root = loader.load();
            
            TaskFormController controller = loader.getController();
            controller.setTaskManager(taskManager);
            controller.setMainController(this);
            controller.setTaskForEditing(task);
            
            Stage stage = new Stage();
            stage.setTitle("Редактирование задачи");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть форму редактирования");
        }
    }
    
    private void toggleTaskCompletion(Task task) {
        try {
            taskManager.toggleTaskCompletion(task.getId());
            updateDisplay();
            updateStatistics();
        } catch (TaskException e) {
            showError("Ошибка", e.getMessage());
        }
    }
    
    private void deleteTask(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление задачи");
        alert.setContentText("Вы уверены, что хотите удалить задачу \"" + task.getTitle() + "\"?");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                taskManager.deleteTask(task.getId());
                updateDisplay();
                updateStatistics();
            } catch (TaskException e) {
                showError("Ошибка", e.getMessage());
            }
        }
    }
    
    public void updateDisplay() {
        applyFilters();
    }
    
    private void updateStatistics() {
        Map<String, Integer> stats = taskManager.getStatistics();
        
        totalTasksLabel.setText("Всего задач: " + stats.get("total"));
        completedTasksLabel.setText("Выполнено: " + stats.get("completed"));
        pendingTasksLabel.setText("Активных: " + stats.get("pending"));
        overdueTasksLabel.setText("Просрочено: " + stats.get("overdue"));
    }
    
    @FXML
    private void showCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/todomanager/views/calendar.fxml"));
            Parent root = loader.load();
            
            CalendarController controller = loader.getController();
            controller.setTaskManager(taskManager);
            
            Stage stage = new Stage();
            stage.setTitle("Календарь задач");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
            
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть календарь");
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}