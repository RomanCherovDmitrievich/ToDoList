package com.todomanager.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Task {
    private String id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private TaskPriority priority;
    private String category;
    private boolean completed;
    private LocalDateTime createdAt;
    
    // Конструкторы
    public Task() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Task(String title, String description, LocalDate dueDate, LocalTime dueTime, 
                TaskPriority priority, String category) {
        this();
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.priority = priority;
        this.category = category;
        this.completed = false;
    }
    
    // Перегруженный конструктор (перегрузка методов)
    public Task(String title, LocalDate dueDate, TaskPriority priority) {
        this(title, "", dueDate, null, priority, "Общие");
    }
    
    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { 
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Название задачи не может быть пустым");
        }
        this.title = title.trim();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { 
        if (dueDate == null) {
            throw new IllegalArgumentException("Дата выполнения не может быть пустой");
        }
        this.dueDate = dueDate; 
    }
    
    public LocalTime getDueTime() { return dueTime; }
    public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { 
        if (priority == null) {
            throw new IllegalArgumentException("Приоритет не может быть пустым");
        }
        this.priority = priority; 
    }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Методы бизнес-логики
    public boolean isOverdue() {
        if (completed) return false;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime taskDateTime = dueTime != null ? 
            LocalDateTime.of(dueDate, dueTime) : 
            dueDate.atTime(23, 59, 59);
            
        return taskDateTime.isBefore(now);
    }
    
    public void toggleCompletion() {
        this.completed = !this.completed;
    }
    
    // Перегруженный метод toString
    @Override
    public String toString() {
        return String.format("Task{id='%s', title='%s', dueDate=%s, priority=%s, completed=%s}", 
                           id, title, dueDate, priority, completed);
    }
    
    // Перегруженный метод equals
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id != null && id.equals(task.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}