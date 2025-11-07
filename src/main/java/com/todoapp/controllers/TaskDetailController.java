package com.todoapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.todoapp.models.Task;
import com.todoapp.models.TaskManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskDetailController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ChoiceBox<Task.Priority> priorityChoiceBox;
    @FXML private ChoiceBox<Task.Category> categoryChoiceBox;
    @FXML private DatePicker dueDatePicker;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label createdAtLabel;

    private Task task;
    private TaskManager taskManager;

    public void initialize() {
        priorityChoiceBox.getItems().setAll(Task.Priority.values());
        categoryChoiceBox.getItems().setAll(Task.Category.values());
    }

    public void setTask(Task task) {
        this.task = task;
        populateFields();
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    private void populateFields() {
        if (task != null) {
            titleField.setText(task.getTitle());
            descriptionArea.setText(task.getDescription());
            priorityChoiceBox.setValue(task.getPriority());
            categoryChoiceBox.setValue(task.getCategory());
            dueDatePicker.setValue(task.getDueDate().toLocalDate());
            createdAtLabel.setText("Создано: " + task.getCreatedAt().toString());
        }
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            task.setTitle(titleField.getText().trim());
            task.setDescription(descriptionArea.getText().trim());
            task.setPriority(priorityChoiceBox.getValue());
            task.setCategory(categoryChoiceBox.getValue());
            
            LocalDateTime newDueDate = dueDatePicker.getValue().atTime(12, 0);
            task.setDueDate(newDueDate);
            
            closeWindow();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean validateInput() {
        if (titleField.getText().trim().isEmpty()) {
            showAlert("Ошибка", "Название задачи не может быть пустым");
            return false;
        }
        
        if (dueDatePicker.getValue() == null) {
            showAlert("Ошибка", "Укажите дату выполнения");
            return false;
        }
        
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}