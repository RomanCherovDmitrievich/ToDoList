package com.todoapp.models;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarModel {
    private YearMonth currentMonth;
    private Map<LocalDate, List<Task>> tasksByDate;
    private TaskManager taskManager;

    public CalendarModel(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.currentMonth = YearMonth.now();
        this.tasksByDate = new HashMap<>();
        updateTasksMap();
    }

    public void setCurrentMonth(YearMonth month) {
        this.currentMonth = month;
        updateTasksMap();
    }

    public YearMonth getCurrentMonth() {
        return currentMonth;
    }

    public List<Task> getTasksForDate(LocalDate date) {
        return tasksByDate.getOrDefault(date, List.of());
    }

    public int getTaskCountForDate(LocalDate date) {
        return getTasksForDate(date).size();
    }

    public boolean hasTasksOnDate(LocalDate date) {
        return tasksByDate.containsKey(date) && !tasksByDate.get(date).isEmpty();
    }

    public boolean hasOverdueTasksOnDate(LocalDate date) {
        return getTasksForDate(date).stream().anyMatch(Task::isOverdue);
    }

    public boolean hasHighPriorityTasksOnDate(LocalDate date) {
        return getTasksForDate(date).stream()
                .anyMatch(task -> task.getPriority() == Task.Priority.HIGH && !task.isCompleted());
    }

    private void updateTasksMap() {
        tasksByDate.clear();
        
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        // Добавляем задачи для каждого дня месяца
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Task> dayTasks = taskManager.getTasksForDate(currentDate);
            if (!dayTasks.isEmpty()) {
                tasksByDate.put(currentDate, dayTasks);
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    public void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateTasksMap();
    }

    public void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateTasksMap();
    }

    public void goToToday() {
        currentMonth = YearMonth.now();
        updateTasksMap();
    }
}