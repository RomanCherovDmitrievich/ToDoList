package com.todoapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.todoapp.models.Task;
import com.todoapp.models.TaskManager;
import com.todoapp.utils.DateUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class CalendarController {
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    
    private YearMonth currentYearMonth;
    private TaskManager taskManager;
    private MainController mainController;

    public void initialize() {
        currentYearMonth = YearMonth.now();
        updateCalendar();
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handlePreviousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        updateCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        updateCalendar();
    }

    @FXML
    private void handleToday() {
        currentYearMonth = YearMonth.now();
        updateCalendar();
    }

    private void updateCalendar() {
        monthYearLabel.setText(currentYearMonth.getMonth().toString() + " " + currentYearMonth.getYear());
        calendarGrid.getChildren().clear();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        
        // Заполняем календарь
        LocalDate date = firstOfMonth.minusDays(dayOfWeek - 1);
        
        for (int i = 0; i < 42; i++) { // 6 недель
            VBox dayCell = createDayCell(date);
            int row = i / 7;
            int col = i % 7;
            calendarGrid.add(dayCell, col, row);
            date = date.plusDays(1);
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox();
        cell.setPrefSize(100, 80);
        cell.setStyle("-fx-border-color: #ddd; -fx-padding: 5;");
        
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        
        // Подсветка текущего дня
        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
        }
        
        // Подсветка дней не текущего месяца
        if (!date.getMonth().equals(currentYearMonth.getMonth())) {
            dayLabel.setTextFill(Color.GRAY);
        }
        
        cell.getChildren().add(dayLabel);
        
        // Добавляем задачи для этого дня
        if (taskManager != null) {
            List<Task> dayTasks = taskManager.getTasksForDate(date);
            int displayCount = Math.min(dayTasks.size(), 3); // Показываем максимум 3 задачи
            
            for (int i = 0; i < displayCount; i++) {
                Task task = dayTasks.get(i);
                Rectangle taskIndicator = new Rectangle(80, 4);
                
                // Цвет в зависимости от приоритета
                switch (task.getPriority()) {
                    case HIGH -> taskIndicator.setFill(Color.RED);
                    case MEDIUM -> taskIndicator.setFill(Color.ORANGE);
                    case LOW -> taskIndicator.setFill(Color.GREEN);
                }
                
                // Полупрозрачный для выполненных задач
                if (task.isCompleted()) {
                    taskIndicator.setOpacity(0.3);
                }
                
                cell.getChildren().add(taskIndicator);
            }
            
            if (dayTasks.size() > 3) {
                Label moreLabel = new Label("... еще " + (dayTasks.size() - 3));
                moreLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
                cell.getChildren().add(moreLabel);
            }
        }
        
        return cell;
    }
}