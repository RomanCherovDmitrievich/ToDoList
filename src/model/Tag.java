package model;

/**
 * @class Tag
 * @brief Класс для представления тега задачи
 * 
 * @details Теги используются для дополнительной категоризации задач.
 * Каждая задача может иметь несколько тегов, а каждый тег может быть
 * привязан к нескольким задачам (связь многие-ко-многим).
 * 
 * @author Чернов
 * @version 1.0
 * @date 2025-11-4
 * 
 * @see Task
 */
public class Tag {
    
    /**
     * @brief Уникальный идентификатор тега
     * @details Генерируется автоматически базой данных
     */
    private int id;
    
    /**
     * @brief Название тега
     * @details Уникальное название тега
     */
    private String name;
    
    /**
     * @brief Цвет тега
     * @details Цвет в формате HEX (#RRGGBB) для отображения
     */
    private String color;
    
    /**
     * @brief Конструктор для создания нового тега
     * @details Используется при создании тега из приложения
     * 
     * @param name Название тега
     * @param color Цвет тега в HEX формате
     */
    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }
    
    /**
     * @brief Конструктор для загрузки из базы данных
     * @details Используется при загрузке тега из БД
     * 
     * @param id Уникальный идентификатор
     * @param name Название тега
     * @param color Цвет тега
     */
    public Tag(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
    
    /**
     * @brief Возвращает уникальный идентификатор тега
     * @return int ID тега
     */
    public int getId() {
        return id;
    }
    
    /**
     * @brief Возвращает название тега
     * @return String Название тега
     */
    public String getName() {
        return name;
    }
    
    /**
     * @brief Возвращает цвет тега
     * @return String Цвет в HEX формате
     */
    public String getColor() {
        return color;
    }
    
    /**
     * @brief Устанавливает название тега
     * @param name Новое название тега
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @brief Устанавливает цвет тега
     * @param color Новый цвет тега в HEX формате
     */
    public void setColor(String color) {
        this.color = color;
    }
    
    /**
     * @brief Возвращает строковое представление тега
     * @return String Название тега
     */
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * @brief Сравнивает теги по названию
     * @param obj Объект для сравнения
     * @return true если названия совпадают, иначе false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return name.equals(tag.name);
    }
    
    /**
     * @brief Возвращает хэш-код тега
     * @return int Хэш-код на основе названия
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}