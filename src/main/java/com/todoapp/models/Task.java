package com.todoapp.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String title;
    private String description;
    private Priority priority;
    private Category category;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private boolean completed;
    private LocalDate completionDate;
    private String id;

    public enum Priority {
        HIGH("Высокий"),
        MEDIUM("Средний"),
        LOW("Низкий");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Category {
        WORK("Работа"),
        HOME("Дом"),
        STUDY("Учеба"),
        PERSONAL("Личное");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Task(String title, String description, Priority priority, Category category, LocalDateTime dueDate) {
        this.id = generateId();
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
        this.completed = false;
    }

    private String generateId() {
        return "task_" + System.currentTimeMillis() + "_" + Math.random() * 1000;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { 
        this.completed = completed;
        if (completed) {
            this.completionDate = LocalDate.now();
        } else {
            this.completionDate = null;
        }
    }
    public LocalDate getCompletionDate() { return completionDate; }

    public boolean isOverdue() {
        return !completed && dueDate != null && dueDate.isBefore(LocalDateTime.now());
    }

    public boolean isDueToday() {
        return !completed && dueDate != null && dueDate.toLocalDate().equals(LocalDate.now());
    }

    public boolean isDueThisWeek() {
        if (completed || dueDate == null) return false;
        LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        LocalDate dueDateLocal = dueDate.toLocalDate();
        return !dueDateLocal.isBefore(startOfWeek) && !dueDateLocal.isAfter(endOfWeek);
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s", title, priority.getDisplayName(), 
                           dueDate != null ? dueDate.toLocalDate().toString() : "Без срока");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}