package view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import model.Task;
import model.Priority;
import model.Category;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @class NewTaskController
 * @brief Контроллер для диалогового окна создания новой задачи
 * 
 * @details Класс управляет интерфейсом создания новой задачи, обрабатывает ввод пользователя,
 *          валидирует данные и создает объект Task. Является частью View слоя архитектуры MVVM.
 * 
 * @author Разработчик
 * @version 1.0
 * @date 2025-11-30
 * 
 * @see Task
 * @see Priority
 * @see Category
 * @see MainController
 * 
 * @note Используется только для создания новых задач, не для редактирования существующих
 */
public class NewTaskController {
    
    /**
     * @brief Поле для ввода заголовка задачи
     * @details JavaFX TextField для ввода краткого названия задачи
     * 
     * @warning Не может быть пустым, проходит обязательную валидацию
     */
    @FXML private TextField titleField;
    
    /**
     * @brief Область для ввода описания задачи
     * @details JavaFX TextArea для многострочного описания задачи
     * 
     * @note Может быть пустым, не проходит обязательную валидацию
     */
    @FXML private TextArea descriptionArea;
    
    /**
     * @brief Выбор даты начала задачи
     * @details JavaFX DatePicker для выбора даты начала выполнения задачи
     * 
     * @default Текущая дата
     */
    @FXML private DatePicker startDatePicker;
    
    /**
     * @brief Выбор часа начала задачи
     * @details JavaFX ComboBox для выбора часа начала (00-23)
     */
    @FXML private ComboBox<String> startHourCombo;
    
    /**
     * @brief Выбор минут начала задачи
     * @details JavaFX ComboBox для выбора минут начала с шагом 5 минут
     */
    @FXML private ComboBox<String> startMinuteCombo;
    
    /**
     * @brief Выбор даты окончания задачи
     * @details JavaFX DatePicker для выбора даты дедлайна задачи
     * 
     * @default Завтрашняя дата
     */
    @FXML private DatePicker endDatePicker;
    
    /**
     * @brief Выбор часа окончания задачи
     * @details JavaFX ComboBox для выбора часа окончания (00-23)
     */
    @FXML private ComboBox<String> endHourCombo;
    
    /**
     * @brief Выбор минут окончания задачи
     * @details JavaFX ComboBox для выбора минут окончания с шагом 5 минут
     */
    @FXML private ComboBox<String> endMinuteCombo;
    
    /**
     * @brief Выбор приоритета задачи
     * @details JavaFX ComboBox для выбора приоритета из значений enum Priority
     * 
     * @default Priority.IMPORTANT
     */
    @FXML private ComboBox<String> priorityCombo;
    
    /**
     * @brief Выбор категории задачи
     * @details JavaFX ComboBox для выбора категории из значений enum Category
     * 
     * @default Category.OTHER
     */
    @FXML private ComboBox<String> categoryCombo;
    
    /**
     * @brief Кнопка создания задачи
     * @details JavaFX Button для подтверждения и создания задачи
     * 
     * @note Активна только при успешной валидации всех полей
     */
    @FXML private Button createButton;
    
    /**
     * @brief Кнопка отмены создания
     * @details JavaFX Button для отмены создания задачи и закрытия диалога
     */
    @FXML private Button cancelButton;
    
    /**
     * @brief Метка для отображения ошибок валидации
     * @details JavaFX Label для отображения сообщений об ошибках валидации формы
     * 
     * @warning Отображается красным цветом при наличии ошибок
     */
    @FXML private Label errorLabel;
    
    /**
     * @brief Созданная задача
     * @details Объект Task, созданный на основе данных из формы
     * 
     * @note Инициализируется только после успешного создания задачи
     */
    private Task createdTask;
    
    /**
     * @brief Флаг успешного создания задачи
     * @details Указывает, была ли задача успешно создана
     * 
     * @value true - задача создана успешно
     * @value false - задача не была создана (отмена или ошибка)
     */
    private boolean taskCreated = false;
    
    /**
     * @brief Инициализация контроллера
     * @details Вызывается автоматически JavaFX после загрузки FXML файла.
     *          Настраивает начальные значения полей формы и инициализирует валидацию.
     * 
     * @note Метод выполняет следующие действия:
     *       1. Устанавливает значения по умолчанию для дат
     *       2. Заполняет выпадающие списки времени
     *       3. Заполняет списки приоритетов и категорий
     *       4. Настраивает слушатель для валидации в реальном времени
     *       5. Выполняет начальную валидацию формы
     * 
     * @see #validateForm()
     * @see Priority#values()
     * @see Category#values()
     */
    @FXML
    public void initialize() {
        // Устанавливаем текущую дату по умолчанию
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        
        // Заполняем ComboBox'ы для времени
        for (int i = 0; i < 24; i++) {
            startHourCombo.getItems().add(String.format("%02d", i));
            endHourCombo.getItems().add(String.format("%02d", i));
        }
        for (int i = 0; i < 60; i += 5) {
            startMinuteCombo.getItems().add(String.format("%02d", i));
            endMinuteCombo.getItems().add(String.format("%02d", i));
        }
        
        // Устанавливаем время по умолчанию (9:00 и 18:00)
        startHourCombo.setValue("09");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("18");
        endMinuteCombo.setValue("00");
        
        // Заполняем ComboBox'ы для приоритета и категории
        for (Priority priority : Priority.values()) {
            priorityCombo.getItems().add(priority.getDisplayName());
        }
        priorityCombo.setValue(Priority.getDefault().getDisplayName());
        
        for (Category category : Category.values()) {
            categoryCombo.getItems().add(category.getDisplayName());
        }
        categoryCombo.setValue(Category.OTHER.getDisplayName());
        
        // Валидация в реальном времени
        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateForm();
        });
        
        // Инициализируем валидацию
        validateForm();
    }
    
    /**
     * @brief Проверяет корректность заполнения формы
     * @details Выполняет валидацию всех обязательных полей формы создания задачи.
     *          Проверяет наличие заголовка и корректность дат.
     * 
     * @return boolean - true если форма валидна, false если есть ошибки
     * 
     * @validation
     *   - Заголовок: не может быть null или пустой строкой
     *   - Даты: обе даты должны быть указаны, дата окончания не может быть раньше даты начала
     * 
     * @note Метод также обновляет интерфейс:
     *       - Устанавливает красную рамку для невалидных полей
     *       - Показывает сообщения об ошибках в errorLabel
     *       - Активирует/деактивирует кнопку создания
     * 
     * @see #errorLabel
     * @see #createButton
     */
    private boolean validateForm() {
        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();
        
        // Проверка заголовка
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            isValid = false;
            errorMessage.append("• Заголовок не может быть пустым\n");
            titleField.setStyle("-fx-border-color: red;");
        } else {
            titleField.setStyle("");
        }
        
        // Проверка дат
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
        
        // Обновляем сообщение об ошибке
        if (!isValid) {
            errorLabel.setText(errorMessage.toString());
            errorLabel.setTextFill(Color.RED);
        } else {
            errorLabel.setText("");
        }
        
        // Активируем/деактивируем кнопку создания
        createButton.setDisable(!isValid);
        
        return isValid;
    }
    
    /**
     * @brief Обработчик кнопки создания задачи
     * @details Вызывается при нажатии кнопки "Создать". Собирает данные из формы,
     *          создает объект Task и закрывает диалоговое окно.
     * 
     * @event OnAction кнопки createButton
     * 
     * @steps
     *   1. Проверяет валидность формы
     *   2. Собирает данные из полей формы
     *   3. Преобразует строковые значения в соответствующие типы
     *   4. Создает объект Task
     *   5. Устанавливает флаг taskCreated в true
     *   6. Закрывает диалоговое окно
     * 
     * @throws NumberFormatException если не удается преобразовать строку времени в число
     * @throws Exception другие возможные ошибки при создании задачи
     * 
     * @see #validateForm()
     * @see Task#Task(String, String, LocalDateTime, LocalDateTime, Priority, Category)
     * @see Priority#fromDisplayName(String)
     * @see Category#fromDisplayName(String)
     */
    @FXML
    private void handleCreateTask() {
        if (!validateForm()) {
            return;
        }
        
        try {
            // Получаем значения из формы
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            
            // Собираем дату и время начала
            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = LocalTime.of(
                Integer.parseInt(startHourCombo.getValue()),
                Integer.parseInt(startMinuteCombo.getValue())
            );
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            
            // Собираем дату и время окончания
            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = LocalTime.of(
                Integer.parseInt(endHourCombo.getValue()),
                Integer.parseInt(endMinuteCombo.getValue())
            );
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
            
            // Получаем приоритет и категорию
            Priority priority = Priority.fromDisplayName(priorityCombo.getValue());
            Category category = Category.fromDisplayName(categoryCombo.getValue());
            
            // Создаем новую задачу
            createdTask = new Task(title, description, startDateTime, endDateTime, priority, category);
            taskCreated = true;
            
            // Закрываем окно
            Stage stage = (Stage) createButton.getScene().getWindow();
            stage.close();
            
        } catch (Exception e) {
            errorLabel.setText("Ошибка при создании задачи: " + e.getMessage());
            errorLabel.setTextFill(Color.RED);
        }
    }
    
    /**
     * @brief Обработчик кнопки отмены
     * @details Вызывается при нажатии кнопки "Отмена". Закрывает диалоговое окно
     *          без создания задачи.
     * 
     * @event OnAction кнопки cancelButton
     * 
     * @note Устанавливает флаг taskCreated в false
     * @see #taskCreated
     */
    @FXML
    private void handleCancel() {
        taskCreated = false;
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * @brief Возвращает созданную задачу
     * @details Геттер для получения созданного объекта Task
     * 
     * @return Task - созданная задача или null если задача не была создана
     * 
     * @note Должен вызываться только после закрытия диалога и проверки isTaskCreated()
     * @see #isTaskCreated()
     */
    public Task getCreatedTask() {
        return createdTask;
    }
    
    /**
     * @brief Проверяет, была ли создана задача
     * @details Возвращает флаг, указывающий на успешное создание задачи
     * 
     * @return boolean - true если задача была создана, false если была отмена или ошибка
     * 
     * @note Используется основным контроллером для определения, нужно ли добавлять задачу в список
     * @see MainController#handleAddTask()
     */
    public boolean isTaskCreated() {
        return taskCreated;
    }
}