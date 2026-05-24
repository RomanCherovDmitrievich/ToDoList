package util;

import model.Category;
import model.Priority;
import model.Task;
import repository.DatabaseConfig;
import repository.DatabaseManager;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CLI-утилита для проверки подключения и массового наполнения БД.
 */
public final class DatabaseCli {
    private static final String PROBE_TABLE = "codex_connection_probe";
    private static final DateTimeFormatter PROBE_NOTE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] ACTIONS = {
        "Подготовить", "Проверить", "Согласовать", "Отправить",
        "Обновить", "Купить", "Позвонить", "Спланировать"
    };

    private static final String[] SUBJECTS = {
        "отчёт", "презентацию", "список покупок", "план встречи",
        "домашнее задание", "резюме", "календарь задач", "рабочий документ"
    };

    private static final String[] TAGS = {
        "Важно", "Срочно", "Работа", "Дом", "Учеба"
    };

    private DatabaseCli() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "probe" -> runProbe();
                case "seed" -> runSeed(args);
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Database CLI error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runProbe() throws Exception {
        DatabaseConfig config = DatabaseConfig.load();
        config.validate();
        config.loadDriver();

        if (config.isSqlite() && config.getSqlitePath() != null && config.getSqlitePath().getParent() != null) {
            Files.createDirectories(config.getSqlitePath().getParent());
        }

        if (config.isCustom()) {
            throw new IllegalStateException(
                "Probe mode is implemented for SQLite/PostgreSQL/MySQL/Firebird only. For another DB engine, adapt SQL in DatabaseCli."
            );
        }

        System.out.println("DB config: " + config.describe());

        try (Connection connection = config.openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createProbeTableSql(config));
        }

        String note = "probe from DatabaseCli at " + LocalDateTime.now().format(PROBE_NOTE_TIME);

        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO " + PROBE_TABLE + "(note) VALUES (?)"
             )) {
            statement.setString(1, note);
            statement.executeUpdate();
        }

        try (Connection connection = config.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(selectLatestProbeRowsSql(config))) {
            System.out.println("Latest probe rows:");
            while (rs.next()) {
                System.out.printf(
                    "  id=%s | note=%s | created_at=%s%n",
                    rs.getString("id"),
                    rs.getString("note"),
                    rs.getString("created_at")
                );
            }
        }

        try (Connection connection = config.openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE " + PROBE_TABLE);
        }

        System.out.println("Probe completed successfully.");
    }

    private static void runSeed(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Missing task count. Example: seed 1000");
        }

        int count = Integer.parseInt(args[1]);
        if (count <= 0) {
            throw new IllegalArgumentException("Task count must be greater than 0");
        }

        DatabaseManager databaseManager = DatabaseManager.getInstance();
        if (!databaseManager.isAvailable()) {
            throw new IllegalStateException(
                "Database is not available. Check db.properties and JDBC drivers before seeding."
            );
        }

        System.out.println("Seeding " + count + " tasks into " + databaseManager.getConfig().describe());

        LocalDateTime baseTime = LocalDateTime.now().withSecond(0).withNano(0);
        for (int i = 0; i < count; i++) {
            Task task = generateTask(i, baseTime);
            databaseManager.saveTask(task);
            databaseManager.addTagToTask(task.getId(), TAGS[i % TAGS.length]);
        }

        System.out.println("Created " + count + " tasks.");
    }

    private static Task generateTask(int index, LocalDateTime baseTime) {
        Priority priority = Priority.values()[index % Priority.values().length];
        Category category = Category.values()[index % Category.values().length];
        LocalDateTime start = baseTime.plusHours(index % 48).plusDays(index / 48);
        LocalDateTime end = start.plusMinutes(45 + (index % 6) * 15L);

        Task task = new Task(
            ACTIONS[index % ACTIONS.length] + " " + SUBJECTS[index % SUBJECTS.length] + " #" + (index + 1),
            "Автоматически созданная задача №" + (index + 1) + " для проверки нагрузки и работы БД.",
            start,
            end,
            priority,
            category
        );

        task.setSortIndex(index + 1);
        task.setReminderOffsetMinutes((index % 4) * 15);
        if (index % 10 == 0) {
            task.setRecurrenceRule("DAILY");
        } else if (index % 15 == 0) {
            task.setRecurrenceRule("WEEKLY");
        }

        return task;
    }

    private static String createProbeTableSql(DatabaseConfig config) {
        if (config.isPostgresql()) {
            return """
                CREATE TABLE IF NOT EXISTS codex_connection_probe (
                    id BIGSERIAL PRIMARY KEY,
                    note TEXT NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        }
        if (config.isMysql()) {
            return """
                CREATE TABLE IF NOT EXISTS codex_connection_probe (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    note VARCHAR(255) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        }
        if (config.isFirebird()) {
            return """
                CREATE TABLE codex_connection_probe (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    note VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        }

        return """
            CREATE TABLE IF NOT EXISTS codex_connection_probe (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                note TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
    }

    private static String selectLatestProbeRowsSql(DatabaseConfig config) {
        if (config.isFirebird()) {
            return "SELECT FIRST 3 id, note, created_at FROM " + PROBE_TABLE + " ORDER BY id DESC";
        }
        return "SELECT id, note, created_at FROM " + PROBE_TABLE + " ORDER BY id DESC LIMIT 3";
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java util.DatabaseCli probe");
        System.out.println("  java util.DatabaseCli seed <count>");
    }
}
