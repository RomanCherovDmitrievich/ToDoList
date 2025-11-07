package com.todoapp.utils;

import com.todoapp.models.Task;
import com.todoapp.models.TaskManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FileManager {
    private static final String DATA_FILE = "tasks.dat";
    private static final String EXPORT_DIR = "exports";
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void saveTasks(TaskManager taskManager) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(taskManager);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения задач: " + e.getMessage());
        }
    }

    public static TaskManager loadTasks() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return new TaskManager();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            return (TaskManager) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки задач: " + e.getMessage());
            return new TaskManager();
        }
    }

    public static void exportToCSV(List<Task> tasks) {
        try {
            // Создаем директорию для экспорта, если её нет
            Files.createDirectories(Paths.get(EXPORT_DIR));
            
            String fileName = EXPORT_DIR + "/tasks_export_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                // Заголовок CSV
                writer.println("Название,Описание,Приоритет,Категория,Дата выполнения,Статус,Дата создания");
                
                // Данные
                for (Task task : tasks) {
                    writer.printf("\"%s\",\"%s\",%s,%s,%s,%s,%s\n",
                        escapeCsv(task.getTitle()),
                        escapeCsv(task.getDescription()),
                        task.getPriority().getDisplayName(),
                        task.getCategory().getDisplayName(),
                        task.getDueDate() != null ? task.getDueDate().format(CSV_DATE_FORMATTER) : "",
                        task.isCompleted() ? "Выполнено" : "Активно",
                        task.getCreatedAt().format(CSV_DATE_FORMATTER)
                    );
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка экспорта в CSV: " + e.getMessage());
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    public static void createBackup() {
        File source = new File(DATA_FILE);
        if (!source.exists()) return;

        try {
            String backupName = "tasks_backup_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".dat";
            
            Files.copy(source.toPath(), Paths.get(backupName));
        } catch (IOException e) {
            System.err.println("Ошибка создания бэкапа: " + e.getMessage());
        }
    }

    public static boolean restoreFromBackup(String backupFile) {
        try {
            Files.copy(Paths.get(backupFile), Paths.get(DATA_FILE));
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка восстановления из бэкапа: " + e.getMessage());
            return false;
        }
    }

    public static void cleanupOldExports(int keepDays) {
        File exportDir = new File(EXPORT_DIR);
        if (!exportDir.exists()) return;

        File[] files = exportDir.listFiles((dir, name) -> name.startsWith("tasks_export_"));
        if (files == null) return;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        
        for (File file : files) {
            try {
                String fileName = file.getName();
                String dateTimeStr = fileName.replace("tasks_export_", "").replace(".csv", "");
                LocalDateTime fileTime = LocalDateTime.parse(dateTimeStr, 
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                
                if (fileTime.isBefore(cutoff)) {
                    Files.delete(file.toPath());
                }
            } catch (Exception e) {
                System.err.println("Ошибка удаления старого файла: " + file.getName());
            }
        }
    }
}