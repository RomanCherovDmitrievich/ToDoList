package util;

import model.Task;
import model.TaskManager;
import util.NotificationSettings;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Периодически проверяет задачи и отправляет системные напоминания.
 */
public class TaskReminderService {
    private static final int CHECK_INTERVAL_SECONDS = 60;

    private static TaskReminderService instance;

    private ScheduledExecutorService scheduler;
    private final Set<String> upcomingNotified = ConcurrentHashMap.newKeySet();
    private final Set<String> overdueNotified = ConcurrentHashMap.newKeySet();

    private final NotificationService notificationService = NotificationService.getInstance();
    private final EmailNotifier emailNotifier = new EmailNotifier();

    private TaskReminderService() {
    }

    public static synchronized TaskReminderService getInstance() {
        if (instance == null) {
            instance = new TaskReminderService();
        }
        return instance;
    }

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "task-reminder-thread");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(
            this::safeCheck,
            5,
            CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    private void safeCheck() {
        try {
            checkTasks();
        } catch (Exception e) {
            System.err.println("Reminder check error: " + e.getMessage());
        }
    }

    private void checkTasks() {
        List<Task> tasks = TaskManager.getInstance().getAllTasks();
        NotificationSettings settings = NotificationSettings.load();
        LocalDateTime now = LocalDateTime.now();

        Set<String> activeIds = new HashSet<>();

        for (Task task : tasks) {
            String id = task.getId();
            activeIds.add(id);

            if (task.isCompleted()) {
                upcomingNotified.remove(id);
                overdueNotified.remove(id);
                continue;
            }

            int offset = task.getReminderOffsetMinutes() > 0 ? task.getReminderOffsetMinutes() : settings.getReminderOffsetMinutes();
            LocalDateTime start = task.getStartTime();
            LocalDateTime upcomingEdge = now.plusMinutes(Math.max(1, offset));

            if (!start.isBefore(now) && !start.isAfter(upcomingEdge)) {
                long minutes = Math.max(0, Duration.between(now, start).toMinutes());
                if (upcomingNotified.add(id)) {
                    notifyAllChannels(settings, "Скоро начнется задача",
                        task.getTitle() + " (через " + minutes + " мин.)");
                }
            }

            LocalDateTime end = task.getEndTime();
            if (now.isAfter(end)) {
                if (overdueNotified.add(id)) {
                    notifyAllChannels(settings, "Задача просрочена",
                        task.getTitle() + " — дедлайн был " + task.getFormattedEndTime());
                }
            } else {
                overdueNotified.remove(id);
            }
        }

        upcomingNotified.retainAll(activeIds);
        overdueNotified.retainAll(activeIds);
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        upcomingNotified.clear();
        overdueNotified.clear();
    }

    private void notifyAllChannels(NotificationSettings settings, String title, String message) {
        if (settings.isPopupEnabled()) {
            notificationService.showNotification(title, message);
        }
        if (settings.isSoundEnabled()) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
        if (settings.isEmailEnabled()) {
            String emailTo = settings.getEmailTo();
            emailNotifier.sendEmail(emailTo, title, message);
        }
    }
}
