package view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import model.Category;
import model.Priority;
import model.Task;
import util.RecurrenceUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Диалог создания/редактирования задач.
 */
public class NewTaskController {
    @FXML private Label dialogTitleLabel;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;

    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourCombo;
    @FXML private ComboBox<String> startMinuteCombo;

    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourCombo;
    @FXML private ComboBox<String> endMinuteCombo;

    @FXML private ComboBox<String> priorityCombo;
    @FXML private ComboBox<String> categoryCombo;

    @FXML private Spinner<Integer> reminderValueSpinner;
    @FXML private ChoiceBox<String> reminderUnitChoice;

    @FXML private ComboBox<String> recurrenceCombo;
    @FXML private TextField recurrenceRuleField;
    @FXML private DatePicker recurrenceEndPicker;
    @FXML private Label recurrenceHintLabel;

    @FXML private Button createButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabel;

    private Task createdTask;
    private boolean taskCreated = false;
    private Task editingTask;
    private LocalDate prefillDate;

    @FXML
    public void initialize() {
        initializeTimeCombos();
        initializePriorityAndCategory();
        initializeReminder();
        initializeRecurrence();

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        if (prefillDate != null) {
            startDatePicker.setValue(prefillDate);
            endDatePicker.setValue(prefillDate.plusDays(1));
        }

        titleField.textProperty().addListener((observable, oldValue, newValue) -> validateForm());
        validateForm();
    }

    public void setEditingTask(Task task) {
        this.editingTask = task;
        if (task != null) {
            applyTaskToForm(task);
        }
    }

    public void setPrefillDate(LocalDate date) {
        this.prefillDate = date;
        if (date != null && startDatePicker != null) {
            startDatePicker.setValue(date);
            endDatePicker.setValue(date.plusDays(1));
        }
    }

    private void initializeTimeCombos() {
        for (int i = 0; i < 24; i++) {
            startHourCombo.getItems().add(String.format("%02d", i));
            endHourCombo.getItems().add(String.format("%02d", i));
        }
        for (int i = 0; i < 60; i += 5) {
            startMinuteCombo.getItems().add(String.format("%02d", i));
            endMinuteCombo.getItems().add(String.format("%02d", i));
        }

        startHourCombo.setValue("09");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("18");
        endMinuteCombo.setValue("00");
    }

    private void initializePriorityAndCategory() {
        for (Priority priority : Priority.values()) {
            priorityCombo.getItems().add(priority.getDisplayName());
        }
        priorityCombo.setValue(Priority.getDefault().getDisplayName());

        for (Category category : Category.values()) {
            categoryCombo.getItems().add(category.getDisplayName());
        }
        categoryCombo.setValue(Category.OTHER.getDisplayName());
    }

    private void initializeReminder() {
        reminderValueSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10080, 0));
        reminderUnitChoice.getItems().addAll("мин", "час", "день");
        reminderUnitChoice.setValue("мин");
    }

    private void initializeRecurrence() {
        recurrenceCombo.getItems().addAll(
            "Не повторять",
            "Каждый день",
            "Каждую неделю",
            "Каждый будний день",
            "Каждый месяц",
            "Пользовательское правило"
        );
        recurrenceCombo.setValue("Не повторять");
        recurrenceCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateRecurrenceFields());
        updateRecurrenceFields();
    }

    private void updateRecurrenceFields() {
        boolean custom = "Пользовательское правило".equals(recurrenceCombo.getValue());
        recurrenceRuleField.setDisable(!custom);
        recurrenceHintLabel.setDisable(!custom);
    }

    private void applyTaskToForm(Task task) {
        dialogTitleLabel.setText("Редактировать задачу");
        createButton.setText("Сохранить");

        titleField.setText(task.getTitle());
        descriptionArea.setText(task.getDescription());

        startDatePicker.setValue(task.getStartTime().toLocalDate());
        endDatePicker.setValue(task.getEndTime().toLocalDate());

        startHourCombo.setValue(String.format("%02d", task.getStartTime().getHour()));
        startMinuteCombo.setValue(String.format("%02d", task.getStartTime().getMinute()));
        endHourCombo.setValue(String.format("%02d", task.getEndTime().getHour()));
        endMinuteCombo.setValue(String.format("%02d", task.getEndTime().getMinute()));

        priorityCombo.setValue(task.getPriority().getDisplayName());
        categoryCombo.setValue(task.getCategory().getDisplayName());

        int reminderMinutes = task.getReminderOffsetMinutes();
        if (reminderMinutes <= 0) {
            reminderValueSpinner.getValueFactory().setValue(0);
            reminderUnitChoice.setValue("мин");
        } else if (reminderMinutes % (24 * 60) == 0) {
            reminderValueSpinner.getValueFactory().setValue(reminderMinutes / (24 * 60));
            reminderUnitChoice.setValue("день");
        } else if (reminderMinutes % 60 == 0) {
            reminderValueSpinner.getValueFactory().setValue(reminderMinutes / 60);
            reminderUnitChoice.setValue("час");
        } else {
            reminderValueSpinner.getValueFactory().setValue(reminderMinutes);
            reminderUnitChoice.setValue("мин");
        }

        String rule = task.getRecurrenceRule();
        if (rule == null || rule.isBlank() || "NONE".equalsIgnoreCase(rule)) {
            recurrenceCombo.setValue("Не повторять");
            recurrenceRuleField.setText("");
        } else if ("DAILY".equalsIgnoreCase(rule)) {
            recurrenceCombo.setValue("Каждый день");
        } else if ("WEEKLY".equalsIgnoreCase(rule)) {
            recurrenceCombo.setValue("Каждую неделю");
        } else if ("WEEKDAYS".equalsIgnoreCase(rule)) {
            recurrenceCombo.setValue("Каждый будний день");
        } else if ("MONTHLY".equalsIgnoreCase(rule)) {
            recurrenceCombo.setValue("Каждый месяц");
        } else {
            recurrenceCombo.setValue("Пользовательское правило");
            recurrenceRuleField.setText(rule);
        }

        if (task.getRecurrenceEnd() != null) {
            recurrenceEndPicker.setValue(task.getRecurrenceEnd().toLocalDate());
        }

        updateRecurrenceFields();
    }

    private boolean validateForm() {
        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            isValid = false;
            errorMessage.append("• Заголовок не может быть пустым\n");
            titleField.setStyle("-fx-border-color: red;");
        } else {
            titleField.setStyle("");
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            isValid = false;
            errorMessage.append("• Укажите даты начала и окончания\n");
        } else if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            isValid = false;
            errorMessage.append("• Дата окончания не может быть раньше даты начала\n");
            endDatePicker.setStyle("-fx-border-color: red;");
        } else {
            endDatePicker.setStyle("");
        }

        if ("Пользовательское правило".equals(recurrenceCombo.getValue())) {
            String rule = normalizeCustomRule(recurrenceRuleField.getText());
            if (rule.isBlank()) {
                isValid = false;
                errorMessage.append("• Укажите RRULE для повторения\n");
            } else if (RecurrenceUtil.nextOccurrence(LocalDateTime.now(), rule) == null) {
                isValid = false;
                errorMessage.append("• RRULE не распознано. Проверьте формат\n");
            }
        }

        if (!isValid) {
            errorLabel.setText(errorMessage.toString());
            errorLabel.setTextFill(Color.RED);
        } else {
            errorLabel.setText("");
        }

        createButton.setDisable(!isValid);
        return isValid;
    }

    @FXML
    private void handleCreateTask() {
        if (!validateForm()) {
            return;
        }

        try {
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();

            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = LocalTime.of(
                Integer.parseInt(startHourCombo.getValue()),
                Integer.parseInt(startMinuteCombo.getValue())
            );
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);

            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = LocalTime.of(
                Integer.parseInt(endHourCombo.getValue()),
                Integer.parseInt(endMinuteCombo.getValue())
            );
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            Priority priority = Priority.fromDisplayName(priorityCombo.getValue());
            Category category = Category.fromDisplayName(categoryCombo.getValue());

            int reminderMinutes = toMinutes(reminderValueSpinner.getValue(), reminderUnitChoice.getValue());
            String recurrenceRule = buildRecurrenceRule();
            LocalDateTime recurrenceEnd = recurrenceEndPicker.getValue() == null
                ? null
                : recurrenceEndPicker.getValue().atTime(23, 59);

            if (editingTask != null) {
                editingTask.setTitle(title);
                editingTask.setDescription(description);
                editingTask.setStartTime(startDateTime);
                editingTask.setEndTime(endDateTime);
                editingTask.setPriority(priority);
                editingTask.setCategory(category);
                editingTask.setReminderOffsetMinutes(reminderMinutes);
                editingTask.setRecurrenceRule(recurrenceRule);
                editingTask.setRecurrenceEnd(recurrenceEnd);
                createdTask = editingTask;
            } else {
                createdTask = new Task(title, description, startDateTime, endDateTime, priority, category);
                createdTask.setReminderOffsetMinutes(reminderMinutes);
                createdTask.setRecurrenceRule(recurrenceRule);
                createdTask.setRecurrenceEnd(recurrenceEnd);
            }

            taskCreated = true;
            Stage stage = (Stage) createButton.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            errorLabel.setText("Ошибка при создании задачи: " + e.getMessage());
            errorLabel.setTextFill(Color.RED);
        }
    }

    @FXML
    private void handleCancel() {
        taskCreated = false;
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public Task getCreatedTask() {
        return createdTask;
    }

    public boolean isTaskCreated() {
        return taskCreated;
    }

    private int toMinutes(Integer value, String unit) {
        int amount = value == null ? 0 : value;
        if (amount <= 0) {
            return 0;
        }
        if ("час".equals(unit)) {
            return amount * 60;
        }
        if ("день".equals(unit)) {
            return amount * 24 * 60;
        }
        return amount;
    }

    private String buildRecurrenceRule() {
        String selected = recurrenceCombo.getValue();
        if (selected == null || "Не повторять".equals(selected)) {
            return "NONE";
        }
        return switch (selected) {
            case "Каждый день" -> "DAILY";
            case "Каждую неделю" -> "WEEKLY";
            case "Каждый будний день" -> "WEEKDAYS";
            case "Каждый месяц" -> "MONTHLY";
            case "Пользовательское правило" -> normalizeCustomRule(recurrenceRuleField.getText());
            default -> "NONE";
        };
    }

    private String normalizeCustomRule(String rawRule) {
        if (rawRule == null) {
            return "";
        }
        String rule = rawRule.trim();
        if (rule.isBlank()) {
            return "";
        }
        if (!rule.toUpperCase().startsWith("RRULE")) {
            if (rule.toUpperCase().contains("FREQ=")) {
                rule = "RRULE:" + rule;
            }
        }
        return rule;
    }
}
