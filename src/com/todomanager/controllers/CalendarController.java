package com.todomanager.controllers;

import com.todomanager.managers.TaskManager;
import com.todomanager.models.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class CalendarController {
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox tasksForDateBox;
    @FXML private Label selectedDateLabel;
    
    private TaskManager taskManager;
    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private DateTimeFormatter monthFormatter;
    private DateTimeFormatter dayFormatter;
    
    public void initialize() {
        monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        dayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        
        updateCalendar();
    }
    
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    private void updateCalendar() {
        // Очищаем календарь
        calendarGrid.getChildren().clear();
        
        // Устанавливаем заголовок
        monthYearLabel.setText(currentYearMonth.format(monthFormatter));
        
        // Получаем первый день месяца и определяем день недели
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1-Понедельник, 7-Воскресенье
        
        // Заполняем календарь
        LocalDate date = firstOfMonth.minusDays(dayOfWeek - 1);
        
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                VBox dayCell = createDayCell(date);
                calendarGrid.add(dayCell, j, i);
                date = date.plusDays(1);
            }
        }
        
        // Показываем задачи для выбранной даты
        showTasksForSelectedDate();
    }
    
    private VBox createDayCell(LocalDate date) {
        VBox dayCell = new VBox(2);
        dayCell.setPrefSize(60, 60);
        dayCell.setStyle("-fx-border-color: #ddd; -fx-border-width: 1px; -fx-padding: 5px;");
        
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-weight: bold;");
        
        // Проверяем, есть ли задачи на эту дату
        boolean hasTasks = taskManager.hasTasksForDate(date);
        boolean hasOverdueTasks = hasOverdueTasks(date);
        
        if (hasTasks) {
            Circle indicator = new Circle(4);
            if (hasOverdueTasks) {
                indicator.setFill(Color.RED);
            } else {
                indicator.setFill(Color.BLUE);
            }
            dayCell.getChildren().addAll(dayLabel, indicator);
        } else {
            dayCell.getChildren().add(dayLabel);
        }
        
        // Стиль для текущего месяца
        if (date.getMonth() != currentYearMonth.getMonth()) {
            dayLabel.setTextFill(Color.GRAY);
        }
        
        // Стиль для сегодняшнего дня
        if (date.equals(LocalDate.now())) {
            dayCell.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px; -fx-background-color: #fff0f0;");
        }
        
        // Стиль для выбранной даты
        if (date.equals(selectedDate)) {
            dayCell.setStyle("-fx-border-color: #3498db; -fx-border-width: 2px; -fx-background-color: #f0f8ff;");
        }
        
        // Обработчик клика
        dayCell.setOnMouseClicked(event -> {
            selectedDate = date;
            updateCalendar();
            showTasksForSelectedDate();
        });
        
        return dayCell;
    }
    
    private boolean hasOverdueTasks(LocalDate date) {
        List<Task> tasks = taskManager.getTasksByDate(date);
        return tasks.stream().anyMatch(task -> !task.isCompleted() && task.isOverdue());
    }
    
    private void showTasksForSelectedDate() {
        tasksForDateBox.getChildren().clear();
        selectedDateLabel.setText("Задачи на " + selectedDate.format(dayFormatter));
        
        List<Task> tasks = taskManager.getTasksByDate(selectedDate);
        
        if (tasks.isEmpty()) {
            Label noTasksLabel = new Label("Нет задач на выбранную дату");
            noTasksLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            tasksForDateBox.getChildren().add(noTasksLabel);
        } else {
            for (Task task : tasks) {
                VBox taskItem = createTaskItem(task);
                tasksForDateBox.getChildren().add(taskItem);
            }
        }
    }
    
    private VBox createTaskItem(Task task) {
        VBox taskItem = new VBox(5);
        taskItem.setStyle("-fx-border-color: #ddd; -fx-border-width: 1px; -fx-padding: 10px; -fx-background-color: #f9f9f9;");
        
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        Label detailsLabel = new Label(
            String.format("Приоритет: %s | Категория: %s", 
                task.getPriority().getDisplayName(),
                task.getCategory()
            )
        );
        detailsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        Label statusLabel = new Label();
        if (task.isCompleted()) {
            statusLabel.setText("✓ Выполнено");
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #999;");
            titleLabel.setText("[ВЫПОЛНЕНО] " + task.getTitle());
        } else if (task.isOverdue()) {
            statusLabel.setText("⚠ Просрочено");
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            statusLabel.setText("⏳ В процессе");
            statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        }
        
        if (task.getDueTime() != null) {
            Label timeLabel = new Label("Время: " + task.getDueTime().toString());
            timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            taskItem.getChildren().addAll(titleLabel, detailsLabel, timeLabel, statusLabel);
        } else {
            taskItem.getChildren().addAll(titleLabel, detailsLabel, statusLabel);
        }
        
        return taskItem;
    }
    
    @FXML
    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        updateCalendar();
    }
    
    @FXML
    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        updateCalendar();
    }
    
    @FXML
    private void today() {
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        updateCalendar();
    }
}