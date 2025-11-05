package com.todomanager.managers;

import com.todomanager.models.Task;
import com.todomanager.models.TaskPriority;
import com.todomanager.services.FileStorageService;
import com.todomanager.exceptions.TaskException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TaskManager {
    private List<Task> tasks;
    private FileStorageService storageService;
    
    // Различные модификаторы доступа
    private int taskCount; // private - доступ только внутри класса
    protected String version; // protected - для наследников
    public static final String APP_NAME = "TodoManager"; // public static final - константа
    
    public TaskManager() {
        this.tasks = new ArrayList<>();
        this.storageService = new FileStorageService();
        this.version = "1.0";
        loadTasksFromFile();
    }
    
    // Перегруженные методы addTask
    public void addTask(Task task) throws TaskException {
        if (task == null) {
            throw new TaskException("Задача не может быть null");
        }
        
        if (task.getId() == null) {
            task.setId(generateId());
        }
        
        tasks.add(task);
        taskCount++;
        saveTasksToFile();
    }
    
    // Перегруженный метод - добавление с минимальными параметрами
    public void addTask(String title, LocalDate dueDate, TaskPriority priority) throws TaskException {
        Task task = new Task(title, dueDate, priority);
        addTask(task);
    }
    
    public void updateTask(Task updatedTask) throws TaskException {
        if (updatedTask == null || updatedTask.getId() == null) {
            throw new TaskException("Некорректная задача для обновления");
        }
        
        Task existingTask = findTaskById(updatedTask.getId());
        if (existingTask == null) {
            throw new TaskException("Задача не найдена");
        }
        
        // Обновляем поля
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setDueDate(updatedTask.getDueDate());
        existingTask.setDueTime(updatedTask.getDueTime());
        existingTask.setPriority(updatedTask.getPriority());
        existingTask.setCategory(updatedTask.getCategory());
        existingTask.setCompleted(updatedTask.isCompleted());
        
        saveTasksToFile();
    }
    
    public void deleteTask(String taskId) throws TaskException {
        Task task = findTaskById(taskId);
        if (task == null) {
            throw new TaskException("Задача не найдена");
        }
        
        tasks.remove(task);
        taskCount--;
        saveTasksToFile();
    }
    
    public void toggleTaskCompletion(String taskId) throws TaskException {
        Task task = findTaskById(taskId);
        if (task == null) {
            throw new TaskException("Задача не найдена");
        }
        
        task.toggleCompletion();
        saveTasksToFile();
    }
    
    // Работа с массивами/коллекциями
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }
    
    public List<Task> getTasksByDate(LocalDate date) {
        return tasks.stream()
                   .filter(task -> task.getDueDate().equals(date))
                   .collect(Collectors.toList());
    }
    
    public List<Task> getTasksByPriority(TaskPriority priority) {
        return tasks.stream()
                   .filter(task -> task.getPriority() == priority)
                   .collect(Collectors.toList());
    }
    
    public List<Task> getCompletedTasks() {
        return tasks.stream()
                   .filter(Task::isCompleted)
                   .collect(Collectors.toList());
    }
    
    public List<Task> getPendingTasks() {
        return tasks.stream()
                   .filter(task -> !task.isCompleted())
                   .collect(Collectors.toList());
    }
    
    public List<Task> getOverdueTasks() {
        return tasks.stream()
                   .filter(task -> !task.isCompleted() && task.isOverdue())
                   .collect(Collectors.toList());
    }
    
    // Использование циклов
    public Map<LocalDate, List<Task>> getTasksGroupedByDate() {
        Map<LocalDate, List<Task>> groupedTasks = new TreeMap<>();
        
        for (Task task : tasks) {
            LocalDate date = task.getDueDate();
            groupedTasks.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        }
        
        return groupedTasks;
    }
    
    // Проверка условий
    public boolean hasTasksForDate(LocalDate date) {
        for (Task task : tasks) {
            if (task.getDueDate().equals(date)) {
                return true;
            }
        }
        return false;
    }
    
    public Task findTaskById(String id) {
        for (Task task : tasks) {
            if (task.getId().equals(id)) {
                return task;
            }
        }
        return null;
    }
    
    // Статистика
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", tasks.size());
        stats.put("completed", getCompletedTasks().size());
        stats.put("pending", getPendingTasks().size());
        stats.put("overdue", getOverdueTasks().size());
        
        // Статистика по приоритетам
        for (TaskPriority priority : TaskPriority.values()) {
            stats.put(priority.name().toLowerCase(), getTasksByPriority(priority).size());
        }
        
        return stats;
    }
    
    private String generateId() {
        return UUID.randomUUID().toString();
    }
    
    private void loadTasksFromFile() {
        try {
            List<Task> loadedTasks = storageService.loadTasks();
            tasks.clear();
            tasks.addAll(loadedTasks);
            taskCount = tasks.size();
        } catch (TaskException e) {
            System.err.println("Ошибка загрузки: " + e.getMessage());
            tasks = new ArrayList<>();
        }
    }
    
    private void saveTasksToFile() throws TaskException {
        storageService.saveTasks(tasks);
    }
    
    // Геттеры с различными модификаторами доступа
    public int getTaskCount() {
        return taskCount;
    }
    
    protected String getVersion() {
        return version;
    }
}