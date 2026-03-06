package util;

import model.Task;
import repository.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * @class DatabaseMigrator
 * @brief Утилита для миграции данных между JSON и базой данных
 * 
 * @details Обеспечивает миграцию задач из JSON файла в SQLite базу данных
 * и обратно. Позволяет создавать резервные копии данных.
 * 
 * @author Чернов
 * @version 1.0
 * @date 2025-11-4
 * 
 * @see DatabaseManager
 * @see JsonUtil
 */
public class DatabaseMigrator {
    
    /**
     * @brief Путь к JSON файлу с задачами
     */
    private static final String JSON_FILE_PATH = "data/tasks.json";
    
    /**
     * @brief Расширение для резервных копий JSON
     */
    private static final String BACKUP_EXTENSION = ".backup";
    
    /**
     * @brief Мигрирует задачи из JSON в базу данных
     * @details Загружает задачи из JSON файла и сохраняет их в БД
     * 
     * @return true если миграция успешна, false если произошла ошибка
     * 
     * @note Создает резервную копию JSON файла перед миграцией
     */
    public static boolean migrateJsonToDatabase() {
        try {
            File jsonFile = new File(JSON_FILE_PATH);
            if (!jsonFile.exists()) {
                System.out.println("JSON файл не найден: " + JSON_FILE_PATH);
                return false;
            }
            
            System.out.println("Начинаем миграцию данных из JSON в базу данных...");
            
            // Создаем резервную копию
            createBackup();
            
            // Читаем задачи из JSON
            Gson gson = new Gson();
            Type taskListType = new TypeToken<List<Task>>(){}.getType();
            
            List<Task> tasks;
            try (FileReader reader = new FileReader(jsonFile)) {
                tasks = gson.fromJson(reader, taskListType);
            }
            
            if (tasks == null || tasks.isEmpty()) {
                System.out.println("В JSON файле нет задач для миграции");
                return true;
            }
            
            System.out.println("Загружено " + tasks.size() + " задач из JSON");
            
            // Сохраняем задачи в БД
            DatabaseManager dbManager = DatabaseManager.getInstance();
            for (Task task : tasks) {
                dbManager.saveTask(task);
            }
            
            System.out.println("Миграция успешно завершена!");
            System.out.println("Мигрировано задач: " + tasks.size());
            System.out.println("Резервная копия создана: " + JSON_FILE_PATH + BACKUP_EXTENSION);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Ошибка чтения JSON файла: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка миграции данных: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @brief Экспортирует задачи из базы данных в JSON
     * @details Загружает задачи из БД и сохраняет их в JSON файл
     * 
     * @return true если экспорт успешен, false если произошла ошибка
     */
    public static boolean exportDatabaseToJson() {
        try {
            System.out.println("Начинаем экспорт данных из базы данных в JSON...");
            
            // Загружаем задачи из БД
            DatabaseManager dbManager = DatabaseManager.getInstance();
            List<Task> tasks = dbManager.getAllTasks();
            
            if (tasks.isEmpty()) {
                System.out.println("В базе данных нет задач для экспорта");
                return true;
            }
            
            System.out.println("Загружено " + tasks.size() + " задач из базы данных");
            
            // Сохраняем в JSON
            JsonUtil.saveTasks(tasks);
            
            System.out.println("Экспорт успешно завершен!");
            System.out.println("Экспортировано задач: " + tasks.size());
            
            return true;
            
        }catch (Exception e) {
            System.err.println("Ошибка экспорта данных: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @brief Создает резервную копию JSON файла
     * @details Копирует текущий JSON файл с добавлением расширения .backup
     * 
     * @throws IOException если не удается создать резервную копию
     */
    private static void createBackup() throws IOException {
        Path source = Paths.get(JSON_FILE_PATH);
        Path backup = Paths.get(JSON_FILE_PATH + BACKUP_EXTENSION);
        
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Резервная копия создана: " + backup);
    }
    
    /**
     * @brief Восстанавливает данные из резервной копии
     * @details Восстанавливает JSON файл из резервной копии
     * 
     * @return true если восстановление успешно, false если произошла ошибка
     */
    public static boolean restoreFromBackup() {
        try {
            Path backup = Paths.get(JSON_FILE_PATH + BACKUP_EXTENSION);
            Path original = Paths.get(JSON_FILE_PATH);
            
            if (!Files.exists(backup)) {
                System.out.println("Резервная копия не найдена: " + backup);
                return false;
            }
            
            Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Данные восстановлены из резервной копии: " + backup);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Ошибка восстановления из резервной копии: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * @brief Синхронизирует данные между JSON и БД
     * @details Сравнивает данные в обоих источниках и синхронизирует их
     * 
     * @return Строка с результатами синхронизации
     */
    public static String synchronizeData() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("Синхронизация данных:\n");
            
            // Загружаем из JSON
            List<Task> jsonTasks = JsonUtil.loadTasks();
            result.append("Задач в JSON: ").append(jsonTasks.size()).append("\n");
            
            // Загружаем из БД
            DatabaseManager dbManager = DatabaseManager.getInstance();
            List<Task> dbTasks = dbManager.getAllTasks();
            result.append("Задач в БД: ").append(dbTasks.size()).append("\n");
            
            // Находим различия
            int addedToDB = 0;
            int addedToJSON = 0;
            
            // Добавляем в БД задачи, которых там нет
            for (Task jsonTask : jsonTasks) {
                boolean existsInDB = dbTasks.stream()
                    .anyMatch(dbTask -> dbTask.getId().equals(jsonTask.getId()));
                if (!existsInDB) {
                    dbManager.saveTask(jsonTask);
                    addedToDB++;
                }
            }
            
            // Добавляем в JSON задачи, которых там нет
            for (Task dbTask : dbTasks) {
                boolean existsInJSON = jsonTasks.stream()
                    .anyMatch(jsonTask -> jsonTask.getId().equals(dbTask.getId()));
                if (!existsInJSON) {
                    jsonTasks.add(dbTask);
                    addedToJSON++;
                }
            }
            
            // Сохраняем обновленный JSON
            if (addedToJSON > 0) {
                JsonUtil.saveTasks(jsonTasks);
            }
            
            result.append("Добавлено в БД: ").append(addedToDB).append("\n");
            result.append("Добавлено в JSON: ").append(addedToJSON).append("\n");
            result.append("Синхронизация завершена!");
            
            return result.toString();
            
        } catch (Exception e) {
            return "Ошибка синхронизации: " + e.getMessage();
        }
    }
    
    /**
     * @brief Точка входа для запуска миграции из командной строки
     * @details Позволяет запустить миграцию вне приложения
     * 
     * @param args Аргументы командной строки
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Использование:");
            System.out.println("  java DatabaseMigrator migrate   - Мигрировать из JSON в БД");
            System.out.println("  java DatabaseMigrator export    - Экспортировать из БД в JSON");
            System.out.println("  java DatabaseMigrator sync      - Синхронизировать данные");
            System.out.println("  java DatabaseMigrator restore   - Восстановить из резервной копии");
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "migrate":
                if (migrateJsonToDatabase()) {
                    System.out.println("Миграция успешно завершена!");
                } else {
                    System.out.println("Миграция не удалась!");
                }
                break;
                
            case "export":
                if (exportDatabaseToJson()) {
                    System.out.println("Экспорт успешно завершен!");
                } else {
                    System.out.println("Экспорт не удался!");
                }
                break;
                
            case "sync":
                System.out.println(synchronizeData());
                break;
                
            case "restore":
                if (restoreFromBackup()) {
                    System.out.println("Восстановление успешно завершено!");
                } else {
                    System.out.println("Восстановление не удалось!");
                }
                break;
                
            default:
                System.out.println("Неизвестная команда: " + command);
                break;
        }
    }
}