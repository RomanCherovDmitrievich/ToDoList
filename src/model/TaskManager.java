package model;

import repository.DatabaseManager;
import util.JsonUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import util.RecurrenceUtil;

/**
 * Центральный менеджер задач (Singleton).
 */
public class TaskManager {
    private static TaskManager instance;

    private final List<Task> tasks = new ArrayList<>();
    private DatabaseManager dbManager;
    private boolean useDatabase = true;

    private LocalDateTime lastOverdueCheck = LocalDateTime.MIN;
    private static final Duration OVERDUE_CHECK_INTERVAL = Duration.ofSeconds(30);

    private TaskManager() {
        initializeStorage();
    }

    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    private void initializeStorage() {
        List<Task> jsonTasks = JsonUtil.loadTasks();

        if (useDatabase) {
            try {
                dbManager = DatabaseManager.getInstance();
                List<Task> dbTasks = dbManager.getAllTasks();
                mergeTasks(jsonTasks, dbTasks);
            } catch (Exception e) {
                System.err.println("TaskManager: DB unavailable, fallback to JSON only: " + e.getMessage());
                useDatabase = false;
                tasks.clear();
                tasks.addAll(jsonTasks);
            }
        } else {
            tasks.clear();
            tasks.addAll(jsonTasks);
        }

        checkAllTasksOverdue();
        normalizeSortIndexes();
    }

    private void mergeTasks(List<Task> jsonTasks, List<Task> dbTasks) {
        tasks.clear();

        if (dbTasks.isEmpty() && !jsonTasks.isEmpty()) {
            tasks.addAll(jsonTasks);
            if (dbManager != null) {
                for (Task task : tasks) {
                    dbManager.saveTask(task);
                }
            }
            return;
        }

        tasks.addAll(dbTasks);

        if (!jsonTasks.isEmpty()) {
            Set<String> existingIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
            for (Task jsonTask : jsonTasks) {
                if (!existingIds.contains(jsonTask.getId())) {
                    tasks.add(jsonTask);
                    if (dbManager != null) {
                        dbManager.saveTask(jsonTask);
                    }
                }
            }
        }
    }

    public synchronized void addTask(Task task) {
        if (task == null) {
            return;
        }
        if (task.getSortIndex() <= 0) {
            task.setSortIndex(nextSortIndex());
        }
        tasks.add(task);
        persistTask(task);
        saveJsonSnapshot();
        checkAllTasksOverdue();
    }

    public synchronized boolean removeTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return false;
        }

        boolean removed = tasks.removeIf(task -> task.getId().equals(taskId));
        if (!removed) {
            return false;
        }

        if (useDatabase && dbManager != null) {
            dbManager.deleteTask(taskId);
        }
        saveJsonSnapshot();
        return true;
    }

    public synchronized List<Task> getAllTasks() {
        maybeCheckOverdue();
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    public synchronized List<Task> getTasksByCompletion(boolean completed) {
        return Collections.unmodifiableList(
            tasks.stream()
                .filter(task -> task.isCompleted() == completed)
                .collect(Collectors.toList())
        );
    }

    public synchronized List<Task> searchTasks(String query) {
        if (query == null || query.isBlank()) {
            return getAllTasks();
        }

        String trimmed = query.trim();
        boolean hasNonAscii = trimmed.chars().anyMatch(ch -> ch > 127);

        if (!hasNonAscii && useDatabase && dbManager != null) {
            List<Task> dbResult = new ArrayList<>(dbManager.searchTasks(trimmed));
            if (!dbResult.isEmpty()) {
                return Collections.unmodifiableList(dbResult);
            }
        }

        String lower = trimmed.toLowerCase();
        return Collections.unmodifiableList(
            tasks.stream()
                .filter(task -> task.getTitle().toLowerCase().contains(lower)
                    || task.getDescription().toLowerCase().contains(lower))
                .collect(Collectors.toList())
        );
    }

    public synchronized boolean markAsCompleted(String taskId, boolean completed) {
        Task task = getTaskById(taskId);
        if (task == null) {
            return false;
        }

        task.setCompleted(completed);
        task.checkOverdue();
        persistTask(task);
        saveJsonSnapshot();

        if (completed) {
            createNextRecurringTask(task);
        }
        return true;
    }

    public synchronized boolean updateTask(Task updatedTask) {
        if (updatedTask == null) {
            return false;
        }

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(updatedTask.getId())) {
                updatedTask.checkOverdue();
                tasks.set(i, updatedTask);
                persistTask(updatedTask);
                saveJsonSnapshot();
                return true;
            }
        }

        return false;
    }

    public synchronized void checkAllTasksOverdue() {
        boolean changed = false;
        for (Task task : tasks) {
            boolean old = task.isOverdue();
            task.checkOverdue();
            if (old != task.isOverdue()) {
                changed = true;
                if (useDatabase && dbManager != null) {
                    dbManager.updateTask(task);
                }
            }
        }

        lastOverdueCheck = LocalDateTime.now();

        if (changed) {
            saveJsonSnapshot();
        }
    }

    private void maybeCheckOverdue() {
        if (Duration.between(lastOverdueCheck, LocalDateTime.now()).compareTo(OVERDUE_CHECK_INTERVAL) >= 0) {
            checkAllTasksOverdue();
        }
    }

    public synchronized int getTaskCount(boolean completed) {
        return (int) tasks.stream().filter(task -> task.isCompleted() == completed).count();
    }

    public synchronized int getOverdueTaskCount() {
        maybeCheckOverdue();
        return (int) tasks.stream().filter(task -> task.isOverdue() && !task.isCompleted()).count();
    }

    public synchronized Task getTaskById(String taskId) {
        if (taskId == null) {
            return null;
        }
        return tasks.stream()
            .filter(task -> task.getId().equals(taskId))
            .findFirst()
            .orElse(null);
    }

    public synchronized void clearAllTasks() {
        if (useDatabase && dbManager != null) {
            // Удаляем из БД поштучно, чтобы сохранить целостность статистики/связей.
            List<String> ids = tasks.stream().map(Task::getId).collect(Collectors.toList());
            for (String id : ids) {
                dbManager.deleteTask(id);
            }
        }

        tasks.clear();
        saveJsonSnapshot();
    }

    public synchronized void loadTasks(List<Task> loadedTasks) {
        tasks.clear();
        if (loadedTasks != null) {
            tasks.addAll(loadedTasks);
        }

        if (useDatabase && dbManager != null) {
            List<Task> existing = dbManager.getAllTasks();
            Set<String> newIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
            for (Task old : existing) {
                if (!newIds.contains(old.getId())) {
                    dbManager.deleteTask(old.getId());
                }
            }
            for (Task task : tasks) {
                dbManager.saveTask(task);
            }
        }

        saveJsonSnapshot();
        checkAllTasksOverdue();
        normalizeSortIndexes();
    }

    public synchronized boolean addTagToTask(String taskId, String tagName) {
        if (!useDatabase || dbManager == null) {
            return false;
        }
        try {
            dbManager.addTagToTask(taskId, tagName);
            return true;
        } catch (Exception e) {
            System.err.println("addTagToTask error: " + e.getMessage());
            return false;
        }
    }

    public synchronized List<String> getTaskTags(String taskId) {
        if (!useDatabase || dbManager == null) {
            return new ArrayList<>();
        }
        return dbManager.getTaskTags(taskId);
    }

    public synchronized String getStatistics() {
        int total = tasks.size();
        int completed = getTaskCount(true);
        int pending = getTaskCount(false);
        int overdue = getOverdueTaskCount();

        return String.format(
            "Всего задач: %d\nВыполнено: %d\nОжидает выполнения: %d\nПросрочено: %d",
            total,
            completed,
            pending,
            overdue
        );
    }

    public synchronized boolean isUseDatabase() {
        return useDatabase;
    }

    public synchronized void setUseDatabase(boolean useDatabase) {
        this.useDatabase = useDatabase;
        if (useDatabase && dbManager == null) {
            initializeStorage();
        }
    }

    public synchronized void close() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    public synchronized List<Task> getFocusTasks(int limit) {
        maybeCheckOverdue();
        Comparator<Task> comparator = Comparator
            .comparing(Task::isCompleted)
            .thenComparing((Task t) -> priorityRank(t.getPriority()))
            .thenComparing(Task::getEndTime);

        return tasks.stream()
            .filter(task -> !task.isCompleted())
            .sorted(comparator)
            .limit(Math.max(1, limit))
            .collect(Collectors.toList());
    }

    public synchronized List<Task> getUpcomingTasks(int minutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime edge = now.plusMinutes(Math.max(1, minutes));

        return tasks.stream()
            .filter(task -> !task.isCompleted())
            .filter(task -> !task.getStartTime().isBefore(now) && !task.getStartTime().isAfter(edge))
            .sorted(Comparator.comparing(Task::getStartTime))
            .collect(Collectors.toList());
    }

    private int priorityRank(Priority priority) {
        return switch (priority) {
            case URGENT -> 0;
            case IMPORTANT -> 1;
            case NORMAL -> 2;
        };
    }

    private void persistTask(Task task) {
        if (useDatabase && dbManager != null) {
            dbManager.saveTask(task);
        }
    }

    private void saveJsonSnapshot() {
        try {
            JsonUtil.saveTasks(tasks);
        } catch (Exception e) {
            System.err.println("JSON save error: " + e.getMessage());
        }
    }

    public synchronized void updateSortOrder(List<Task> orderedTasks) {
        if (orderedTasks == null || orderedTasks.isEmpty()) {
            return;
        }
        tasks.clear();
        tasks.addAll(orderedTasks);
        int index = 1;
        for (Task task : tasks) {
            task.setSortIndex(index++);
            persistTask(task);
        }
        saveJsonSnapshot();
    }

    private int nextSortIndex() {
        return tasks.stream().mapToInt(Task::getSortIndex).max().orElse(0) + 1;
    }

    private void normalizeSortIndexes() {
        boolean allZero = tasks.stream().allMatch(task -> task.getSortIndex() == 0);
        if (!allZero) {
            return;
        }
        int index = 1;
        for (Task task : tasks) {
            task.setSortIndex(index++);
        }
        saveJsonSnapshot();
    }

    private void createNextRecurringTask(Task task) {
        String rule = task.getRecurrenceRule();
        if (rule == null || rule.isBlank() || rule.equalsIgnoreCase("NONE")) {
            return;
        }

        LocalDateTime nextStart = RecurrenceUtil.nextOccurrence(task.getStartTime(), rule);
        if (nextStart == null) {
            return;
        }

        if (task.getRecurrenceEnd() != null && nextStart.isAfter(task.getRecurrenceEnd())) {
            return;
        }

        long durationMinutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
        if (durationMinutes < 0) {
            durationMinutes = 60;
        }

        LocalDateTime nextEnd = nextStart.plusMinutes(durationMinutes);

        Task next = new Task(
            task.getTitle(),
            task.getDescription(),
            nextStart,
            nextEnd,
            task.getPriority(),
            task.getCategory()
        );
        next.setRecurrenceRule(rule);
        next.setReminderOffsetMinutes(task.getReminderOffsetMinutes());
        next.setRecurrenceEnd(task.getRecurrenceEnd());
        next.setSortIndex(nextSortIndex());
        addTask(next);
    }
}
