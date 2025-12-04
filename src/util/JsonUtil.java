package util;

import model.Task;
import model.Priority;
import model.Category;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @class JsonUtil
 * @brief Утилита для работы с JSON файлами задач
 * 
 * @details Класс предоставляет статические методы для загрузки, сохранения
 * и управления задачами в формате JSON. Все задачи хранятся в файле tasks.json
 * в директории data/. Класс использует стандартные Java библиотеки для работы
 * с файлами и парсинга JSON без внешних зависимостей.
 * 
 * @author Разработчик
 * @version 1.0
 * @date 2025-11-30
 * 
 * @see Task
 * @see Priority
 * @see Category
 * @see Files
 * @see Path
 * 
 * @note Использует ручной парсинг JSON без сторонних библиотек
 * @warning Не потокобезопасен - для многопоточного использования требуется синхронизация
 * @bug При повреждении JSON файла может выбросить RuntimeException
 */
public class JsonUtil {
    
    /** 
     * @brief Директория для хранения данных
     * @details Относительный путь к папке, где хранятся файлы задач
     */
    private static final String DATA_DIR = "data";
    
    /** 
     * @brief Имя файла задач
     * @details Основной файл для хранения всех задач в формате JSON
     */
    private static final String TASKS_FILE = "tasks.json";
    
    /** 
     * @brief Полный путь к файлу задач
     * @details Конкатенация DATA_DIR и TASKS_FILE
     */
    private static final String FULL_PATH = DATA_DIR + "/" + TASKS_FILE;
    
    /** 
     * @brief Паттерн для парсинга даты
     * @details Формат даты и времени, используемый в JSON файле
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    
    /**
     * @brief Загружает все задачи из JSON файла
     * @details Читает файл tasks.json, парсит его содержимое и создает
     *          список объектов Task. Автоматически создает директорию data/
     *          если она не существует.
     * 
     * @return List<Task> Список загруженных задач (может быть пустым)
     * 
     * @throws IOException если возникают проблемы с чтением файла
     * @throws IllegalArgumentException если JSON имеет неверный формат
     * @throws RuntimeException если не удается распарсить JSON или создать задачи
     * 
     * @note Если файл не существует, возвращает пустой список
     * @note Логирует количество загруженных задач в консоль
     * 
     * @see #parseTasksFromJson(String)
     * @see Files#readAllLines(Path)
     * @see Files#createDirectories(Path)
     */
    public static List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        
        try {
            // Проверяем существование директории и файла
            Path dataDir = Paths.get(DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                return tasks;
            }
            
            Path tasksFile = Paths.get(FULL_PATH);
            if (!Files.exists(tasksFile)) {
                return tasks;
            }
            
            // Читаем все строки из файла
            List<String> lines = Files.readAllLines(tasksFile);
            if (lines.isEmpty()) {
                return tasks;
            }
            
            // Объединяем строки и парсим JSON
            String jsonContent = String.join("", lines);
            tasks = parseTasksFromJson(jsonContent);
            
            System.out.println("Загружено задач: " + tasks.size());
            
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке задач из файла: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ошибка парсинга JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tasks;
    }
    
    /**
     * @brief Сохраняет все задачи в JSON файл
     * @details Преобразует список задач в JSON строку и записывает ее в файл.
     *          Перезаписывает существующий файл полностью.
     * 
     * @param tasks Список задач для сохранения
     * @return boolean true если сохранение успешно, false при ошибке
     * 
     * @throws IOException если возникают проблемы с записью в файл
     * 
     * @note Автоматически создает директорию data/ если она не существует
     * @note Использует StandardOpenOption.TRUNCATE_EXISTING для перезаписи файла
     * @note Логирует количество сохраненных задач в консоль
     * 
     * @see #convertTasksToJson(List)
     * @see Files#write(Path, byte[], OpenOption...)
     * @see StandardOpenOption#CREATE
     * @see StandardOpenOption#TRUNCATE_EXISTING
     */
    public static boolean saveTasks(List<Task> tasks) {
        try {
            // Проверяем существование директории
            Path dataDir = Paths.get(DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            
            // Преобразуем задачи в JSON строку
            String jsonContent = convertTasksToJson(tasks);
            
            // Записываем в файл
            Path tasksFile = Paths.get(FULL_PATH);
            Files.write(tasksFile, jsonContent.getBytes(), 
                       StandardOpenOption.CREATE, 
                       StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("Сохранено задач: " + tasks.size());
            return true;
            
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении задач в файл: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @brief Добавляет одну задачу в файл
     * @details Загружает существующие задачи, добавляет новую и сохраняет обратно.
     *          Неэффективно для частых добавлений - лучше использовать saveTasks().
     * 
     * @param task Задача для добавления (не может быть null)
     * @return boolean true если добавление успешно, false при ошибке
     * 
     * @throws NullPointerException если task равен null
     * 
     * @note Для одиночных добавлений используйте этот метод
     * @note Для массовых операций лучше использовать saveTasks()
     * 
     * @see #loadTasks()
     * @see #saveTasks(List)
     * @see List#add(Object)
     */
    public static boolean addTask(Task task) {
        List<Task> tasks = loadTasks();
        tasks.add(task);
        return saveTasks(tasks);
    }
    
    /**
     * @brief Удаляет задачу по ID
     * @details Загружает задачи, ищет задачу с указанным ID и удаляет ее,
     *          затем сохраняет обновленный список.
     * 
     * @param taskId Уникальный идентификатор задачи для удаления
     * @return boolean true если задача найдена и удалена, false если не найдена
     * 
     * @throws NullPointerException если taskId равен null
     * 
     * @note ID сравнивается как строка (String.equals())
     * @note Если задача не найдена, возвращает false без изменения файла
     * 
     * @see #loadTasks()
     * @see #saveTasks(List)
     * @see List#removeIf(java.util.function.Predicate)
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
     * @brief Обновляет существующую задачу
     * @details Загружает задачи, находит задачу по ID и заменяет ее новой версией.
     * 
     * @param updatedTask Обновленная версия задачи (должна иметь тот же ID)
     * @return boolean true если задача найдена и обновлена, false если не найдена
     * 
     * @throws NullPointerException если updatedTask равен null
     * 
     * @note Сравнивает ID задачи для поиска
     * @note Полностью заменяет старую задачу новой
     * 
     * @see #loadTasks()
     * @see #saveTasks(List)
     * @see List#set(int, Object)
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
     * @details Создает JSON массив из объектов Task.
     *          Каждая задача преобразуется с помощью Task.toJsonString().
     * 
     * @param tasks Список задач для преобразования (может быть пустым)
     * @return String JSON строка в формате массива объектов
     * 
     * @throws NullPointerException если tasks равен null
     * 
     * @note Возвращает "[]" для пустого списка
     * @note Добавляет отступы и переносы строк для читаемости
     * 
     * @see Task#toJsonString()
     * @see StringBuilder
     */
    private static String convertTasksToJson(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "[]";
        }
        
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            jsonBuilder.append("  ");
            jsonBuilder.append(task.toJsonString());
            
            if (i < tasks.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
    
    /**
     * @brief Парсит JSON строку в список задач
     * @details Разбирает JSON строку, извлекает отдельные объекты
     *          и создает из них объекты Task.
     * 
     * @param jsonContent JSON строка для парсинга
     * @return List<Task> Список распарсенных задач
     * 
     * @throws IllegalArgumentException если JSON имеет неверный формат
     * @throws RuntimeException если не удается распарсить JSON или создать задачи
     * 
     * @note Ожидает JSON в формате массива объектов
     * @note Игнорирует задачи, которые не удалось распарсить (логирует ошибку)
     * 
     * @see #splitJsonObjects(String)
     * @see #parseSingleTask(String)
     */
    private static List<Task> parseTasksFromJson(String jsonContent) {
        List<Task> tasks = new ArrayList<>();
        
        // Убираем пробелы и переносы строк
        String cleanJson = jsonContent.trim();
        
        // Проверяем, что JSON начинается с '[' и заканчивается ']'
        if (!cleanJson.startsWith("[") || !cleanJson.endsWith("]")) {
            throw new IllegalArgumentException("Неверный формат JSON. Ожидается массив.");
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
                e.printStackTrace();
            }
        }
        
        return tasks;
    }
    
    /**
     * @brief Разделяет JSON массив на отдельные объекты
     * @details Анализирует строку JSON массива и извлекает отдельные объекты.
     *          Учитывает вложенные объекты и строки с кавычками.
     * 
     * @param arrayContent Содержимое JSON массива (без внешних скобок)
     * @return List<String> Список JSON строк отдельных объектов
     * 
     * @note Корректно обрабатывает экранированные кавычки внутри строк
     * @note Учитывает вложенные объекты и массивы
     * 
     * @see StringBuilder
     */
    private static List<String> splitJsonObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inQuotes = false;
        
        for (char c : arrayContent.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
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
                objects.add(current.toString());
                current.setLength(0);
                
                // Пропускаем запятую после объекта, если есть
                // Следующий символ должен быть запятой или концом строки
            }
        }
        
        return objects;
    }
    
    /**
     * @brief Парсит один JSON объект задачи
     * @details Разбирает JSON объект задачи и создает объект Task.
     *          Извлекает все поля задачи из JSON.
     * 
     * @param json JSON строка одного объекта задачи (со скобками)
     * @return Task Объект задачи, созданный из JSON
     * 
     * @throws IllegalArgumentException если JSON имеет неверный формат
     * @throws RuntimeException если не удается создать задачу из извлеченных полей
     * 
     * @note Ожидает валидный JSON объект с фигурными скобками
     * @note Корректно обрабатывает строки с экранированными символами
     * 
     * @see Task#Task(String, String, String, String, String, String, String, boolean, boolean, String)
     * @see Map
     * @see StringBuilder
     */
    private static Task parseSingleTask(String json) {
        // Убираем окружающие фигурные скобки
        String content = json.substring(1, json.length() - 1);
        
        // Разбиваем на пары ключ-значение
        Map<String, String> fields = new HashMap<>();
        
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean inKey = true;
        boolean inQuotes = false;
        int braceCount = 0;
        int bracketCount = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
                if (c == '[') bracketCount++;
                if (c == ']') bracketCount--;
            }
            
            if (!inQuotes && c == ':' && braceCount == 0 && bracketCount == 0) {
                inKey = false;
                continue;
            }
            
            if (!inQuotes && c == ',' && braceCount == 0 && bracketCount == 0) {
                // Сохраняем пару ключ-значение
                String key = currentKey.toString().trim().replace("\"", "");
                String value = currentValue.toString().trim();
                
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
        
        // Создаем объект Task
        try {
            return new Task(
                fields.get("id"),
                fields.get("title"),
                fields.get("description"),
                fields.get("startTime"),
                fields.get("endTime"),
                fields.get("priority"),
                fields.get("category"),
                Boolean.parseBoolean(fields.get("completed")),
                Boolean.parseBoolean(fields.get("overdue")),
                fields.get("createdAt")
            );
        } catch (Exception e) {
            System.err.println("Ошибка создания задачи из JSON: " + e.getMessage());
            throw new RuntimeException("Не удалось создать задачу из JSON", e);
        }
    }
    
    /**
     * @brief Экспортирует задачи в указанный файл
     * @details Сохраняет список задач в формате JSON по указанному пути.
     *          Полезно для создания резервных копий или экспорта данных.
     * 
     * @param filePath Полный путь к файлу для экспорта
     * @param tasks Список задач для экспорта
     * @return boolean true если экспорт успешен, false при ошибке
     * 
     * @throws IOException если возникают проблемы с записью файла
     * @throws NullPointerException если filePath или tasks равны null
     * 
     * @note Перезаписывает существующий файл по указанному пути
     * @note Использует тот же формат, что и основной файл задач
     * 
     * @see #convertTasksToJson(List)
     * @see Files#write(Path, byte[], OpenOption...)
     * @see Paths#get(String, String...)
     */
    public static boolean exportTasks(String filePath, List<Task> tasks) {
        try {
            String jsonContent = convertTasksToJson(tasks);
            Path exportPath = Paths.get(filePath);
            Files.write(exportPath, jsonContent.getBytes(), 
                       StandardOpenOption.CREATE, 
                       StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка экспорта: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * @brief Создает резервную копию файла задач
     * @details Копирует текущий файл tasks.json с добавлением временной метки
     *          в имени. Создает backup в той же директории data/.
     * 
     * @return boolean true если резервное копирование успешно, false при ошибке
     * 
     * @throws IOException если возникают проблемы с чтением/записью файлов
     * 
     * @note Имя backup файла: tasks_backup_YYYYMMDD_HHMMSS.json
     * @note Если исходный файл не существует, возвращает false
     * 
     * @see Files#copy(Path, Path, CopyOption...)
     * @see LocalDateTime#format(java.time.format.DateTimeFormatter)
     * @see StandardCopyOption#REPLACE_EXISTING
     */
    public static boolean createBackup() {
        try {
            Path source = Paths.get(FULL_PATH);
            if (!Files.exists(source)) {
                return false;
            }
            
            String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backup = Paths.get(DATA_DIR + "/tasks_backup_" + timestamp + ".json");
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка создания бэкапа: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * @brief Очищает файл задач
     * @details Записывает пустой JSON массив в файл tasks.json.
     *          Эффективно удаляет все задачи из хранилища.
     * 
     * @return boolean true если очистка успешна, false при ошибке
     * 
     * @throws IOException если возникают проблемы с записью файла
     * 
     * @warning Необратимо удаляет все задачи! Используйте с осторожностью
     * @note Создает файл если он не существует
     * 
     * @see Files#write(Path, byte[], OpenOption...)
     */
    public static boolean clearAllTasks() {
        try {
            Path tasksFile = Paths.get(FULL_PATH);
            Files.write(tasksFile, "[]".getBytes(), 
                       StandardOpenOption.CREATE, 
                       StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка очистки файла: " + e.getMessage());
            return false;
        }
    }
}