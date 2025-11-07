package com.todoapp.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Task> tasks;

    public TaskManager() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void updateTask(Task oldTask, Task newTask) {
        int index = tasks.indexOf(oldTask);
        if (index != -1) {
            tasks.set(index, newTask);
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getTasksForDate(LocalDate date) {
        return tasks.stream()
                .filter(task -> task.getDueDate() != null && 
                               task.getDueDate().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Task::getPriority)
                        .thenComparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksForWeek(LocalDate startOfWeek) {
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return tasks.stream()
                .filter(task -> task.getDueDate() != null && 
                               !task.getDueDate().toLocalDate().isBefore(startOfWeek) &&
                               !task.getDueDate().toLocalDate().isAfter(endOfWeek))
                .sorted(Comparator.comparing(Task::getDueDate)
                        .thenComparing(Task::getPriority))
                .collect(Collectors.toList());
    }

    public List<Task> getOverdueTasks() {
        return tasks.stream()
                .filter(Task::isOverdue)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksDueToday() {
        return tasks.stream()
                .filter(Task::isDueToday)
                .sorted(Comparator.comparing(Task::getPriority)
                        .thenComparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksDueThisWeek() {
        return tasks.stream()
                .filter(Task::isDueThisWeek)
                .sorted(Comparator.comparing(Task::getDueDate)
                        .thenComparing(Task::getPriority))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByPriority(Task.Priority priority) {
        return tasks.stream()
                .filter(task -> task.getPriority() == priority)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByCategory(Task.Category category) {
        return tasks.stream()
                .filter(task -> task.getCategory() == category)
                .sorted(Comparator.comparing(Task::getDueDate)
                        .thenComparing(Task::getPriority))
                .collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        return tasks.stream()
                .filter(Task::isCompleted)
                .sorted(Comparator.comparing(Task::getCompletionDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Task> searchTasks(String query) {
        return tasks.stream()
                .filter(task -> task.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                               task.getDescription().toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparing(Task::getPriority)
                        .thenComparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    public boolean hasTimeConflict(LocalDateTime startTime, LocalDateTime endTime, Task excludeTask) {
        return tasks.stream()
                .filter(task -> task != excludeTask && task.getDueDate() != null && !task.isCompleted())
                .anyMatch(task -> {
                    LocalDateTime taskStart = task.getDueDate();
                    LocalDateTime taskEnd = taskStart.plusHours(1); // Предполагаем длительность 1 час
                    return (startTime.isBefore(taskEnd) && endTime.isAfter(taskStart));
                });
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public int getCompletedCount() {
        return (int) tasks.stream().filter(Task::isCompleted).count();
    }

    public int getOverdueCount() {
        return (int) tasks.stream().filter(Task::isOverdue).count();
    }
}