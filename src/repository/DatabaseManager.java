package repository;

import model.Task;
import model.Category;
import model.Priority;
import model.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
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
 * DAO для задач и связанных таблиц с поддержкой нескольких реляционных СУБД.
 */
public class DatabaseManager implements TaskDao {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static DatabaseManager instance;
    private Connection connection;
    private DatabaseConfig config;
    private SqlDialect dialect;

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
            config = DatabaseConfig.load();
            config.validate();
            config.loadDriver();
            dialect = SqlDialect.from(config.getType());
            connection = ConnectionFactory.open(config);

            enablePragmas();
            createTables();
            migrateSchema();
            seedDefaults();
            System.out.println("Database ready: " + config.describe());
        } catch (Exception e) {
            System.err.println("Database init error: " + e.getMessage());
        }
    }

    private void enablePragmas() throws SQLException {
        if (connection == null || config == null || !config.isSqlite()) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }

    private void createTables() throws SQLException {
        if (!hasConnection()) {
            return;
        }
        String identityColumn = dialect.identityColumn();
        String textType = dialect.textType();
        String timestampType = dialect.timestampType();
        String generatedTimestampType = dialect.generatedTimestampType();

        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(64) PRIMARY KEY,
                username VARCHAR(100) NOT NULL UNIQUE,
                email VARCHAR(255),
                password_hash VARCHAR(255) NOT NULL,
                password_salt VARCHAR(255) NOT NULL,
                created_at %s NOT NULL,
                updated_at %s NOT NULL,
                reset_code VARCHAR(32),
                reset_code_expires_at %s
            )
            """.formatted(timestampType, timestampType, timestampType),
            """
            CREATE TABLE IF NOT EXISTS tasks (
                id VARCHAR(64) PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description %s,
                start_time %s NOT NULL,
                end_time %s NOT NULL,
                priority VARCHAR(32) NOT NULL CHECK(priority IN ('URGENT','IMPORTANT','NORMAL')),
                category VARCHAR(32) NOT NULL CHECK(category IN ('WORK','HOME','STUDY','OTHER')),
                completed INTEGER NOT NULL DEFAULT 0,
                overdue INTEGER NOT NULL DEFAULT 0,
                created_at %s NOT NULL,
                updated_at %s NOT NULL,
                sort_index INTEGER NOT NULL DEFAULT 0,
                owner_user_id VARCHAR(64),
                recurrence_rule VARCHAR(64),
                reminder_offset INTEGER NOT NULL DEFAULT 0,
                recurrence_end %s
            )
            """.formatted(textType, timestampType, timestampType, timestampType, timestampType, timestampType),
            String.format("""
            CREATE TABLE IF NOT EXISTS tags (
                id %s,
                name VARCHAR(128) UNIQUE NOT NULL,
                color VARCHAR(16) NOT NULL DEFAULT '#95a5a6'
            )
            """, identityColumn),
            """
            CREATE TABLE IF NOT EXISTS task_tags (
                task_id VARCHAR(64) NOT NULL,
                tag_id BIGINT NOT NULL,
                assigned_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (task_id, tag_id),
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.formatted(generatedTimestampType),
            String.format("""
            CREATE TABLE IF NOT EXISTS task_statistics (
                id %s,
                task_id VARCHAR(64) NOT NULL,
                operation_type VARCHAR(32) NOT NULL,
                operation_time %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                details %s,
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
            )
            """, identityColumn, generatedTimestampType, textType),
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                %s VARCHAR(128) PRIMARY KEY,
                value %s,
                updated_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """.formatted(dialect.settingsKeyIdentifier(), textType, generatedTimestampType)
        };

        String[] indexes = dialect == SqlDialect.MYSQL
            ? new String[] {
                "CREATE INDEX idx_users_username ON users(username)",
                "CREATE INDEX idx_users_email ON users(email)",
                "CREATE INDEX idx_tasks_completed ON tasks(completed)",
                "CREATE INDEX idx_tasks_priority ON tasks(priority)",
                "CREATE INDEX idx_tasks_category ON tasks(category)",
                "CREATE INDEX idx_tasks_end_time ON tasks(end_time)",
                "CREATE INDEX idx_tasks_owner_user_id ON tasks(owner_user_id)",
                "CREATE INDEX idx_task_tags_task_id ON task_tags(task_id)",
                "CREATE INDEX idx_task_statistics_task_id ON task_statistics(task_id)"
            }
            : new String[] {
                "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)",
                "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_end_time ON tasks(end_time)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_owner_user_id ON tasks(owner_user_id)",
                "CREATE INDEX IF NOT EXISTS idx_task_tags_task_id ON task_tags(task_id)",
                "CREATE INDEX IF NOT EXISTS idx_task_statistics_task_id ON task_statistics(task_id)"
            };

        try (Statement statement = connection.createStatement()) {
            for (String sql : ddl) {
                executeDdlStatement(statement, sql);
            }
            for (String sql : indexes) {
                executeIndexStatement(statement, sql);
            }
        }
    }

    private void seedDefaults() throws SQLException {
        if (!hasConnection()) {
            return;
        }
        String insertTagSql = dialect.insertIgnoreOnConstraint("tags", "(name, color) VALUES(?, ?)", "name");
        String insertSettingSql = dialect.insertIgnoreOnConstraint(
            "app_settings",
            "(" + dialect.settingsKeyIdentifier() + ", value) VALUES(?, ?)",
            "key"
        );

        String[][] defaultTags = {
            {"Важно", "#e67e22"},
            {"Срочно", "#e74c3c"},
            {"Работа", "#3498db"},
            {"Дом", "#2ecc71"},
            {"Учеба", "#16a085"}
        };

        try (PreparedStatement statement = connection.prepareStatement(insertTagSql)) {
            for (String[] tag : defaultTags) {
                statement.setString(1, tag[0]);
                statement.setString(2, tag[1]);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement(insertSettingSql)) {
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
        if (!hasConnection()) {
            return;
        }
        migrateUsersSchema();

        String textType = dialect.textType();
        String timestampType = dialect.timestampType();
        Set<String> columns = new HashSet<>();
        if (dialect == SqlDialect.SQLITE) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("PRAGMA table_info(tasks)")) {
                while (rs.next()) {
                    columns.add(rs.getString("name"));
                }
            }
        } else if (dialect == SqlDialect.FIREBIRD) {
            String sql = """
                SELECT TRIM(r.RDB$FIELD_NAME) AS column_name
                FROM RDB$RELATION_FIELDS r
                WHERE r.RDB$RELATION_NAME = 'TASKS'
                """;
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name").toLowerCase());
                }
            }
        } else {
            String sql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = %s
                  AND table_name = 'tasks'
                """.formatted(dialect == SqlDialect.MYSQL ? "DATABASE()" : "current_schema()");
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name").toLowerCase());
                }
            }
        }

        if (!columns.contains("sort_index")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterAddColumnSql("sort_index INTEGER NOT NULL DEFAULT 0"));
            }
        }
        if (!columns.contains("owner_user_id")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterAddColumnSql("owner_user_id VARCHAR(64)"));
            }
        }
        if (!columns.contains("recurrence_rule")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterAddColumnSql("recurrence_rule VARCHAR(64)"));
            }
        }
        if (!columns.contains("reminder_offset")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterAddColumnSql("reminder_offset INTEGER NOT NULL DEFAULT 0"));
            }
        }
        if (!columns.contains("recurrence_end")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterAddColumnSql("recurrence_end " + timestampType));
            }
        }
    }

    private void migrateUsersSchema() throws SQLException {
        if (!isUsersEmailUnique()) {
            return;
        }

        switch (dialect) {
            case SQLITE -> rebuildSqliteUsersTableWithoutUniqueEmail();
            case MYSQL -> dropMysqlUniqueEmailIndexes();
            case POSTGRESQL -> dropPostgresqlUniqueEmailConstraints();
            case FIREBIRD -> dropFirebirdUniqueEmailConstraints();
        }
    }

    private boolean isUsersEmailUnique() throws SQLException {
        return switch (dialect) {
            case SQLITE -> hasSqliteUniqueEmailConstraint();
            case MYSQL -> !findMysqlUniqueEmailIndexes().isEmpty();
            case POSTGRESQL -> !findPostgresqlUniqueEmailConstraints().isEmpty();
            case FIREBIRD -> !findFirebirdUniqueEmailConstraints().isEmpty();
        };
    }

    private boolean hasSqliteUniqueEmailConstraint() throws SQLException {
        String sql = "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (!rs.next()) {
                return false;
            }
            String ddl = rs.getString("sql");
            if (ddl == null) {
                return false;
            }
            String normalized = ddl.toLowerCase();
            return normalized.contains("email text unique")
                || normalized.contains("email varchar(255) unique")
                || normalized.contains("email varchar(255),")
                && normalized.contains("unique(email)");
        }
    }

    private void rebuildSqliteUsersTableWithoutUniqueEmail() throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("ALTER TABLE users RENAME TO users_old");
            statement.execute("""
                CREATE TABLE users (
                    id VARCHAR(64) PRIMARY KEY,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    email VARCHAR(255),
                    password_hash VARCHAR(255) NOT NULL,
                    password_salt VARCHAR(255) NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    reset_code VARCHAR(32),
                    reset_code_expires_at TEXT
                )
                """);
            statement.execute("""
                INSERT INTO users(
                    id, username, email, password_hash, password_salt,
                    created_at, updated_at, reset_code, reset_code_expires_at
                )
                SELECT
                    id, username, email, password_hash, password_salt,
                    created_at, updated_at, reset_code, reset_code_expires_at
                FROM users_old
                """);
            statement.execute("DROP TABLE users_old");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            statement.execute("PRAGMA foreign_keys = ON");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private List<String> findMysqlUniqueEmailIndexes() throws SQLException {
        List<String> indexes = new ArrayList<>();
        String sql = """
            SELECT INDEX_NAME
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'email'
              AND non_unique = 0
              AND index_name <> 'PRIMARY'
            """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                indexes.add(rs.getString("INDEX_NAME"));
            }
        }
        return indexes;
    }

    private void dropMysqlUniqueEmailIndexes() throws SQLException {
        for (String indexName : findMysqlUniqueEmailIndexes()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE users DROP INDEX `" + indexName + "`");
            }
        }
    }

    private List<String> findPostgresqlUniqueEmailConstraints() throws SQLException {
        List<String> constraints = new ArrayList<>();
        String sql = """
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE t.relname = 'users'
              AND n.nspname = current_schema()
              AND c.contype = 'u'
              AND pg_get_constraintdef(c.oid) ILIKE '%(email)%'
            """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                constraints.add(rs.getString("conname"));
            }
        }
        return constraints;
    }

    private void dropPostgresqlUniqueEmailConstraints() throws SQLException {
        for (String constraintName : findPostgresqlUniqueEmailConstraints()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS " + constraintName);
            }
        }
    }

    private List<String> findFirebirdUniqueEmailConstraints() throws SQLException {
        List<String> constraints = new ArrayList<>();
        String sql = """
            SELECT TRIM(rc.RDB$CONSTRAINT_NAME) AS constraint_name
            FROM RDB$RELATION_CONSTRAINTS rc
            JOIN RDB$INDEX_SEGMENTS seg ON seg.RDB$INDEX_NAME = rc.RDB$INDEX_NAME
            WHERE rc.RDB$RELATION_NAME = 'USERS'
              AND rc.RDB$CONSTRAINT_TYPE = 'UNIQUE'
              AND TRIM(seg.RDB$FIELD_NAME) = 'EMAIL'
            """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                constraints.add(rs.getString("constraint_name"));
            }
        }
        return constraints;
    }

    private void dropFirebirdUniqueEmailConstraints() throws SQLException {
        for (String constraintName : findFirebirdUniqueEmailConstraints()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE USERS DROP CONSTRAINT " + constraintName);
            }
        }
    }

    private String alterAddColumnSql(String columnDefinition) {
        return dialect.addColumnSql("tasks", columnDefinition);
    }

    private void executeDdlStatement(Statement statement, String sql) throws SQLException {
        try {
            if (dialect == SqlDialect.FIREBIRD && sql.contains("CREATE TABLE IF NOT EXISTS")) {
                sql = sql.replace("CREATE TABLE IF NOT EXISTS", "CREATE TABLE");
            }
            statement.execute(sql);
        } catch (SQLException e) {
            if (dialect == SqlDialect.FIREBIRD && isDuplicateObjectError(e)) {
                return;
            }
            throw e;
        }
    }

    private void executeIndexStatement(Statement statement, String sql) throws SQLException {
        try {
            if (dialect == SqlDialect.FIREBIRD && sql.contains(" IF NOT EXISTS ")) {
                sql = sql.replace(" IF NOT EXISTS", "");
            }
            statement.execute(sql);
        } catch (SQLException e) {
            if ((dialect == SqlDialect.MYSQL || dialect == SqlDialect.FIREBIRD) && isDuplicateIndexError(e)) {
                return;
            }
            throw e;
        }
    }

    private boolean isDuplicateIndexError(SQLException exception) {
        return exception.getErrorCode() == 1061 || isDuplicateObjectError(exception);
    }

    private boolean isDuplicateObjectError(SQLException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("already exists")
            || normalized.contains("unsuccessful metadata update")
            || normalized.contains("attempt to store duplicate value");
    }

    @Override
    public synchronized void saveTask(Task task) {
        if (!hasConnection()) {
            return;
        }
        String sql = dialect.upsertTaskSql();

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
            statement.setString(13, task.getOwnerUserId());
            statement.setString(14, task.getRecurrenceRule());
            statement.setInt(15, task.getReminderOffsetMinutes());
            statement.setString(16, task.getRecurrenceEnd() == null ? null : task.getRecurrenceEnd().format(FORMATTER));
            statement.executeUpdate();

            logTaskOperation(task.getId(), "UPSERT", "Task saved");
        } catch (SQLException e) {
            System.err.println("saveTask error: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        if (!hasConnection()) {
            return tasks;
        }
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

    @Override
    public synchronized boolean deleteTask(String taskId) {
        if (!hasConnection()) {
            return false;
        }
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

    @Override
    public synchronized boolean updateTask(Task task) {
        saveTask(task);
        return true;
    }

    @Override
    public synchronized Task getTaskById(String taskId) {
        if (!hasConnection()) {
            return null;
        }
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

    @Override
    public synchronized List<Task> searchTasks(String query) {
        List<Task> tasks = new ArrayList<>();
        if (!hasConnection()) {
            return tasks;
        }
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

    @Override
    public synchronized List<Task> getTasksByCategory(String category) {
        List<Task> tasks = new ArrayList<>();
        if (!hasConnection()) {
            return tasks;
        }
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
        if (!hasConnection()) {
            return tags;
        }
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
        if (!hasConnection() || tagName == null || tagName.isBlank()) {
            return;
        }

        try {
            int tagId = getOrCreateTag(tagName.trim());
            String sql = dialect.insertIgnoreOnConstraint("task_tags", "(task_id, tag_id) VALUES(?, ?)", "task_id, tag_id");
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
        if (!hasConnection()) {
            return;
        }
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
        if (!hasConnection()) {
            return 0;
        }
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
        if (!hasConnection()) {
            return;
        }
        String sql = dialect.upsertSettingSql();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("saveSetting error: " + e.getMessage());
        }
    }

    public synchronized String getSetting(String key, String defaultValue) {
        if (!hasConnection()) {
            return defaultValue;
        }
        String sql = "SELECT value FROM app_settings WHERE " + dialect.settingsKeyIdentifier() + " = ?";
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

    public synchronized int getUsersCount() {
        return getCount("SELECT COUNT(*) FROM users");
    }

    public synchronized User createUser(String id, String username, String email, String passwordHash, String passwordSalt) {
        if (!hasConnection()) {
            throw new IllegalStateException("Database is not available");
        }

        String sql = """
            INSERT INTO users(
                id, username, email, password_hash, password_salt, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String now = LocalDateTime.now().format(FORMATTER);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, username);
            statement.setString(3, email == null || email.isBlank() ? null : email);
            statement.setString(4, passwordHash);
            statement.setString(5, passwordSalt);
            statement.setString(6, now);
            statement.setString(7, now);
            statement.executeUpdate();
            return getUserById(id);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось создать пользователя: " + e.getMessage(), e);
        }
    }

    public synchronized User findUserByIdentifier(String identifier) {
        if (!hasConnection() || identifier == null || identifier.isBlank()) {
            return null;
        }
        User userByUsername = findUserByUsername(identifier);
        if (userByUsername != null) {
            return userByUsername;
        }

        List<User> usersByEmail = findUsersByEmail(identifier);
        return usersByEmail.size() == 1 ? usersByEmail.get(0) : null;
    }

    public synchronized User findUserByUsername(String username) {
        if (!hasConnection() || username == null || username.isBlank()) {
            return null;
        }

        String sql = """
            SELECT *
            FROM users
            WHERE LOWER(username) = LOWER(?)
            LIMIT 1
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return resultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("findUserByUsername error: " + e.getMessage());
        }
        return null;
    }

    public synchronized List<User> findUsersByEmail(String email) {
        List<User> users = new ArrayList<>();
        if (!hasConnection() || email == null || email.isBlank()) {
            return users;
        }

        String sql = """
            SELECT *
            FROM users
            WHERE LOWER(COALESCE(email, '')) = LOWER(?)
            ORDER BY created_at ASC
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(resultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("findUsersByEmail error: " + e.getMessage());
        }
        return users;
    }

    public synchronized User getUserById(String userId) {
        if (!hasConnection() || userId == null || userId.isBlank()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return resultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("getUserById error: " + e.getMessage());
        }
        return null;
    }

    public synchronized boolean saveResetCode(String userId, String resetCode, LocalDateTime expiresAt) {
        return updateUserRecovery(userId, resetCode, expiresAt);
    }

    public synchronized boolean clearResetCode(String userId) {
        return updateUserRecovery(userId, null, null);
    }

    public synchronized boolean updateUserCredentials(String userId, String passwordHash, String passwordSalt) {
        if (!hasConnection()) {
            return false;
        }

        String sql = """
            UPDATE users
            SET password_hash = ?,
                password_salt = ?,
                reset_code = NULL,
                reset_code_expires_at = NULL,
                updated_at = ?
            WHERE id = ?
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, passwordSalt);
            statement.setString(3, LocalDateTime.now().format(FORMATTER));
            statement.setString(4, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateUserCredentials error: " + e.getMessage());
            return false;
        }
    }

    public synchronized int claimOrphanTasks(String userId) {
        if (!hasConnection() || userId == null || userId.isBlank()) {
            return 0;
        }

        String sql = """
            UPDATE tasks
            SET owner_user_id = ?
            WHERE owner_user_id IS NULL OR owner_user_id = ''
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("claimOrphanTasks error: " + e.getMessage());
            return 0;
        }
    }

    private int getOrCreateTag(String tagName) throws SQLException {
        if (!hasConnection()) {
            throw new SQLException("Database connection is not initialized");
        }
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

    private boolean updateUserRecovery(String userId, String resetCode, LocalDateTime expiresAt) {
        if (!hasConnection()) {
            return false;
        }

        String sql = """
            UPDATE users
            SET reset_code = ?,
                reset_code_expires_at = ?,
                updated_at = ?
            WHERE id = ?
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resetCode);
            statement.setString(2, expiresAt == null ? null : expiresAt.format(FORMATTER));
            statement.setString(3, LocalDateTime.now().format(FORMATTER));
            statement.setString(4, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateUserRecovery error: " + e.getMessage());
            return false;
        }
    }

    private boolean existsTask(String taskId) {
        if (!hasConnection()) {
            return false;
        }
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
        if (!hasConnection()) {
            return;
        }
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
        if (!hasConnection()) {
            return 0;
        }
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
        String ownerUserId = rs.getString("owner_user_id");
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

        Task task = new Task(
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
        task.setOwnerUserId(ownerUserId);
        return task;
    }

    private User resultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getString("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("password_salt"),
            parseDatabaseDateTime(rs.getString("created_at")),
            parseDatabaseDateTime(rs.getString("updated_at")),
            rs.getString("reset_code"),
            parseOptionalDatabaseDateTime(rs.getString("reset_code_expires_at"))
        );
    }

    private LocalDateTime parseDatabaseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        return LocalDateTime.parse(value, FORMATTER);
    }

    private LocalDateTime parseOptionalDatabaseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, FORMATTER);
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

    public synchronized DatabaseConfig getConfig() {
        return config;
    }

    public synchronized boolean isAvailable() {
        return hasConnection();
    }

    private boolean hasConnection() {
        return connection != null;
    }
}
