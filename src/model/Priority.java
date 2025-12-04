package model;

import java.util.HashMap;
import java.util.Map;

/**
 * @enum Priority
 * @brief Перечисление приоритетов задач
 * 
 * @details Определяет три уровня приоритета с соответствующими цветами и именами иконок.
 * Используется для классификации задач по важности и срочности.
 * Обеспечивает единообразное отображение приоритетов во всем приложении.
 * 
 * @author Разработчик
 * @version 1.0
 * @date 2025-11-30
 * 
 * @see Task
 * @see TaskManager
 * @see view.MainController
 * @see viewmodel.TaskViewModel
 * 
 * @note Приоритет IMPORTANT используется по умолчанию для новых задач
 * @warning Цвета и имена иконок должны соответствовать ресурсам в папке images/
 */
public enum Priority {
    
    /**
     * @brief Срочный приоритет
     * @details Используется для задач с наивысшим приоритетом, требующих немедленного выполнения
     * 
     * @note Цвет: красный (#FF4444)
     * @note Имя иконки: "urgent"
     */
    URGENT("Срочно", "#FF4444", "urgent"),
    
    /**
     * @brief Важный приоритет
     * @details Используется для важных задач, но не требующих немедленного выполнения
     *          Является приоритетом по умолчанию
     * 
     * @note Цвет: желтый (#FFBB33)
     * @note Имя иконки: "important"
     */
    IMPORTANT("Важно", "#FFBB33", "important"),
    
    /**
     * @brief Обычный приоритет
     * @details Используется для задач с нормальным или низким приоритетом
     * 
     * @note Цвет: зеленый (#00C851)
     * @note Имя иконки: "normal"
     */
    NORMAL("Желательно", "#00C851", "normal");
    
    /**
     * @brief Читаемое имя приоритета
     * @details Имя, которое отображается пользователю в интерфейсе
     */
    private final String displayName;
    
    /**
     * @brief Цвет приоритета
     * @details HEX-цвет, используемый для визуального выделения приоритета в интерфейсе
     */
    private final String color;
    
    /**
     * @brief Имя иконки приоритета
     * @details Имя файла иконки (без расширения) для отображения графического представления
     */
    private final String iconName;
    
    /**
     * @brief Карта для быстрого поиска приоритетов по имени
     * @details Статическая карта, связывающая нижний регистр имени приоритета с enum значением
     */
    private static final Map<String, Priority> BY_NAME = new HashMap<>();
    
    /**
     * @brief Статический инициализатор
     * @details Заполняет карту BY_NAME значениями перечисления при загрузке класса
     * 
     * @note Вызывается автоматически при первом обращении к классу Priority
     */
    static {
        for (Priority p : values()) {
            BY_NAME.put(p.displayName.toLowerCase(), p);
        }
    }
    
    /**
     * @brief Конструктор приоритета
     * @details Инициализирует приоритет с заданным именем, цветом и именем иконки
     * 
     * @param displayName Читаемое имя приоритета для отображения
     * @param color HEX-цвет приоритета в формате "#RRGGBB"
     * @param iconName Имя файла иконки (без расширения)
     * 
     * @note Конструктор вызывается автоматически при объявлении enum значений
     */
    Priority(String displayName, String color, String iconName) {
        this.displayName = displayName;
        this.color = color;
        this.iconName = iconName;
    }
    
    /**
     * @brief Возвращает читаемое имя приоритета
     * @details Предоставляет имя приоритета для отображения в пользовательском интерфейсе
     * 
     * @return String Читаемое имя приоритета
     * 
     * @see #displayName
     * @note Используется в TableView и диалоговых окнах
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @brief Возвращает цвет приоритета
     * @details Предоставляет HEX-цвет приоритета для использования в стилях интерфейса
     * 
     * @return String HEX-цвет в формате "#RRGGBB"
     * 
     * @see #color
     * @note Цвет используется в CSS для стилизации элементов интерфейса и в TableView
     */
    public String getColor() {
        return color;
    }
    
    /**
     * @brief Возвращает имя иконки приоритета
     * @details Предоставляет имя файла иконки для графического представления приоритета
     * 
     * @return String Имя файла иконки (без расширения)
     * 
     * @see #iconName
     * @note Имя используется для загрузки соответствующих файлов из папки resources/images/
     */
    public String getIconName() {
        return iconName;
    }
    
    /**
     * @brief Создает приоритет из читаемого имени
     * @details Преобразует строковое имя приоритета в соответствующее enum значение
     * 
     * @param name Читаемое имя приоритета (регистронезависимое)
     * @return Priority Соответствующее enum значение или IMPORTANT, если имя не найдено
     * 
     * @throws NullPointerException если name равен null
     * 
     * @note Поиск выполняется по нижнему регистру имени
     * @note Если имя не найдено, возвращается приоритет IMPORTANT (по умолчанию)
     * 
     * @example
     * Priority p = Priority.fromDisplayName("Срочно"); // возвращает Priority.URGENT
     * Priority p2 = Priority.fromDisplayName("срочно"); // также возвращает Priority.URGENT
     * Priority p3 = Priority.fromDisplayName("Неизвестно"); // возвращает Priority.IMPORTANT
     */
    public static Priority fromDisplayName(String name) {
        return BY_NAME.getOrDefault(name.toLowerCase(), IMPORTANT);
    }
    
    /**
     * @brief Возвращает приоритет по умолчанию
     * @details Предоставляет значение приоритета, используемое по умолчанию для новых задач
     * 
     * @return Priority Приоритет IMPORTANT
     * 
     * @note Всегда возвращает Priority.IMPORTANT
     * @note Используется при создании новых задач, когда приоритет не указан
     * 
     * @see Task#Task(String, String, LocalDateTime, LocalDateTime, Priority, Category)
     * @see view.NewTaskController
     */
    public static Priority getDefault() {
        return IMPORTANT;
    }
}