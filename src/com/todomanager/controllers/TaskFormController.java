package com.todomanager.controllers;

import com.todomanager.managers.TaskManager;
import com.todomanager.models.Task;
import com.todomanager.models.TaskPriority;
import com.todomanager.exceptions.TaskException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalTime;

public class TaskFormController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> hourComboBox;
    @FXML private ComboBox<String> minuteComboBox;
    @FXML private ComboBox<TaskPriority> priorityComboBox;
    @FXML private TextField categoryField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label formTitle;
    @FXML private Label errorLabel;
    
    private TaskManager taskManager;
    private MainController mainController;
    private Task taskForEditing;
    
    public void initialize() {
        setupTimeComboBoxes();
        setupPriorityComboBox();
        setupDatePicker();
        
        // Обработчики событий для валидации
        titleField.textProperty().addListener((observable, oldValue, newValue) -> validateForm());
        dueDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> validateForm());
    }
    
    private void setupTimeComboBoxes() {
        // Заполняем часы
        for (int i = 0; i < 24; i++) {
            hourComboBox.getItems().add(String.format("%02d", i));
        }
        
        // Заполняем минуты
        for (int i = 0; i < 60; i += 5) {
            minuteComboBox.getItems().add(String.format("%02d", i));
        }
        
        // Устанавливаем значения по умолчанию
        hourComboBox.setValue("09");
        minuteComboBox.setValue("00");
    }
    
    private void setupPriorityComboBox() {
        priorityComboBox.getItems().setAll(TaskPriority.values());
        priorityComboBox.setValue(TaskPriority.MEDIUM);
    }
    
    private void setupDatePicker() {
        dueDatePicker.setValue(LocalDate.now());
        dueDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }
    
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    public void setTaskForEditing(Task task) {
        this.taskForEditing = task;
        populateFormWithTaskData();
    }
    
    private void populateFormWithTaskData() {
        if (taskForEditing != null) {
            formTitle.setText("Редактирование задачи");
            saveButton.setText("Сохранить изменения");
            
            titleField.setText(taskForEditing.getTitle());
            descriptionArea.setText(taskForEditing.getDescription());
            dueDatePicker.setValue(taskForEditing.getDueDate());
            
            if (taskForEditing.getDueTime() != null) {
                hourComboBox.setValue(String.format("%02d", taskForEditing.getDueTime().getHour()));
                minuteComboBox.setValue(String.format("%02d", taskForEditing.getDueTime().getMinute()));
            }
            
            priorityComboBox.setValue(taskForEditing.getPriority());
            categoryField.setText(taskForEditing.getCategory());
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            if (!validateForm()) {
                return;
            }
            
            Task task;
            if (taskForEditing == null) {
                // Создание новой задачи
                task = new Task();
            } else {
                // Редактирование существующей задачи
                task = taskForEditing;
            }
            
            // Установка значений через сеттеры (происходит валидация)
            task.setTitle(titleField.getText());
            task.setDescription(descriptionArea.getText());
            task.setDueDate(dueDatePicker.getValue());
            
            // Установка времени
            if (hourComboBox.getValue() != null && minuteComboBox.getValue() != null) {
                LocalTime time = LocalTime.of(
                    Integer.parseInt(hourComboBox.getValue()),
                    Integer.parseInt(minuteComboBox.getValue())
                );
                task.setDueTime(time);
            } else {
                task.setDueTime(null);
            }
            
            task.setPriority(priorityComboBox.getValue());
            task.setCategory(categoryField.getText().isEmpty() ? "Общие" : categoryField.getText());
            
            // Сохранение задачи
            if (taskForEditing == null) {
                taskManager.addTask(task);
            } else {
                taskManager.updateTask(task);
            }
            
            closeForm();
            
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (TaskException e) {
            showError("Ошибка сохранения: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        closeForm();
    }
    
    private boolean validateForm() {
        String title = titleField.getText();
        LocalDate dueDate = dueDatePicker.getValue();
        
        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();
        
        // Проверка названия
        if (title == null || title.trim().isEmpty()) {
            isValid = false;
            errorMessage.append("• Название задачи обязательно\n");
        } else if (title.length() > 100) {
            isValid = false;
            errorMessage.append("• Название не должно превышать 100 символов\n");
        }
        
        // Проверка даты
        if (dueDate == null) {
            isValid = false;
            errorMessage.append("• Дата выполнения обязательна\n");
        } else if (dueDate.isBefore(LocalDate.now())) {
            isValid = false;
            errorMessage.append("• Дата не может быть в прошлом\n");
        }
        
        // Проверка описания
        String description = descriptionArea.getText();
        if (description != null && description.length() > 500) {
            isValid = false;
            errorMessage.append("• Описание не должно превышать 500 символов\n");
        }
        
        if (isValid) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            saveButton.setDisable(false);
        } else {
            showError(errorMessage.toString());
            saveButton.setDisable(true);
        }
        
        return isValid;
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void closeForm() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
        
        if (mainController != null) {
            mainController.updateDisplay();
            mainController.updateStatistics();
        }
    }
}