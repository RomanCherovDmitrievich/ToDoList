package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @class TaskManager
 * @brief Менеджер задач - центральный класс для управления задачами
 * 
 * @details Класс TaskManager реализует паттерн Singleton и служит центральным
 *          хранилищем и менеджером для всех задач в приложении. Отвечает за
 *          операции CRUD (Create, Read, Update, Delete) с задачами, поиск,
 *          фильтрацию и проверку статусов.
 * 
 * @author Разработчик
 * @version 2.0
 * @date 2025-12-01
 * 
 * @implements Singleton паттерн
 * 
 * @see Task
 * @see Priority
 * @see Category
 * @see util.JsonUtil
 * 
 * @note Все методы возвращают неизменяемые списки для обеспечения инкапсуляции
 * @warning Для доступа к экземпляру используйте только getInstance()
 * @bug Исправлены проблемы с потокобезопасностью и инкапсуляцией
 */
public class TaskManager {
    
    /**
     * @brief Список всех задач
     * @details Внутреннее хранилище задач приложения
     * 
     * @note Использует ArrayList для быстрого доступа по индексу
     * @warning Не использовать напрямую, только через методы класса
     */
    private List<Task> tasks;
    
    /**
     * @brief Единственный экземпляр TaskManager
     * @details Реализация паттерна Singleton
     * 
     * @note Инициализируется лениво (при первом вызове getInstance())
     * @see #getInstance()
     */
    private static TaskManager instance;
    
    /**
     * @brief Приватный конструктор
     * @details Предотвращает создание экземпляров извне класса
     * 
     * @note Инициализирует пустой список задач
     * @warning Только для внутреннего использования
     */
    private TaskManager() {
        this.tasks = new ArrayList<>();
    }
    
    /**
     * @brief Возвращает единственный экземпляр TaskManager
     * @details Реализация паттерна Singleton с ленивой инициализацией
     * 
     * @return TaskManager Единственный экземпляр менеджера задач
     * 
     * @note Использует synchronized для потокобезопасности
     * @warning Всегда используйте этот метод для получения экземпляра
     * 
     * @see Singleton паттерн
     */
    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }
    
    /**
     * @brief Добавляет новую задачу в менеджер
     * @details Добавляет задачу в внутренний список и проверяет все задачи на просроченность
     * 
     * @param task Задача для добавления (не может быть null)
     * 
     * @throws NullPointerException если task равен null
     * 
     * @note После добавления автоматически проверяет все задачи на просроченность
     * @see #checkAllTasksOverdue()
     */
    public void addTask(Task task) {
        tasks.add(task);
        checkAllTasksOverdue();
    }
    
    /**
     * @brief Удаляет задачу по её идентификатору
     * @details Находит и удаляет задачу с указанным ID из списка
     * 
     * @param taskId Уникальный идентификатор задачи для удаления
     * @return true если задача была найдена и удалена, false если задача не найдена
     * 
     * @note Использует Stream API для поиска задачи по ID
     * @note ID задачи генерируется при создании (UUID)
     * 
     * @see Task#getId()
     */
    public boolean removeTask(String taskId) {
        return tasks.removeIf(task -> task.getId().equals(taskId));
    }
    
    /**
     * @brief Возвращает неизменяемую копию списка всех задач
     * @details Предоставляет защищенную неизменяемую копию списка задач для предотвращения модификаций извне
     * 
     * @return List<Task> Неизменяемый список, содержащий все задачи
     * 
     * @note Перед возвратом проверяет все задачи на просроченность
     * @note Возвращает неизменяемую копию для обеспечения инкапсуляции
     * @note Использует Collections.unmodifiableList для создания защищенного представления
     * 
     * @see #checkAllTasksOverdue()
     * @see Collections#unmodifiableList(List)
     */
    public List<Task> getAllTasks() {
        checkAllTasksOverdue();
        // Возвращаем неизменяемый список
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }
    
    /**
     * @brief Фильтрует задачи по статусу выполнения
     * @details Возвращает список задач, отфильтрованный по статусу выполнения
     * 
     * @param completed true для выполненных задач, false для невыполненных
     * @return List<Task> Неизменяемый список задач с указанным статусом выполнения
     * 
     * @note Использует Stream API для фильтрации
     * @note Возвращает неизменяемую копию (оригинальный список не модифицируется)
     * @note Использует Collections.unmodifiableList для создания защищенного представления
     * 
     * @see Task#isCompleted()
     * @see Stream#filter(Predicate)
     * @see Collections#unmodifiableList(List)
     */
    public List<Task> getTasksByCompletion(boolean completed) {
        List<Task> filteredTasks = tasks.stream()
            .filter(task -> task.isCompleted() == completed)
            .collect(Collectors.toList());
        
        // Возвращаем неизменяемый список
        return Collections.unmodifiableList(new ArrayList<>(filteredTasks));
    }
    
    /**
     * @brief Выполняет поиск задач по тексту
     * @details Ищет задачи, в заголовке или описании которых содержится указанный текст
     * 
     * @param query Текст для поиска (может быть null или пустым)
     * @return List<Task> Неизменяемый список найденных задач или все задачи если query пустой
     * 
     * @note Поиск не чувствителен к регистру (все приводится к нижнему регистру)
     * @note Если query null или пустой, возвращает копию всех задач
     * @note Ищет как в заголовке, так и в описании задачи
     * @note Возвращает неизменяемую копию для обеспечения инкапсуляции
     * 
     * @see Task#getTitle()
     * @see Task#getDescription()
     * @see String#toLowerCase()
     * @see String#contains(CharSequence)
     * @see Collections#unmodifiableList(List)
     */
    public List<Task> searchTasks(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Возвращаем копию всех задач
            return Collections.unmodifiableList(new ArrayList<>(tasks));
        }
        
        String lowerQuery = query.toLowerCase();
        List<Task> foundTasks = tasks.stream()
            .filter(task -> task.getTitle().toLowerCase().contains(lowerQuery) ||
                           task.getDescription().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
        
        // Возвращаем неизменяемый список
        return Collections.unmodifiableList(new ArrayList<>(foundTasks));
    }
    
    /**
     * @brief Изменяет статус выполнения задачи
     * @details Помечает задачу как выполненную или невыполненную по её ID
     * 
     * @param taskId Уникальный идентификатор задачи
     * @param completed Новый статус выполнения (true - выполнена, false - не выполнена)
     * @return true если задача найдена и статус изменен, false если задача не найдена
     * 
     * @note При установке статуса "выполнено" автоматически снимается флаг просроченности
     * @note Использует обычный цикл for для возможности возврата после нахождения задачи
     * 
     * @see Task#setCompleted(boolean)
     * @see Task#getId()
     */
    public boolean markAsCompleted(String taskId, boolean completed) {
        for (Task task : tasks) {
            if (task.getId().equals(taskId)) {
                task.setCompleted(completed);
                return true;
            }
        }
        return false;
    }
    
    /**
     * @brief Проверяет все задачи на просроченность
     * @details Проходит по всем задачам и обновляет их статус просроченности
     * 
     * @note Вызывается автоматически при добавлении задачи и получении списка
     * @note Проверяет только невыполненные задачи
     * @note Использует текущее системное время для проверки
     * 
     * @see LocalDateTime#now()
     * @see Task#isCompleted()
     * @see Task#getEndTime()
     * @see Task#checkOverdue()
     */
    public void checkAllTasksOverdue() {
        LocalDateTime now = LocalDateTime.now();
        for (Task task : tasks) {
            if (!task.isCompleted() && now.isAfter(task.getEndTime())) {
                task.checkOverdue();
            }
        }
    }
    
    /**
     * @brief Подсчитывает количество задач по статусу выполнения
     * @details Возвращает количество задач с указанным статусом выполнения
     * 
     * @param completed Статус выполнения для подсчета (true - выполненные, false - невыполненные)
     * @return int Количество задач с указанным статусом
     * 
     * @note Использует Stream API с фильтрацией и подсчетом
     * @note Более эффективен чем getTasksByCompletion().size()
     * 
     * @see Stream#filter(Predicate)
     * @see Stream#count()
     */
    public int getTaskCount(boolean completed) {
        return (int) tasks.stream()
            .filter(task -> task.isCompleted() == completed)
            .count();
    }
    
    /**
     * @brief Подсчитывает количество просроченных задач
     * @details Возвращает количество невыполненных задач, у которых истек срок выполнения
     * 
     * @return int Количество просроченных невыполненных задач
     * 
     * @note Учитывает только невыполненные задачи (выполненные не могут быть просроченными)
     * @note Использует Stream API для фильтрации и подсчета
     * 
     * @see Task#isOverdue()
     * @see Task#isCompleted()
     */
    public int getOverdueTaskCount() {
        return (int) tasks.stream()
            .filter(task -> task.isOverdue() && !task.isCompleted())
            .count();
    }
    
    /**
     * @brief Очищает все задачи
     * @details Удаляет все задачи из внутреннего списка
     * 
     * @note После очистки список задач становится пустым
     * @warning Операция необратима, задачи не сохраняются автоматически
     * 
     * @see ArrayList#clear()
     * @see util.JsonUtil#saveTasks(List)
     */
    public void clearAllTasks() {
        tasks.clear();
    }
    
    /**
     * @brief Загружает задачи из внешнего списка
     * @details Заменяет текущий список задач новым списком
     * 
     * @param loadedTasks Новый список задач для загрузки
     * 
     * @throws NullPointerException если loadedTasks равен null
     * 
     * @note После загрузки автоматически проверяет все задачи на просроченность
     * @note Очищает предыдущий список задач
     * 
     * @see #clearAllTasks()
     * @see #checkAllTasksOverdue()
     * @see ArrayList#addAll(Collection)
     */
    public void loadTasks(List<Task> loadedTasks) {
        tasks.clear();
        tasks.addAll(loadedTasks);
        checkAllTasksOverdue();
    }
    
    /**
     * @brief Получает изменяемую копию списка задач для внутреннего использования
     * @details Предоставляет изменяемую копию внутреннего списка задач
     * 
     * @return List<Task> Изменяемая копия списка задач
     * 
     * @note Приватный метод для внутреннего использования
     * @warning Не использовать извне класса для сохранения инкапсуляции
     * 
     * @see ArrayList#ArrayList(Collection)
     */
    private List<Task> getMutableTasksCopy() {
        return new ArrayList<>(tasks);
    }
}