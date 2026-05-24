package repository;

import model.Task;

import java.util.List;

/**
 * Единый DAO-интерфейс для работы с задачами независимо от СУБД.
 */
public interface TaskDao {
    void saveTask(Task task);
    List<Task> getAllTasks();
    boolean deleteTask(String taskId);
    boolean updateTask(Task task);
    Task getTaskById(String taskId);
    List<Task> searchTasks(String query);
    List<Task> getTasksByCategory(String category);
}
