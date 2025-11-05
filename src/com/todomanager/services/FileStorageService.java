package com.todomanager.services;

import com.todomanager.models.Task;
import com.todomanager.models.TaskPriority;
import com.todomanager.exceptions.TaskException;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FileStorageService {
    private static final String FILE_NAME = "tasks.dat";
    
    public void saveTasks(List<Task> tasks) throws TaskException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(tasks);
        } catch (IOException e) {
            throw new TaskException("Ошибка сохранения задач", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<Task> loadTasks() throws TaskException {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Task>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new TaskException("Ошибка загрузки задач", e);
        }
    }
}