package repository;

import model.Task;
import model.Category;
import model.Priority;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SQLite DAO для задач и связанных таблиц.
 */
public class DatabaseManager {
    private static final Path DB_FILE = util.PathResolver.getDatabaseFile();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            Files.createDirectories(util.PathResolver.getDataDir());
            Class.forName("org.sqlite.JDBC");
            String dbPath = DB_FILE.toAbsolutePath().toString();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            enablePragmas();
            createTables();
            migrateSchema();
            seedDefaults();
            System.out.println("SQLite database ready: " + dbPath);
        } catch (Exception e) {
            System.err.println("Database init error: " + e.getMessage());
        }
    }

    private void enablePragmas() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }

    private void createTables() throws SQLException {
        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                priority TEXT NOT NULL CHECK(priority IN ('URGENT','IMPORTANT','NORMAL')),
                category TEXT NOT NULL CHECK(category IN ('WORK','HOME','STUDY','OTHER')),
                completed INTEGER NOT NULL DEFAULT 0,
                overdue INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                sort_index INTEGER NOT NULL DEFAULT 0,
                recurrence_rule TEXT,
                reminder_offset INTEGER NOT NULL DEFAULT 0,
                recurrence_end TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                color TEXT NOT NULL DEFAULT '#95a5a6'
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS task_tags (
                task_id TEXT NOT NULL,
                tag_id INTEGER NOT NULL,
                assigned_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (task_id, tag_id),
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS task_statistics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id TEXT NOT NULL,
                operation_type TEXT NOT NULL,
                operation_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                details TEXT,
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        };

        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed)",
            "CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority)",
            "CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category)",
            "CREATE INDEX IF NOT EXISTS idx_tasks_end_time ON tasks(end_time)",
            "CREATE INDEX IF NOT EXISTS idx_task_tags_task_id ON task_tags(task_id)",
            "CREATE INDEX IF NOT EXISTS idx_task_statistics_task_id ON task_statistics(task_id)"
        };

        try (Statement statement = connection.createStatement()) {
            for (String sql : ddl) {
                statement.execute(sql);
            }
            for (String sql : indexes) {
                statement.execute(sql);
            }
        }
    }

    private void seedDefaults() throws SQLException {
        String[][] defaultTags = {
            {"Важно", "#e67e22"},
            {"Срочно", "#e74c3c"},
            {"Работа", "#3498db"},
            {"Дом", "#2ecc71"},
            {"Учеба", "#16a085"}
        };

        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT OR IGNORE INTO tags(name, color) VALUES(?, ?)"
        )) {
            for (String[] tag : defaultTags) {
                statement.setString(1, tag[0]);
                statement.setString(2, tag[1]);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT OR IGNORE INTO app_settings(key, value) VALUES(?, ?)"
        )) {
            statement.setString(1, "use_database");
            statement.setString(2, "true");
            statement.addBatch();

            statement.setString(1, "reminder_minutes");
            statement.setString(2, "15");
            statement.addBatch();

            statement.setString(1, "theme");
            statement.setString(2, "summer");
            statement.addBatch();

            statement.setString(1, "notify_popup");
            statement.setString(2, "true");
            statement.addBatch();

            statement.setString(1, "notify_sound");
            statement.setString(2, "false");
            statement.addBatch();

            statement.setString(1, "notify_email");
            statement.setString(2, "false");
            statement.addBatch();

            statement.setString(1, "notify_email_to");
            statement.setString(2, "");
            statement.addBatch();

            statement.setString(1, "music_enabled");
            statement.setString(2, "true");
            statement.addBatch();

            statement.setString(1, "music_volume");
            statement.setString(2, "0.35");
            statement.addBatch();

            statement.setString(1, "widget_enabled");
            statement.setString(2, "false");
            statement.addBatch();
            statement.executeBatch();
        }
    }

    private void migrateSchema() throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(tasks)")) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }

        if (!columns.contains("sort_index")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE tasks ADD COLUMN sort_index INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!columns.contains("recurrence_rule")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE tasks ADD COLUMN recurrence_rule TEXT");
            }
        }
        if (!columns.contains("reminder_offset")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE tasks ADD COLUMN reminder_offset INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!columns.contains("recurrence_end")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE tasks ADD COLUMN recurrence_end TEXT");
            }
        }
    }

    public synchronized void saveTask(Task task) {
        String sql = """
            INSERT INTO tasks(
                id, title, description, start_time, end_time, priority,
                category, completed, overdue, created_at, updated_at,
                sort_index, recurrence_rule, reminder_offset, recurrence_end
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title=excluded.title,
                description=excluded.description,
                start_time=excluded.start_time,
                end_time=excluded.end_time,
                priority=excluded.priority,
                category=excluded.category,
                completed=excluded.completed,
                overdue=excluded.overdue,
                updated_at=excluded.updated_at,
                sort_index=excluded.sort_index,
                recurrence_rule=excluded.recurrence_rule,
                reminder_offset=excluded.reminder_offset,
                recurrence_end=excluded.recurrence_end
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String now = LocalDateTime.now().format(FORMATTER);
            statement.setString(1, task.getId());
            statement.setString(2, task.getTitle());
            statement.setString(3, task.getDescription());
            statement.setString(4, task.getStartTime().format(FORMATTER));
            statement.setString(5, task.getEndTime().format(FORMATTER));
            statement.setString(6, task.getPriority().name());
            statement.setString(7, task.getCategory().name());
            statement.setInt(8, task.isCompleted() ? 1 : 0);
            statement.setInt(9, task.isOverdue() ? 1 : 0);
            statement.setString(10, task.getCreatedAt().format(FORMATTER));
            statement.setString(11, now);
            statement.setInt(12, task.getSortIndex());
            statement.setString(13, task.getRecurrenceRule());
            statement.setInt(14, task.getReminderOffsetMinutes());
            statement.setString(15, task.getRecurrenceEnd() == null ? null : task.getRecurrenceEnd().format(FORMATTER));
            statement.executeUpdate();

            logTaskOperation(task.getId(), "UPSERT", "Task saved");
        } catch (SQLException e) {
            System.err.println("saveTask error: " + e.getMessage());
        }
    }

    public synchronized List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY sort_index ASC, end_time ASC";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(resultSetToTask(rs));
            }
        } catch (SQLException e) {
            System.err.println("getAllTasks error: " + e.getMessage());
        }

        return tasks;
    }

    public synchronized boolean deleteTask(String taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (existsTask(taskId)) {
                logTaskOperation(taskId, "DELETE", "Task removed");
            }
            statement.setString(1, taskId);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("deleteTask error: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateTask(Task task) {
        saveTask(task);
        return true;
    }

    public synchronized Task getTaskById(String taskId) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return resultSetToTask(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("getTaskById error: " + e.getMessage());
        }
        return null;
    }

    public synchronized List<Task> searchTasks(String query) {
        List<Task> tasks = new ArrayList<>();
        String sql = """
            SELECT * FROM tasks
            WHERE LOWER(title) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?)
            ORDER BY end_time ASC
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String wildcard = "%" + query + "%";
            statement.setString(1, wildcard);
            statement.setString(2, wildcard);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(resultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("searchTasks error: " + e.getMessage());
        }

        return tasks;
    }

    public synchronized List<Task> getTasksByCategory(String category) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE category = ? ORDER BY end_time ASC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(resultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("getTasksByCategory error: " + e.getMessage());
        }

        return tasks;
    }

    public synchronized List<String> getTaskTags(String taskId) {
        List<String> tags = new ArrayList<>();
        String sql = """
            SELECT t.name
            FROM tags t
            JOIN task_tags tt ON t.id = tt.tag_id
            WHERE tt.task_id = ?
            ORDER BY t.name ASC
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("getTaskTags error: " + e.getMessage());
        }

        return tags;
    }

    public synchronized void addTagToTask(String taskId, String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return;
        }

        try {
            int tagId = getOrCreateTag(tagName.trim());
            String sql = "INSERT OR IGNORE INTO task_tags(task_id, tag_id) VALUES(?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, taskId);
                statement.setInt(2, tagId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("addTagToTask error: " + e.getMessage());
        }
    }

    public synchronized void removeTagFromTask(String taskId, String tagName) {
        String sql = """
            DELETE FROM task_tags
            WHERE task_id = ?
              AND tag_id = (SELECT id FROM tags WHERE name = ?)
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            statement.setString(2, tagName);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("removeTagFromTask error: " + e.getMessage());
        }
    }

    public synchronized int getCompletedTasksCount() {
        return getCount("SELECT COUNT(*) FROM tasks WHERE completed = 1");
    }

    public synchronized int getPendingTasksCount() {
        return getCount("SELECT COUNT(*) FROM tasks WHERE completed = 0");
    }

    public synchronized int getOverdueTasksCount() {
        return getCount("SELECT COUNT(*) FROM tasks WHERE completed = 0 AND overdue = 1");
    }

    public synchronized int getTasksByPriority(String priority) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE priority = ? AND completed = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, priority);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("getTasksByPriority error: " + e.getMessage());
            return 0;
        }
    }

    public synchronized void saveSetting(String key, String value) {
        String sql = """
            INSERT INTO app_settings(key, value, updated_at) VALUES(?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(key) DO UPDATE SET
                value=excluded.value,
                updated_at=CURRENT_TIMESTAMP
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("saveSetting error: " + e.getMessage());
        }
    }

    public synchronized String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            System.err.println("getSetting error: " + e.getMessage());
        }
        return defaultValue;
    }

    private int getOrCreateTag(String tagName) throws SQLException {
        String findSql = "SELECT id FROM tags WHERE name = ?";
        try (PreparedStatement find = connection.prepareStatement(findSql)) {
            find.setString(1, tagName);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = "INSERT INTO tags(name, color) VALUES(?, '#95a5a6')";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, tagName);
            insert.executeUpdate();
        }

        try (PreparedStatement find = connection.prepareStatement(findSql)) {
            find.setString(1, tagName);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new SQLException("Cannot create tag: " + tagName);
    }

        private boolean existsTask(String taskId) {
        String sql = "SELECT 1 FROM tasks WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

private void logTaskOperation(String taskId, String operationType, String details) {
        String sql = "INSERT INTO task_statistics(task_id, operation_type, details) VALUES(?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            statement.setString(2, operationType);
            statement.setString(3, details);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("logTaskOperation error: " + e.getMessage());
        }
    }

    private int getCount(String sql) {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("count query error: " + e.getMessage());
            return 0;
        }
    }

    private Task resultSetToTask(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String startTime = rs.getString("start_time");
        String endTime = rs.getString("end_time");
        String priority = rs.getString("priority");
        String category = rs.getString("category");
        boolean completed = rs.getInt("completed") == 1;
        boolean overdue = rs.getInt("overdue") == 1;
        String createdAt = rs.getString("created_at");
        int sortIndex = rs.getInt("sort_index");
        String recurrenceRule = rs.getString("recurrence_rule");
        int reminderOffset = rs.getInt("reminder_offset");
        String recurrenceEnd = rs.getString("recurrence_end");

        if (createdAt == null || createdAt.isBlank()) {
            createdAt = LocalDateTime.now().format(FORMATTER);
        }
        if (startTime == null || startTime.isBlank()) {
            startTime = LocalDateTime.now().format(FORMATTER);
        }
        if (endTime == null || endTime.isBlank()) {
            endTime = LocalDateTime.now().plusHours(1).format(FORMATTER);
        }
        if (priority == null || priority.isBlank()) {
            priority = Priority.IMPORTANT.name();
        }
        if (category == null || category.isBlank()) {
            category = Category.OTHER.name();
        }

        return new Task(
            id,
            title,
            description,
            startTime,
            endTime,
            priority,
            category,
            completed,
            overdue,
            createdAt,
            sortIndex,
            recurrenceRule,
            reminderOffset,
            recurrenceEnd
        );
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Database close error: " + e.getMessage());
        }
    }
}
