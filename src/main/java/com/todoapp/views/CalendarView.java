package com.todoapp.views;

import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.todoapp.models.Task;
import com.todoapp.models.CalendarModel;

import java.time.LocalDate;
import java.util.List;

public class CalendarView {
    private GridPane calendarGrid;
    private CalendarModel calendarModel;

    public CalendarView(CalendarModel calendarModel) {
        this.calendarModel = calendarModel;
        this.calendarGrid = new GridPane();
        initializeGrid();
    }

    private void initializeGrid() {
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        calendarGrid.setStyle("-fx-padding: 10;");
    }

    public GridPane getCalendarGrid() {
        return calendarGrid;
    }

    public void updateView() {
        calendarGrid.getChildren().clear();
        createDayHeaders();
        createCalendarDays();
    }

    private void createDayHeaders() {
        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-alignment: center;");
            calendarGrid.add(dayLabel, i, 0);
        }
    }

    private void createCalendarDays() {
        LocalDate firstOfMonth = calendarModel.getCurrentMonth().atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        
        LocalDate date = firstOfMonth.minusDays(dayOfWeek - 1);
        
        for (int i = 0; i < 42; i++) { // 6 недель
            VBox dayCell = createDayCell(date);
            int row = (i / 7) + 1; // +1 для заголовков
            int col = i % 7;
            calendarGrid.add(dayCell, col, row);
            date = date.plusDays(1);
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(3);
        cell.setPrefSize(100, 80);
        cell.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 5;");
        
        // Подсветка текущего дня
        if (date.equals(LocalDate.now())) {
            cell.setStyle(cell.getStyle() + " -fx-background-color: #e3f2fd;");
        }
        
        // Подсветка дней не текущего месяца
        if (!date.getMonth().equals(calendarModel.getCurrentMonth().getMonth())) {
            cell.setStyle(cell.getStyle() + " -fx-opacity: 0.5;");
        }

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-weight: bold;");
        cell.getChildren().add(dayLabel);
        
        // Добавляем индикаторы задач
        addTaskIndicators(cell, date);
        
        return cell;
    }

    private void addTaskIndicators(VBox cell, LocalDate date) {
        List<Task> dayTasks = calendarModel.getTasksForDate(date);
        
        if (dayTasks.isEmpty()) {
            return;
        }

        // Показываем до 3 индикаторов задач
        int tasksToShow = Math.min(dayTasks.size(), 3);
        
        for (int i = 0; i < tasksToShow; i++) {
            Task task = dayTasks.get(i);
            Rectangle indicator = new Rectangle(80, 3);
            
            // Цвет индикатора в зависимости от статуса и приоритета
            if (task.isCompleted()) {
                indicator.setFill(Color.LIGHTGRAY);
            } else if (task.isOverdue()) {
                indicator.setFill(Color.RED);
            } else {
                switch (task.getPriority()) {
                    case HIGH -> indicator.setFill(Color.RED);
                    case MEDIUM -> indicator.setFill(Color.ORANGE);
                    case LOW -> indicator.setFill(Color.GREEN);
                }
            }
            
            cell.getChildren().add(indicator);
        }
        
        // Показываем счетчик, если задач больше 3
        if (dayTasks.size() > 3) {
            Label moreLabel = new Label("... еще " + (dayTasks.size() - 3));
            moreLabel.setStyle("-fx-font-size: 9; -fx-text-fill: gray;");
            cell.getChildren().add(moreLabel);
        }
    }
}