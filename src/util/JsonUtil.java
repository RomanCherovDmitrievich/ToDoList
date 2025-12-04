package util;

import model.Task;
import model.Priority;
import model.Category;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @class JsonUtil
 * @brief Утилита для загрузки и сохранения задач в формате JSON
 * 
 * @details Класс предоставляет статические методы для работы с задачами в формате JSON.
 * Поддерживает гибкую обработку форматов дат, тестирование через изменение путей,
 * и обработку ошибок с резервным копированием.
 * 
 * @author Разработчик
 * @version 2.0
 * @date 2025-12-01
 * 
 * @see Task
 * @see Priority
 * @see Category
 * @see LocalDateTime
 * @see DateTimeFormatter
 * 
 * @note Поддерживает миллисекунды и микросекунды в форматах дат
 * @warning Не потокобезопасен - требует синхронизации для многопоточного использования
 * @bug Исправлены ошибки парсинга JSON из предыдущей версии
 */
public class JsonUtil {
    
    /** 
     * @brief Директория для хранения данных
     * @details Может быть изменена для целей тестирования
     */
    private static String dataDir = "data";
    
    /** 
     * @brief Имя файла задач
     * @details Может быть изменено для целей тестирования
     */
    private static String tasksFile = "tasks.json";
    
    /** 
     * @brief Форматер для даты с поддержкой миллисекунд и микросекунд
     * @details Используется для парсинга дат в различных форматах
     */
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .toFormatter();
    
    /**
     * @brief Устанавливает путь к данным для тестирования
     * @details Позволяет изменить стандартный путь к данным для изоляции тестов
     * 
     * @param directory Директория для хранения файла задач
     * @param filename Имя файла задач
     * 
     * @note Используется только для тестирования
     * @warning Изменяет глобальное состояние класса
     */
    public static void setDataPath(String directory, String filename) {
        dataDir = directory;
        tasksFile = filename;
    }
    
    /**
     * @brief Сбрасывает путь к данным на стандартный
     * @details Восстанавливает стандартные значения путей после тестирования
     * 
     * @note Используется для очистки после тестов
     */
    public static void resetDataPath() {
        dataDir = "data";
        tasksFile = "tasks.json";
    }
    
    /**
     * @brief Возвращает полный путь к файлу задач
     * @details Формирует путь из текущих значений dataDir и tasksFile
     * 
     * @return String Полный путь к файлу задач
     * 
     * @note Внутренний метод, используется другими методами класса
     */
    private static String getFullPath() {
        return dataDir + "/" + tasksFile;
    }
    
    /**
     * @brief Загружает все задачи из JSON файла
     * @details Использует стандартный путь к файлу задач
     * 
     * @return List<Task> Список загруженных задач
     * 
     * @see #loadTasksFromPath(String)
     */
    public static List<Task> loadTasks() {
        return loadTasksFromPath(getFullPath());
    }
    
    /**
     * @brief Загружает задачи из указанного пути
     * @details Читает файл по указанному пути и парсит его содержимое
     * 
     * @param filePath Полный путь к файлу с задачами
     * @return List<Task> Список загруженных задач (может быть пустым)
     * 
     * @throws IOException если возникают проблемы с чтением файла
     * @throws IllegalArgumentException если JSON имеет неверный формат
     * 
     * @note Создает директорию если её нет
     * @note Возвращает пустой список для несуществующего файла
     * @note Обрабатывает пустые файлы корректно
     * 
     * @see #parseTasksFromJson(String)
     */
    public static List<Task> loadTasksFromPath(String filePath) {
        List<Task> tasks = new ArrayList<>();
        
        try {
            Path tasksPath = Paths.get(filePath);
            
            // Проверяем существование файла
            if (!Files.exists(tasksPath)) {
                // Создаем директорию если её нет
                Path parentDir = tasksPath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                // Возвращаем пустой список для несуществующего файла
                return tasks;
            }
            
            // Проверяем, не пустой ли файл
            if (Files.size(tasksPath) == 0) {
                return tasks;
            }
            
            // Читаем весь файл
            String jsonContent = Files.readString(tasksPath).trim();
            
            if (jsonContent.isEmpty()) {
                return tasks;
            }
            
            tasks = parseTasksFromJson(jsonContent);
            
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке задач из файла " + filePath + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка парсинга JSON из файла " + filePath + ": " + e.getMessage());
        }
        
        return tasks;
    }
    
    /**
     * @brief Сохраняет все задачи в JSON файл
     * @details Использует стандартный путь к файлу задач
     * 
     * @param tasks Список задач для сохранения
     * @return boolean true если сохранение успешно, false при ошибке
     * 
     * @see #saveTasksToPath(List, String)
     */
    public static boolean saveTasks(List<Task> tasks) {
        return saveTasksToPath(tasks, getFullPath());
    }
    
    /**
     * @brief Сохраняет задачи в указанный путь
     * @details Преобразует задачи в JSON и записывает в указанный файл
     * 
     * @param tasks Список задач для сохранения
     * @param filePath Полный путь к файлу
     * @return boolean true если сохранение успешно, false при ошибке
     * 
     * @throws IOException если возникают проблемы с записью файла
     * 
     * @note Создает директорию если её нет
     * @note Перезаписывает существующий файл
     * 
     * @see #convertTasksToJson(List)
     */
    public static boolean saveTasksToPath(List<Task> tasks, String filePath) {
        try {
            if (tasks == null) {
                tasks = new ArrayList<>(); // Защита от null
            }
        
            // Преобразуем задачи в JSON строку
            String jsonContent = convertTasksToJson(tasks);
        
            // Создаем директорию если её нет
            Path tasksPath = Paths.get(filePath);
            Path parentDir = tasksPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
        
            // Записываем в файл - используем правильные опции
            Files.writeString(tasksPath, jsonContent, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        
            // Проверяем, что файл создан и не пустой
            if (Files.exists(tasksPath) && Files.size(tasksPath) > 0) {
                return true;
            } else {
                return false;
            }
        
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении задач в файл " + filePath + ": " + e.getMessage());
            e.printStackTrace(); // Добавим трассировку
            return false;
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при сохранении: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @brief Добавляет одну задачу в файл
     * @details Загружает существующие задачи, добавляет новую и сохраняет обратно
     * 
     * @param task Задача для добавления
     * @return boolean true если добавление успешно, false при ошибке
     * 
     * @note Неэффективно для массовых добавлений
     * @warning Загружает и сохраняет весь файл для одной операции
     * 
     * @see #loadTasks()
     * @see #saveTasks(List)
     */
    public static boolean addTask(Task task) {
        List<Task> tasks = loadTasks();
        tasks.add(task);
        return saveTasks(tasks);
    }
    
    /**
     * @brief Удаляет задачу по ID
     * @details Загружает задачи, удаляет задачу с указанным ID и сохраняет обратно
     * 
     * @param taskId Уникальный идентификатор задачи
     * @return boolean true если задача найдена и удалена, false если не найдена
     * 
     * @note ID сравнивается как строка
     * @warning Загружает и сохраняет весь файл для одной операции
     * 
     * @see #loadTasks()
     * @see #saveTasks(List)
     */
    public static boolean removeTask(String taskId) {
        List<Task> tasks = loadTasks();
        boolean removed = tasks.removeIf(task -> task.getId().equals(taskId));
        if (removed) {
            return saveTasks(tasks);
        }
        return false;
    }
    
    /**
     * @brief Обновляет задачу
     * @details Загружает задачи, находит задачу по ID и заменяет её обновленной версией
     * 
     * @param updatedTask Обновленная версия задачи
     * @return boolean true если задача найдена и обновлена, false если не найдена
     * 
     * @note ID должен совпадать с существующей задачей
     * @warning Загружает и сохраняет весь файл для одной операции
     * 
     * @see #loadTasks()
     * @see #saveTasks(List)
     */
    public static boolean updateTask(Task updatedTask) {
        List<Task> tasks = loadTasks();
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.getId().equals(updatedTask.getId())) {
                tasks.set(i, updatedTask);
                return saveTasks(tasks);
            }
        }
        
        return false;
    }
    
    /**
     * @brief Преобразует список задач в JSON строку
     * @details Создает JSON массив из объектов Task
     * 
     * @param tasks Список задач для преобразования
     * @return String JSON строка в формате массива
     * 
     * @throws NullPointerException если tasks равен null
     * 
     * @note Возвращает "[]" для пустого или null списка
     * @note Добавляет отступы для читаемости
     * 
     * @see Task#toJsonString()
     */
    private static String convertTasksToJson(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "[]"; // Всегда возвращаем валидный JSON
        }
    
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("[\n");
        
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                if (task != null) {
                    jsonBuilder.append("  ");
                    jsonBuilder.append(task.toJsonString());
                
                    if (i < tasks.size() - 1) {
                        jsonBuilder.append(",");
                    }
                    jsonBuilder.append("\n");
                }
            }
        
            jsonBuilder.append("]");
            return jsonBuilder.toString();
        } catch (Exception e) {
            System.err.println("Ошибка при конвертации задач в JSON: " + e.getMessage());
            return "[]"; // Возвращаем пустой массив при ошибке
        }
    }
    
    /**
     * @brief Парсит JSON строку в список задач
     * @details Разбирает JSON строку и создает список объектов Task
     * 
     * @param jsonContent JSON строка для парсинга
     * @return List<Task> Список распарсенных задач
     * 
     * @throws IllegalArgumentException если JSON имеет неверный формат
     * 
     * @note Поддерживает как массив объектов, так и одиночный объект
     * @note Игнорирует задачи, которые не удалось распарсить
     * 
     * @see #splitJsonObjects(String)
     * @see #parseSingleTask(String)
     */
    private static List<Task> parseTasksFromJson(String jsonContent) {
        List<Task> tasks = new ArrayList<>();
        
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return tasks;
        }
        
        String cleanJson = jsonContent.trim();
        
        // Проверяем, что JSON начинается с '[' и заканчивается ']'
        if (!cleanJson.startsWith("[") || !cleanJson.endsWith("]")) {
            // Возможно это один объект, а не массив
            if (cleanJson.startsWith("{") && cleanJson.endsWith("}")) {
                cleanJson = "[" + cleanJson + "]";
            } else {
                throw new IllegalArgumentException("Неверный формат JSON. Ожидается массив или объект.");
            }
        }
        
        // Убираем внешние скобки
        String arrayContent = cleanJson.substring(1, cleanJson.length() - 1).trim();
        
        if (arrayContent.isEmpty()) {
            return tasks;
        }
        
        // Разделяем объекты в массиве
        List<String> taskStrings = splitJsonObjects(arrayContent);
        
        for (String taskJson : taskStrings) {
            try {
                Task task = parseSingleTask(taskJson.trim());
                if (task != null) {
                    tasks.add(task);
                }
            } catch (Exception e) {
                System.err.println("Ошибка парсинга задачи: " + e.getMessage());
                // Продолжаем с другими задачами
            }
        }
        
        return tasks;
    }
    
    /**
     * @brief Разделяет JSON массив на отдельные объекты
     * @details Анализирует строку JSON массива и извлекает отдельные объекты
     * 
     * @param arrayContent Содержимое JSON массива (без внешних скобок)
     * @return List<String> Список JSON строк отдельных объектов
     * 
     * @note Корректно обрабатывает экранированные символы
     * @note Учитывает вложенные объекты и кавычки
     */
    private static List<String> splitJsonObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                current.append(c);
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                current.append(c);
                continue;
            }
            
            if (c == '"' && !escapeNext) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
            }
            
            current.append(c);
            
            if (!inQuotes && braceCount == 0 && c == '}') {
                // Нашли полный объект
                objects.add(current.toString());
                current.setLength(0);
                
                // Пропускаем запятую после объекта, если есть
                if (i + 1 < arrayContent.length() && arrayContent.charAt(i + 1) == ',') {
                    i++;
                }
            }
        }
        
        return objects;
    }
    
    /**
     * @brief Парсит один JSON объект задачи
     * @details Разбирает JSON объект задачи и создает объект Task
     * 
     * @param json JSON строка одного объекта задачи
     * @return Task Объект задачи, созданный из JSON
     * 
     * @throws RuntimeException если не удается создать задачу
     * 
     * @note Поддерживает отсутствующие поля (использует значения по умолчанию)
     * @note Автоматически генерирует ID если он отсутствует
     * 
     * @see #parseJsonFields(String)
     * @see #parseDateTime(String)
     */
    private static Task parseSingleTask(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        String content = json.trim();
        
        // Убираем окружающие фигурные скобки
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }
        
        // Разбиваем на пары ключ-значение
        Map<String, String> fields = parseJsonFields(content);
        
        try {
            return new Task(
                fields.getOrDefault("id", UUID.randomUUID().toString()),
                fields.getOrDefault("title", ""),
                fields.getOrDefault("description", ""),
                parseDateTime(fields.get("startTime")),
                parseDateTime(fields.get("endTime")),
                fields.getOrDefault("priority", "IMPORTANT"),
                fields.getOrDefault("category", "OTHER"),
                Boolean.parseBoolean(fields.getOrDefault("completed", "false")),
                Boolean.parseBoolean(fields.getOrDefault("overdue", "false")),
                parseDateTime(fields.get("createdAt"))
            );
        } catch (Exception e) {
            System.err.println("Ошибка создания задачи из JSON: " + e.getMessage());
            throw new RuntimeException("Не удалось создать задачу из JSON", e);
        }
    }
    
    /**
     * @brief Парсит JSON поля из строки
     * @details Извлекает пары ключ-значение из строки JSON объекта
     * 
     * @param jsonContent Строка с содержимым JSON объекта
     * @return Map<String, String> Карта с извлеченными полями
     * 
     * @note Корректно обрабатывает экранированные символы
     * @note Учитывает вложенные объекты и массивы
     */
    private static Map<String, String> parseJsonFields(String jsonContent) {
        Map<String, String> fields = new HashMap<>();
        
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean inKey = true;
        boolean inQuotes = false;
        boolean escapeNext = false;
        int braceCount = 0;
        int bracketCount = 0;
        
        for (int i = 0; i < jsonContent.length(); i++) {
            char c = jsonContent.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                if (inKey) {
                    currentKey.append(c);
                } else {
                    currentValue.append(c);
                }
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                if (inKey) {
                    currentKey.append(c);
                } else {
                    currentValue.append(c);
                }
                continue;
            }
            
            if (c == '"' && !escapeNext) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
                if (c == '[') bracketCount++;
                if (c == ']') bracketCount--;
            }
            
            if (!inQuotes && c == ':' && braceCount == 0 && bracketCount == 0 && inKey) {
                inKey = false;
                continue;
            }
            
            if (!inQuotes && c == ',' && braceCount == 0 && bracketCount == 0 && !inKey) {
                // Сохраняем пару ключ-значение
                String key = currentKey.toString().trim().replace("\"", "");
                String value = currentValue.toString().trim();
                
                // Убираем кавычки из значения, если они есть
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                fields.put(key, value);
                
                // Сбрасываем для следующей пары
                currentKey.setLength(0);
                currentValue.setLength(0);
                inKey = true;
                continue;
            }
            
            if (inKey) {
                currentKey.append(c);
            } else {
                currentValue.append(c);
            }
        }
        
        // Добавляем последнюю пару
        if (currentKey.length() > 0) {
            String key = currentKey.toString().trim().replace("\"", "");
            String value = currentValue.toString().trim();
            
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            
            fields.put(key, value);
        }
        
        return fields;
    }
    
    /**
     * @brief Парсит дату-время с поддержкой различных форматов
     * @details Поддерживает различные форматы дат с миллисекундами и без
     * 
     * @param dateTimeStr Строка с датой и временем
     * @return String Дата в формате ISO_LOCAL_DATE_TIME
     * 
     * @note Пытается парсить в нескольких форматах
     * @note Возвращает текущую дату при ошибке парсинга
     * @note Добавляет время по умолчанию если передана только дата
     */
    private static String parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        try {
            // Пробуем парсить с нашим форматером
            LocalDateTime.parse(dateTimeStr, FORMATTER);
            return dateTimeStr;
        } catch (Exception e1) {
            try {
                // Пробуем стандартный ISO формат
                LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTimeStr;
            } catch (Exception e2) {
                try {
                    // Пробуем без времени
                    LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr + "T00:00:00");
                    return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e3) {
                    System.err.println("Не удалось распарсить дату: " + dateTimeStr);
                    return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            }
        }
    }
    
    /**
     * @brief Экспортирует задачи в файл
     * @details Сохраняет список задач в указанный файл
     * 
     * @param filePath Полный путь к файлу для экспорта
     * @param tasks Список задач для экспорта
     * @return boolean true если экспорт успешен, false при ошибке
     * 
     * @note Псевдоним для saveTasksToPath
     * 
     * @see #saveTasksToPath(List, String)
     */
    public static boolean exportTasks(String filePath, List<Task> tasks) {
        return saveTasksToPath(tasks, filePath);
    }
    
    /**
     * @brief Резервное копирование файла задач
     * @details Создает копию файла задач с временной меткой в имени
     * 
     * @return boolean true если резервное копирование успешно, false при ошибке
     * 
     * @note Имя backup файла: tasks_backup_YYYYMMDD_HHMMSS.json
     * @note Если исходный файл не существует, возвращает false
     */
    public static boolean createBackup() {
        try {
            Path source = Paths.get(getFullPath());
            if (!Files.exists(source)) {
                return false;
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backup = Paths.get(dataDir + "/tasks_backup_" + timestamp + ".json");
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка создания бэкапа: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * @brief Очищает файл задач
     * @details Записывает пустой JSON массив в стандартный файл задач
     * 
     * @return boolean true если очистка успешна, false при ошибке
     * 
     * @warning Необратимо удаляет все задачи
     */
    public static boolean clearAllTasks() {
        return saveTasksToPath(new ArrayList<>(), getFullPath());
    }
    
    /**
     * @brief Очищает файл задач по указанному пути
     * @details Записывает пустой JSON массив в указанный файл
     * 
     * @param filePath Полный путь к файлу для очистки
     * @return boolean true если очистка успешна, false при ошибке
     * 
     * @warning Необратимо удаляет все задачи в указанном файле
     */
    public static boolean clearAllTasks(String filePath) {
        return saveTasksToPath(new ArrayList<>(), filePath);
    }
}