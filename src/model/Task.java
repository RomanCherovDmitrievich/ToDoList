package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * @class Task
 * @brief Основной класс, представляющий задачу в приложении
 * 
 * @details Класс Task содержит все данные о задаче: заголовок, описание,
 *          даты начала и окончания, приоритет, категорию и статусы выполнения.
 *          Предоставляет методы для проверки просроченности, форматирования времени
 *          и сериализации в JSON формат для сохранения.
 * 
 * @author Чернов
 * @version 1.0
 * @date 2025-11-4
 * 
 * @see Priority
 * @see Category
 * @see TaskManager
 * @see util.JsonUtil
 * 
 * @note Все даты хранятся в формате LocalDateTime
 * @note ID задачи генерируется автоматически при создании через UUID
 */
public class Task {
    
    /**
     * @brief Уникальный идентификатор задачи
     * @details Генерируется автоматически при создании новой задачи
     *          Используется для однозначной идентификации задачи в системе
     */
    private String id;
    
    /**
     * @brief Заголовок задачи
     * @details Краткое название задачи, отображается в таблице
     */
    private String title;
    
    /**
     * @brief Описание задачи
     * @details Подробное описание задачи, может быть многострочным
     */
    private String description;
    
    /**
     * @brief Время начала задачи
     * @details Дата и время, когда задача должна быть начата
     */
    private LocalDateTime startTime;
    
    /**
     * @brief Время окончания задачи (дедлайн)
     * @details Дата и время, к которому задача должна быть выполнена
     *          Используется для определения просроченности
     */
    private LocalDateTime endTime;
    
    /**
     * @brief Приоритет задачи
     * @details Определяет важность и срочность задачи
     */
    private Priority priority;
    
    /**
     * @brief Категория задачи
     * @details Классифицирует задачу по тематике
     */
    private Category category;
    
    /**
     * @brief Статус выполнения задачи
     * @details true - задача выполнена, false - задача активна
     */
    private boolean completed;
    
    /**
     * @brief Статус просроченности задачи
     * @details true - задача просрочена, false - задача в срок
     *          Автоматически обновляется при проверке
     */
    private boolean overdue;
    
    /**
     * @brief Дата создания задачи
     * @details Дата и время, когда задача была создана в системе
     */
    private LocalDateTime createdAt;
    
    /**
     * @brief Форматер для даты и времени
     * @details Используется для сериализации и десериализации дат в JSON
     * Сериализация — это преобразование данных в JSON, например, запись в файл.
     * Десериализация — это обратное преобразование, например, преобразование строки JSON в структуру (или любой другой тип, который может быть записан в формат данных JSON).
     * 
     * @note Формат: "yyyy-MM-dd'T'HH:mm:ss" (ISO-like)
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * @brief Конструктор для создания новой задачи
     * @details Создает задачу с автоматически сгенерированным ID и текущей датой создания
     * 
     * @param title Заголовок задачи (не может быть null или пустым)
     * @param description Описание задачи (может быть пустым)
     * @param startTime Время начала задачи
     * @param endTime Время окончания (дедлайн)
     * @param priority Приоритет задачи (если null, используется IMPORTANT по умолчанию)
     * @param category Категория задачи (если null, используется OTHER по умолчанию)
     * 
     * @throws IllegalArgumentException если title пустой или endTime раньше startTime
     * 
     * @note Приоритет по умолчанию - IMPORTANT, категория - OTHER
     * @note ID генерируется с помощью UUID.randomUUID()
     */
    public Task(String title, String description, LocalDateTime startTime, 
                LocalDateTime endTime, Priority priority, Category category) {
        this.id = UUID.randomUUID().toString(); // UUID — 128-битное значение.
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = (priority != null) ? priority : Priority.getDefault();
        this.category = (category != null) ? category : Category.OTHER;
        this.completed = false;
        this.overdue = false;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * @brief Конструктор для загрузки из JSON
     * @details Создает задачу из данных, загруженных из JSON файла
     *          Проверяет просроченность при создании
     * 
     * @details Возможно, имелся в виду метод parse() класса LocalDateTime в Java, который позволяет преобразовать строку, представляющую дату и время с секундами, в объект LocalDateTime. 
     *          Метод принимает два аргумента:
     *          Первый аргумент — строка, представляющая дату.
     *          Второй аргумент (опциональный) — экземпляр DateTimeFormatter, указывающий любой пользовательский шаблон.
     * 
     * @param id Уникальный идентификатор задачи
     * @param title Заголовок задачи
     * @param description Описание задачи
     * @param startTime Время начала в строковом формате (yyyy-MM-dd'T'HH:mm:ss)
     * @param endTime Время окончания в строковом формате (yyyy-MM-dd'T'HH:mm:ss)
     * @param priority Приоритет в строковом формате (enum name)
     * @param category Категория в строковом формате (enum name)
     * @param completed Статус выполнения
     * @param overdue Статус просроченности
     * @param createdAt Дата создания в строковом формате (yyyy-MM-dd'T'HH:mm:ss)
     * 
     * @throws java.time.format.DateTimeParseException если строки дат не соответствуют формату
     * @throws IllegalArgumentException если priority или category не являются допустимыми enum значениями
     * 
     * @note Используется JsonUtil для десериализации задач из файла
     * @note Автоматически вызывает checkOverdue() после создания
     * 
     * @see util.JsonUtil
     */
    public Task(String id, String title, String description, String startTime,
                String endTime, String priority, String category, 
                boolean completed, boolean overdue, String createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = LocalDateTime.parse(startTime, FORMATTER); // parse() - метод который парсит вводные данные в нужный формат(FORMATTER)
        this.endTime = LocalDateTime.parse(endTime, FORMATTER);
        this.priority = Priority.valueOf(priority); // valueOf() — статический метод класса String в Java, который преобразует различные типы данных в их строковое представление.
        this.category = Category.valueOf(category);
        this.completed = completed;
        this.overdue = overdue;
        this.createdAt = LocalDateTime.parse(createdAt, FORMATTER);
        
        // Проверяем, не просрочена ли задача
        checkOverdue();
    }
    
    /**
     * @brief Проверяет, не просрочена ли задача
     * @details Сравнивает текущее время с дедлайном задачи
     *          Автоматически обновляет поле overdue
     * 
     * @note Метод вызывается при создании задачи из JSON и изменении времени окончания
     * @note Просроченными считаются только невыполненные задачи
     * 
     * @see #setEndTime(LocalDateTime)
     * @see #setCompleted(boolean)
     */
    public void checkOverdue() {
        if (!completed && LocalDateTime.now().isAfter(endTime)) {
            this.overdue = true;
        }
    }
    
    
    /**
     * @brief Возвращает уникальный идентификатор задачи
     * @return String Уникальный ID задачи
     */
    public String getId() { return id; }
    
    /**
     * @brief Возвращает заголовок задачи
     * @return String Заголовок задачи
     */
    public String getTitle() { return title; }
    
    /**
     * @brief Возвращает описание задачи
     * @return String Описание задачи
     */
    public String getDescription() { return description; }
    
    /**
     * @brief Возвращает время начала задачи
     * @return LocalDateTime Время начала
     */
    public LocalDateTime getStartTime() { return startTime; }
    
    /**
     * @brief Возвращает время окончания задачи
     * @return LocalDateTime Время окончания (дедлайн)
     */
    public LocalDateTime getEndTime() { return endTime; }
    
    /**
     * @brief Возвращает приоритет задачи
     * @return Priority Приоритет задачи
     */
    public Priority getPriority() { return priority; }
    
    /**
     * @brief Возвращает категорию задачи
     * @return Category Категория задачи
     */
    public Category getCategory() { return category; }
    
    /**
     * @brief Проверяет, выполнена ли задача
     * @return boolean true если задача выполнена, иначе false
     */
    public boolean isCompleted() { return completed; }
    
    /**
     * @brief Проверяет, просрочена ли задача
     * @return boolean true если задача просрочена, иначе false
     */
    public boolean isOverdue() { return overdue; }
    
    /**
     * @brief Возвращает дату создания задачи
     * @return LocalDateTime Дата и время создания задачи
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    
    /**
     * @brief Устанавливает новый заголовок задачи
     * @param title Новый заголовок задачи
     */
    public void setTitle(String title) { this.title = title; }
    
    /**
     * @brief Устанавливает новое описание задачи
     * @param description Новое описание задачи
     */
    public void setDescription(String description) { this.description = description; }
    
    /**
     * @brief Устанавливает новое время начала задачи
     * @param startTime Новое время начала
     */
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    /**
     * @brief Устанавливает новое время окончания задачи
     * @details При изменении времени окончания автоматически проверяется просроченность
     * 
     * @param endTime Новое время окончания (дедлайн)
     * 
     * @see #checkOverdue()
     */
    public void setEndTime(LocalDateTime endTime) { 
        this.endTime = endTime; 
        checkOverdue();
    }
    
    /**
     * @brief Устанавливает новый приоритет задачи
     * @param priority Новый приоритет
     */
    public void setPriority(Priority priority) { this.priority = priority; }
    
    /**
     * @brief Устанавливает новую категорию задачи
     * @param category Новая категория
     */
    public void setCategory(Category category) { this.category = category; }
    
    /**
     * @brief Устанавливает статус выполнения задачи
     * @details При установке статуса "выполнено" автоматически снимается флаг просроченности
     * 
     * @param completed Новый статус выполнения
     * 
     * @note Если задача помечается как выполненная, она не может быть просроченной
     */
    public void setCompleted(boolean completed) { 
        this.completed = completed; 
        if (completed) {
            this.overdue = false;
        }
    }
    
    /**
     * @brief Возвращает отформатированное время начала
     * @details Форматирует LocalDateTime в читаемую строку
     * 
     * @return String Время начала в формате "dd.MM.yyyy HH:mm"
     * 
     * @note Используется для отображения в пользовательском интерфейсе
     */
    public String getFormattedStartTime() {
        return startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
    
    /**
     * @brief Возвращает отформатированное время окончания
     * @details Форматирует LocalDateTime в читаемую строку
     * 
     * @return String Время окончания в формате "dd.MM.yyyy HH:mm"
     * 
     * @note Используется для отображения в пользовательском интерфейсе
     * @note Отображается в TableView и диалоговых окнах
     */
    public String getFormattedEndTime() {
        return endTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
    
    /**
     * @brief Преобразует задачу в JSON строку
     * @details Сериализует все поля задачи в формат JSON для сохранения в файл
     * 
     * @return JSON строка, представляющая задачу
     * 
     * @note Используется JsonUtil для сохранения задач в файл
     * @warning Не использовать для отображения пользователю
     * 
     * @see util.JsonUtil#saveTasks(List)
     */
    public String toJsonString() {
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        // Экранируем специальные символы
        String escapedTitle = escapeJson(title);
        String escapedDescription = escapeJson(description);
    
        String result = String.format(
            "{\"id\":\"%s\",\"title\":\"%s\",\"description\":\"%s\"," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\",\"priority\":\"%s\"," +
            "\"category\":\"%s\",\"completed\":%s,\"overdue\":%s," +
            "\"createdAt\":\"%s\"}",
            id, 
            escapedTitle, 
            escapedDescription,
            startTime.format(outputFormatter), 
            endTime.format(outputFormatter),
            priority.name(), 
            category.name(),
            completed, 
            overdue, 
            createdAt.format(outputFormatter)
    );
    
    System.out.println("DEBUG toJsonString: " + result);
    return result;
    }
    
    /**
     * @brief Экранирует специальные символы для JSON
     * @details Заменяет символы, которые должны быть экранированы в JSON строке
     * 
     * @param str Исходная строка
     * @return String Строка с экранированными символами
     * 
     * @note Экранирует: кавычки, переносы строк, возвраты каретки, табуляции
     * @private
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t")
              .replace("\b", "\\b")
              .replace("\f", "\\f");
    }
    
    /**
     * @brief Возвращает строковое представление задачи
     * @details Формат: "Заголовок [Приоритет]"
     * 
     * @return String Строковое представление задачи
     * 
     * @note Используется для отладки и логгирования
     */
    @Override
    public String toString() {
        return String.format("%s [%s]", title, priority.getDisplayName());
    }
}