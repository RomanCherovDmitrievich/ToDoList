package viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.Task;
import model.Priority;
import model.Category;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @class TaskViewModel
 * @brief ViewModel для отображения задачи в пользовательском интерфейсе
 * 
 * @details Реализует паттерн ViewModel в архитектуре MVVM. Служит промежуточным слоем
 *          между моделью данных (Task) и представлением (JavaFX UI). Предоставляет
 *          JavaFX свойства для двусторонней привязки данных и форматирует данные
 *          для удобного отображения в таблице.
 * 
 * @note Все свойства являются Observable, что позволяет автоматически обновлять UI
 *       при изменении данных и наоборот.
 * 
 * @see Task
 * @see Priority
 * @see Category
 * @see javafx.beans.property
 * @since Версия 1.0
 */
public class TaskViewModel {
    /** @brief Уникальный идентификатор задачи (неизменяемый) */
    private final StringProperty id;
    
    /** @brief Заголовок задачи */
    private final StringProperty title;
    
    /** @brief Описание задачи */
    private final StringProperty description;
    
    /** @brief Время начала выполнения задачи */
    private final ObjectProperty<LocalDateTime> startTime;
    
    /** @brief Время окончания (дедлайн) задачи */
    private final ObjectProperty<LocalDateTime> endTime;
    
    /** @brief Приоритет задачи (URGENT, IMPORTANT, NORMAL) */
    private final ObjectProperty<Priority> priority;
    
    /** @brief Категория задачи (WORK, HOME, STUDY, OTHER) */
    private final ObjectProperty<Category> category;
    
    /** @brief Флаг выполнения задачи */
    private final BooleanProperty completed;
    
    /** @brief Флаг просроченности задачи */
    private final BooleanProperty overdue;
    
    /** @brief Дата и время создания задачи */
    private final ObjectProperty<LocalDateTime> createdAt;
    
    /** @brief Ссылка на исходную модель данных Task */
    private final Task task;
    
    /**
     * @brief Форматер для полного отображения даты и времени
     * @value "dd.MM.yyyy HH:mm" (пример: "31.12.2025 23:59")
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    /**
     * @brief Форматер для отображения только даты
     * @value "dd.MM.yyyy" (пример: "31.12.2025")
     */
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    /**
     * @brief Форматер для отображения только времени
     * @value "HH:mm" (пример: "23:59")
     */
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * @brief Конструктор на основе модели Task
     * @param task Исходный объект задачи
     * 
     * @details Создает ViewModel, инициализируя все JavaFX свойства значениями
     *          из переданного объекта Task. Устанавливает двусторонние привязки
     *          между свойствами ViewModel и полями модели.
     * 
     * @param task Объект модели Task, не может быть null
     * @throws NullPointerException если task равен null
     * 
     * @note После создания ViewModel изменения в свойствах автоматически
     *       синхронизируются с исходной моделью Task.
     * 
     * @see #setupBindings()
     */
    public TaskViewModel(Task task) {
        this.task = task;
        
        // Инициализация всех свойств значениями из модели Task
        this.id = new SimpleStringProperty(task.getId());
        this.title = new SimpleStringProperty(task.getTitle());
        this.description = new SimpleStringProperty(task.getDescription());
        this.startTime = new SimpleObjectProperty<>(task.getStartTime());
        this.endTime = new SimpleObjectProperty<>(task.getEndTime());
        this.priority = new SimpleObjectProperty<>(task.getPriority());
        this.category = new SimpleObjectProperty<>(task.getCategory());
        this.completed = new SimpleBooleanProperty(task.isCompleted());
        this.overdue = new SimpleBooleanProperty(task.isOverdue());
        this.createdAt = new SimpleObjectProperty<>(task.getCreatedAt());
        
        // Настройка двусторонних привязок между ViewModel и Model
        setupBindings();
    }
    
    /**
     * @brief Настраивает двусторонние привязки между ViewModel и Model
     * @private
     * 
     * @details Устанавливает слушатели изменений для всех свойств ViewModel.
     *          При изменении любого свойства в UI (например, редактирование в таблице)
     *          соответствующее изменение применяется к исходной модели Task.
     * 
     * @note Привязки обеспечивают автоматическую синхронизацию данных между
     *       представлением и моделью без необходимости ручного обновления.
     * 
     * @warning Изменение поля id не поддерживается, так как ID неизменяем
     * 
     * @see javafx.beans.property.Property#addListener
     */
    private void setupBindings() {
        // Привязка заголовка: UI → Model
        title.addListener((observable, oldValue, newValue) -> {
            task.setTitle(newValue);
        });
        
        // Привязка описания: UI → Model
        description.addListener((observable, oldValue, newValue) -> {
            task.setDescription(newValue);
        });
        
        // Привязка времени начала: UI → Model
        startTime.addListener((observable, oldValue, newValue) -> {
            task.setStartTime(newValue);
        });
        
        // Привязка времени окончания: UI → Model
        endTime.addListener((observable, oldValue, newValue) -> {
            task.setEndTime(newValue);
        });
        
        // Привязка приоритета: UI → Model
        priority.addListener((observable, oldValue, newValue) -> {
            task.setPriority(newValue);
        });
        
        // Привязка категории: UI → Model
        category.addListener((observable, oldValue, newValue) -> {
            task.setCategory(newValue);
        });
        
        // Привязка статуса выполнения: UI → Model
        completed.addListener((observable, oldValue, newValue) -> {
            task.setCompleted(newValue);
            // При изменении статуса выполнения обновляем просроченность
            if (newValue) {
                overdue.set(false);
            }
        });
    }
    
    // ====================================================
    // Геттеры и сеттеры свойств (для привязки в FXML)
    // ====================================================
    
    /**
     * @brief Возвращает идентификатор задачи
     * @return Уникальный идентификатор задачи (String)
     */
    public String getId() {
        return id.get();
    }
    
    /**
     * @brief Возвращает свойство идентификатора задачи
     * @return StringProperty для привязки в FXML
     */
    public StringProperty idProperty() {
        return id;
    }
    
    /**
     * @brief Возвращает заголовок задачи
     * @return Заголовок задачи
     */
    public String getTitle() {
        return title.get();
    }
    
    /**
     * @brief Возвращает свойство заголовка задачи
     * @return StringProperty для привязки в FXML
     */
    public StringProperty titleProperty() {
        return title;
    }
    
    /**
     * @brief Устанавливает новый заголовок задачи
     * @param title Новый заголовок задачи
     */
    public void setTitle(String title) {
        this.title.set(title);
    }
    
    /**
     * @brief Возвращает описание задачи
     * @return Описание задачи
     */
    public String getDescription() {
        return description.get();
    }
    
    /**
     * @brief Возвращает свойство описания задачи
     * @return StringProperty для привязки в FXML
     */
    public StringProperty descriptionProperty() {
        return description;
    }
    
    /**
     * @brief Устанавливает новое описание задачи
     * @param description Новое описание задачи
     */
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    /**
     * @brief Возвращает время начала задачи
     * @return LocalDateTime время начала
     */
    public LocalDateTime getStartTime() {
        return startTime.get();
    }
    
    /**
     * @brief Возвращает свойство времени начала задачи
     * @return ObjectProperty<LocalDateTime> для привязки в FXML
     */
    public ObjectProperty<LocalDateTime> startTimeProperty() {
        return startTime;
    }
    
    /**
     * @brief Устанавливает новое время начала задачи
     * @param startTime Новое время начала
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime.set(startTime);
    }
    
    /**
     * @brief Возвращает время окончания (дедлайн) задачи
     * @return LocalDateTime время окончания
     */
    public LocalDateTime getEndTime() {
        return endTime.get();
    }
    
    /**
     * @brief Возвращает свойство времени окончания задачи
     * @return ObjectProperty<LocalDateTime> для привязки в FXML
     */
    public ObjectProperty<LocalDateTime> endTimeProperty() {
        return endTime;
    }
    
    /**
     * @brief Устанавливает новое время окончания задачи
     * @param endTime Новое время окончания
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime.set(endTime);
    }
    
    /**
     * @brief Возвращает приоритет задачи
     * @return Приоритет задачи (enum Priority)
     */
    public Priority getPriority() {
        return priority.get();
    }
    
    /**
     * @brief Возвращает свойство приоритета задачи
     * @return ObjectProperty<Priority> для привязки в FXML
     */
    public ObjectProperty<Priority> priorityProperty() {
        return priority;
    }
    
    /**
     * @brief Устанавливает новый приоритет задачи
     * @param priority Новый приоритет
     */
    public void setPriority(Priority priority) {
        this.priority.set(priority);
    }
    
    /**
     * @brief Возвращает категорию задачи
     * @return Категория задачи (enum Category)
     */
    public Category getCategory() {
        return category.get();
    }
    
    /**
     * @brief Возвращает свойство категории задачи
     * @return ObjectProperty<Category> для привязки в FXML
     */
    public ObjectProperty<Category> categoryProperty() {
        return category;
    }
    
    /**
     * @brief Устанавливает новую категорию задачи
     * @param category Новая категория
     */
    public void setCategory(Category category) {
        this.category.set(category);
    }
    
    /**
     * @brief Проверяет, выполнена ли задача
     * @return true если задача выполнена, false в противном случае
     */
    public boolean isCompleted() {
        return completed.get();
    }
    
    /**
     * @brief Возвращает свойство статуса выполнения задачи
     * @return BooleanProperty для привязки в FXML (например, к CheckBox)
     */
    public BooleanProperty completedProperty() {
        return completed;
    }
    
    /**
     * @brief Устанавливает статус выполнения задачи
     * @param completed Новый статус выполнения
     * 
     * @note При установке completed в true автоматически сбрасывается флаг overdue
     */
    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }
    
    /**
     * @brief Проверяет, просрочена ли задача
     * @return true если задача просрочена, false в противном случае
     */
    public boolean isOverdue() {
        return overdue.get();
    }
    
    /**
     * @brief Возвращает свойство просроченности задачи
     * @return BooleanProperty для привязки в FXML
     */
    public BooleanProperty overdueProperty() {
        return overdue;
    }
    
    /**
     * @brief Возвращает дату и время создания задачи
     * @return LocalDateTime создания задачи
     */
    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }
    
    /**
     * @brief Возвращает свойство даты создания задачи
     * @return ObjectProperty<LocalDateTime> для привязки в FXML
     */
    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }
    
    // ====================================================
    // Методы для удобного отображения в таблице
    // ====================================================
    
    /**
     * @brief Возвращает отформатированное время начала задачи
     * @return Строка в формате "dd.MM.yyyy HH:mm"
     * 
     * @example "31.12.2025 09:00"
     * @see #DATE_TIME_FORMATTER
     */
    public String getFormattedStartTime() {
        return startTime.get().format(DATE_TIME_FORMATTER);
    }
    
    /**
     * @brief Возвращает отформатированное время окончания задачи
     * @return Строка в формате "dd.MM.yyyy HH:mm"
     * 
     * @example "31.12.2025 18:00"
     * @see #DATE_TIME_FORMATTER
     */
    public String getFormattedEndTime() {
        return endTime.get().format(DATE_TIME_FORMATTER);
    }
    
    /**
     * @brief Возвращает только дату окончания задачи
     * @return Строка в формате "dd.MM.yyyy"
     * 
     * @example "31.12.2025"
     * @see #DATE_FORMATTER
     */
    public String getFormattedEndDate() {
        return endTime.get().format(DATE_FORMATTER);
    }
    
    /**
     * @brief Возвращает только время окончания задачи
     * @return Строка в формате "HH:mm"
     * 
     * @example "18:00"
     * @see #TIME_FORMATTER
     */
    public String getFormattedEndTimeOnly() {
        return endTime.get().format(TIME_FORMATTER);
    }
    
    /**
     * @brief Возвращает отображаемое имя приоритета
     * @return Локализованное имя приоритета ("Срочно", "Важно", "Желательно")
     * 
     * @see Priority#getDisplayName()
     */
    public String getPriorityDisplay() {
        return priority.get().getDisplayName();
    }
    
    /**
     * @brief Возвращает цвет приоритета для CSS стилей
     * @return HEX-код цвета в формате "#RRGGBB"
     * 
     * @example "#FF4444" для URGENT (красный)
     * @see Priority#getColor()
     */
    public String getPriorityColor() {
        return priority.get().getColor();
    }
    
    /**
     * @brief Возвращает имя иконки приоритета
     * @return Имя файла иконки для данного приоритета
     * 
     * @example "urgent" для URGENT приоритета
     * @see Priority#getIconName()
     */
    public String getPriorityIconName() {
        return priority.get().getIconName();
    }
    
    /**
     * @brief Возвращает отображаемое имя категории
     * @return Локализованное имя категории ("Работа", "Дом", "Учёба", "Другое")
     * 
     * @see Category#getDisplayName()
     */
    public String getCategoryDisplay() {
        return category.get().getDisplayName();
    }
    
    /**
     * @brief Возвращает цвет категории для CSS стилей
     * @return HEX-код цвета в формате "#RRGGBB"
     * 
     * @example "#3D5AFE" для WORK (синий)
     * @see Category#getColor()
     */
    public String getCategoryColor() {
        return category.get().getColor();
    }
    
    /**
     * @brief Возвращает оставшееся время до дедлайна в читаемом формате
     * @return Текстовое описание оставшегося времени
     * 
     * @details Возвращает строку в зависимости от статуса задачи:
     *          - Для просроченных задач: "Просрочено сегодня/вчера/N дн. назад"
     *          - Для будущих задач: "Сегодня/Завтра/Через N дн."
     * 
     * @example "Просрочено вчера", "Сегодня", "Через 3 дн."
     */
    public String getTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = endTime.get();
        
        if (now.isAfter(deadline)) {
            // Задача просрочена
            long days = now.toLocalDate().toEpochDay() - deadline.toLocalDate().toEpochDay();
            if (days == 0) {
                return "Просрочено сегодня";
            } else if (days == 1) {
                return "Просрочено вчера";
            } else {
                return "Просрочено " + days + " дн. назад";
            }
        } else {
            // Задача еще не просрочена
            long days = deadline.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay();
            if (days == 0) {
                return "Сегодня";
            } else if (days == 1) {
                return "Завтра";
            } else {
                return "Через " + days + " дн.";
            }
        }
    }
    
    /**
     * @brief Возвращает статус задачи в виде строки
     * @return Текстовое описание статуса задачи
     * 
     * @details Возвращает один из трех статусов:
     *          - "Выполнено" если задача завершена
     *          - "Просрочено" если задача просрочена и не выполнена
     *          - "Активно" в остальных случаях
     * 
     * @return "Выполнено" | "Просрочено" | "Активно"
     */
    public String getStatusText() {
        if (completed.get()) {
            return "Выполнено";
        } else if (overdue.get()) {
            return "Просрочено";
        } else {
            return "Активно";
        }
    }
    
    /**
     * @brief Возвращает базовую модель задачи
     * @return Исходный объект Task, связанный с этим ViewModel
     * 
     * @note Может использоваться для операций, требующих прямой работы с моделью,
     *       например, сохранение в JSON или сложные вычисления.
     */
    public Task getTask() {
        return task;
    }
    
    /**
     * @brief Обновляет ViewModel на основе изменений в модели
     * @details Синхронизирует все свойства ViewModel с текущими значениями
     *          из связанного объекта Task. Используется при внешних изменениях
     *          модели, которые не были инициированы через UI.
     * 
     * @note Этот метод следует вызывать при изменении модели Task извне,
     *       например, при загрузке задач из файла или изменении через TaskManager.
     * 
     * @see TaskManager
     * @see util.JsonUtil
     */
    public void updateFromModel() {
        title.set(task.getTitle());
        description.set(task.getDescription());
        startTime.set(task.getStartTime());
        endTime.set(task.getEndTime());
        priority.set(task.getPriority());
        category.set(task.getCategory());
        completed.set(task.isCompleted());
        overdue.set(task.isOverdue());
    }
    
    /**
     * @brief Возвращает строковое представление ViewModel
     * @return Строка в формате "TaskViewModel{title='...', priority=..., completed=...}"
     * 
     * @overrides Object.toString()
     */
    @Override
    public String toString() {
        return String.format("TaskViewModel{title='%s', priority=%s, completed=%s}", 
            title.get(), priority.get().getDisplayName(), completed.get());
    }
    
    /**
     * @brief Сравнивает два объекта TaskViewModel на равенство
     * @param obj Объект для сравнения
     * @return true если объекты равны, false в противном случае
     * 
     * @details Два TaskViewModel считаются равными, если их идентификаторы (id) равны.
     * 
     * @overrides Object.equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TaskViewModel that = (TaskViewModel) obj;
        return id.get().equals(that.id.get());
    }
    
    /**
     * @brief Возвращает хэш-код объекта
     * @return Хэш-код, основанный на идентификаторе задачи
     * 
     * @overrides Object.hashCode()
     */
    @Override
    public int hashCode() {
        return id.get().hashCode();
    }
}