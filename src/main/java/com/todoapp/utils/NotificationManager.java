package com.todoapp.utils;

import com.todoapp.models.Task;
import com.todoapp.models.TaskManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class NotificationManager {
    private TaskManager taskManager;
    private LocalDateTime lastNotificationCheck;

    public NotificationManager(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.lastNotificationCheck = LocalDateTime.now();
    }

    public void checkNotifications() {
        LocalDateTime now = LocalDateTime.now();
        
        // Проверяем просроченные задачи
        checkOverdueTasks();
        
        // Проверяем задачи на сегодня
        checkTodayTasks();
        
        // Проверяем приближающиеся дедлайны
        checkUpcomingDeadlines();
        
        lastNotificationCheck = now;
    }

    private void checkOverdueTasks() {
        List<Task> overdueTasks = taskManager.getOverdueTasks().stream()
                .filter(task -> task.getDueDate().isAfter(lastNotificationCheck))
                .toList();

        for (Task task : overdueTasks) {
            showNotification("Просроченная задача", 
                "Задача '" + task.getTitle() + "' просрочена!");
        }
    }

    private void checkTodayTasks() {
        List<Task> todayTasks = taskManager.getTasksDueToday().stream()
                .filter(task -> !task.isCompleted())
                .toList();

        if (!todayTasks.isEmpty()) {
            StringBuilder message = new StringBuilder("Задачи на сегодня:\n");
            for (Task task : todayTasks) {
                message.append("• ").append(task.getTitle()).append("\n");
            }
            showNotification("Задачи на сегодня", message.toString());
        }
    }

    private void checkUpcomingDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourFromNow = now.plusHours(1);
        
        List<Task> upcomingTasks = taskManager.getAllTasks().stream()
                .filter(task -> !task.isCompleted() && task.getDueDate() != null)
                .filter(task -> task.getDueDate().isAfter(now) && 
                               task.getDueDate().isBefore(oneHourFromNow))
                .filter(task -> task.getDueDate().isAfter(lastNotificationCheck))
                .toList();

        for (Task task : upcomingTasks) {
            long minutesLeft = ChronoUnit.MINUTES.between(now, task.getDueDate());
            showNotification("Приближается дедлайн", 
                "Задача '" + task.getTitle() + "' через " + minutesLeft + " минут!");
        }
    }

    private void showNotification(String title, String message) {
        // В реальном приложении здесь можно использовать системные уведомления
        // или более красивый кастомный popup
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Автоматическое закрытие через 5 секунд
        Thread autoCloseThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (alert.isShowing()) {
                    javafx.application.Platform.runLater(alert::close);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        autoCloseThread.setDaemon(true);
        autoCloseThread.start();
        
        alert.show();
    }

    public boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public void showReminder(Task task) {
        String message = String.format(
            "Напоминание о задаче:\n\n" +
            "Название: %s\n" +
            "Категория: %s\n" +
            "Приоритет: %s\n" +
            "Время выполнения: %s",
            task.getTitle(),
            task.getCategory().getDisplayName(),
            task.getPriority().getDisplayName(),
            DateUtils.formatDateTime(task.getDueDate())
        );
        
        showNotification("Напоминание", message);
    }
}