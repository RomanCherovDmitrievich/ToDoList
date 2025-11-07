package com.todoapp.views;

import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.todoapp.models.Task;
import java.time.format.DateTimeFormatter;

public class CustomTaskCell extends ListCell<Task> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        
        if (empty || task == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else {
            // Основной контейнер
            HBox mainContainer = new HBox(10);
            mainContainer.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
            
            // Индикатор приоритета
            Circle priorityIndicator = new Circle(5);
            switch (task.getPriority()) {
                case HIGH -> priorityIndicator.setFill(Color.RED);
                case MEDIUM -> priorityIndicator.setFill(Color.ORANGE);
                case LOW -> priorityIndicator.setFill(Color.GREEN);
            }
            
            // Информация о задаче
            VBox taskInfo = new VBox(5);
            
            Label titleLabel = new Label(task.getTitle());
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            
            Label detailsLabel = new Label();
            detailsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
            
            String details = String.format("%s • %s • %s",
                task.getCategory().getDisplayName(),
                task.getPriority().getDisplayName(),
                task.getDueDate() != null ? task.getDueDate().format(formatter) : "Без срока"
            );
            detailsLabel.setText(details);
            
            // Описание (если есть)
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                Label descLabel = new Label(task.getDescription());
                descLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
                descLabel.setWrapText(true);
                taskInfo.getChildren().add(descLabel);
            }
            
            taskInfo.getChildren().addAll(titleLabel, detailsLabel);
            
            // Статус
            Label statusLabel = new Label();
            if (task.isCompleted()) {
                statusLabel.setText("✓ Выполнено");
                statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (task.isOverdue()) {
                statusLabel.setText("⌛ Просрочено");
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (task.isDueToday()) {
                statusLabel.setText("⏰ Сегодня");
                statusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            }
            
            mainContainer.getChildren().addAll(priorityIndicator, taskInfo, statusLabel);
            
            // Стиль для выполненной задачи
            if (task.isCompleted()) {
                mainContainer.setStyle(mainContainer.getStyle() + " -fx-opacity: 0.6;");
            }
            
            setGraphic(mainContainer);
            setText(null);
        }
    }
}