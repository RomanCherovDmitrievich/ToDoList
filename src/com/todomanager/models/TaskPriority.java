package com.todomanager.models;

public enum TaskPriority {
    LOW("Низкий", "#27ae60"),
    MEDIUM("Средний", "#f39c12"), 
    HIGH("Высокий", "#e74c3c");
    
    private final String displayName;
    private final String color;
    
    TaskPriority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColor() {
        return color;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}