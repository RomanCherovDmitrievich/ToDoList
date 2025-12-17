package model;

import java.util.HashMap;
import java.util.Map;

/**
 * @enum Category
 * @brief Перечисление категорий задач
 * 
 * @details Категории используются для классификации задач по тематике.
 * Каждая категория имеет читаемое имя для отображения и цвет для визуального выделения.
 * Предоставляет методы для удобного получения категорий по их именам.
 * Enum (перечисление) в Java — это специальный тип данных, который позволяет создавать переменные, значения которых ограничены строго определённым списком.
 * 
 * @author Чернов
 * @version 1.0
 * @date 2025-11-4
 * 
 * @see Task
 * @see TaskManager
 * @see view.MainController
 * 
 * @note Цвета задаются в формате HEX для использования в CSS
 * @note Категория OTHER используется по умолчанию при неизвестном имени
 */
public enum Category { 
    
    /**
     * @brief Рабочая категория
     * @details Используется для задач, связанных с работой или профессиональной деятельностью
     */
    WORK("Работа", "#3D5AFE"),
    
    /**
     * @brief Домашняя категория
     * @details Используется для личных и бытовых задач
     */
    HOME("Дом", "#FF4081"),
    
    /**
     * @brief Учебная категория
     * @details Используется для задач, связанных с обучением и образованием
     */
    STUDY("Учёба", "#6200EA"),
    
    /**
     * @brief Другая категория
     * @details Используется для задач, которые не подходят под другие категории
     *          Является категорией по умолчанию
     */
    OTHER("Другое", "#757575");
    
    /**
     * @brief Читаемое имя категории
     * @details Имя, которое отображается пользователю в интерфейсе
     */
    private final String displayName;
    
    /**
     * @brief Цвет категории
     * @details HEX-цвет, используемый для визуального выделения категории в интерфейсе
     */
    private final String color;
    
    /**
     * @brief Карта для быстрого поиска категорий по имени
     * @details Статическая карта, связывающая нижний регистр имени категории с enum значением
     */
    private static final Map<String, Category> BY_NAME = new HashMap<>();
    
    /**
     * @brief Статический инициализатор
     * @details Заполняет карту BY_NAME значениями перечисления при загрузке класса
     * 
     * @note Вызывается автоматически при первом обращении к классу Category
     */
    static {
        for (Category c : values()) { // Values() — статический метод в Java, который возвращает массив всех констант перечисления (enum).
            BY_NAME.put(c.displayName.toLowerCase(), c);//toLowerCase() - делает из больших букв маленький
        }
    }
    
    /**
     * @brief Конструктор категории
     * @details Инициализирует категорию с заданным именем и цветом
     * 
     * @param displayName Читаемое имя категории для отображения
     * @param color HEX-цвет категории в формате "#RRGGBB"
     * 
     * @note Конструктор вызывается автоматически при объявлении enum значений
     */
    Category(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    /**
     * @brief Возвращает читаемое имя категории
     * @details Предоставляет имя категории для отображения в пользовательском интерфейсе
     * 
     * @return String Читаемое имя категории
     * 
     * @see #displayName
     * @note Используется в TableView и диалоговых окнах
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @brief Возвращает цвет категории
     * @details Предоставляет HEX-цвет категории для использования в стилях интерфейса
     * 
     * @return String HEX-цвет в формате "#RRGGBB"
     * 
     * @see #color
     * @note Цвет используется в CSS для стилизации элементов интерфейса
     */
    public String getColor() {
        return color;
    }
    
    /**
     * @brief Создает категорию из читаемого имени
     * @details Преобразует строковое имя категории в соответствующее enum значение
     * getOrDefault — метод класса HashMap в Java, который возвращает значение, 
     * ассоциированное с указанным ключом, если он существует в карте. 
     * Если ключа нет, метод возвращает значение по умолчанию, указанное пользователем.
     * 
     * @param name Читаемое имя категории (регистронезависимое)
     * @return Category Соответствующее enum значение или OTHER, если имя не найдено
     * 
     * @throws NullPointerException если name равен null
     * 
     * @note Поиск выполняется по нижнему регистру имени
     * @note Если имя не найдено, возвращается категория OTHER
     * 
     * @example
     * Category cat = Category.fromDisplayName("Работа"); // возвращает Category.WORK
     * Category cat2 = Category.fromDisplayName("работа"); // также возвращает Category.WORK
     * Category cat3 = Category.fromDisplayName("Неизвестно"); // возвращает Category.OTHER
     */
    public static Category fromDisplayName(String name) {
        return BY_NAME.getOrDefault(name.toLowerCase(), OTHER);
    }
}