package unit.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.DatabaseConfig;

import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование DatabaseConfig")
class DatabaseConfigTest {

    @Test
    @DisplayName("SQLite конфиг используется по умолчанию")
    void testDefaultSqliteConfig() {
        Properties properties = new Properties();
        Path configFile = Path.of("data", "db.properties");
        Path defaultDb = Path.of("data", "todolist.db");

        DatabaseConfig config = DatabaseConfig.fromProperties(properties, configFile, defaultDb);

        assertTrue(config.isSqlite());
        assertEquals("jdbc:sqlite:" + defaultDb.toAbsolutePath(), config.getJdbcUrl());
        assertEquals(defaultDb.toAbsolutePath(), config.getSqlitePath());
    }

    @Test
    @DisplayName("PostgreSQL конфиг собирает JDBC URL и credentials")
    void testPostgresqlConfig() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "postgresql");
        properties.setProperty("db.host", "example.org");
        properties.setProperty("db.port", "5432");
        properties.setProperty("db.name", "dbstud");
        properties.setProperty("db.username", "chernov_rd");
        properties.setProperty("db.password", "secret");
        properties.setProperty("db.sslMode", "require");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        config.validate();

        assertTrue(config.isPostgresql());
        assertEquals(
            "jdbc:postgresql://example.org:5432/dbstud?sslmode=require",
            config.getJdbcUrl()
        );
        assertEquals("chernov_rd", config.getConnectionProperties().getProperty("user"));
        assertEquals("secret", config.getConnectionProperties().getProperty("password"));
    }

    @Test
    @DisplayName("Custom JDBC без URL и драйвера валидироваться не должен")
    void testCustomConfigRequiresJdbcUrlAndDriver() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "custom");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        assertThrows(IllegalStateException.class, config::validate);
    }

    @Test
    @DisplayName("MySQL конфиг собирает JDBC URL и credentials")
    void testMysqlConfig() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "mysql");
        properties.setProperty("db.host", "mysql.example.org");
        properties.setProperty("db.port", "3306");
        properties.setProperty("db.name", "todo_db");
        properties.setProperty("db.username", "todo_user");
        properties.setProperty("db.password", "topsecret");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        config.validate();

        assertTrue(config.isMysql());
        assertEquals(
            "jdbc:mysql://mysql.example.org:3306/todo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            config.getJdbcUrl()
        );
        assertEquals("todo_user", config.getConnectionProperties().getProperty("user"));
        assertEquals("topsecret", config.getConnectionProperties().getProperty("password"));
    }

    @Test
    @DisplayName("Firebird конфиг собирает JDBC URL и credentials")
    void testFirebirdConfig() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "firebird");
        properties.setProperty("db.host", "192.168.1.6");
        properties.setProperty("db.port", "3050");
        properties.setProperty("db.name", "emp");
        properties.setProperty("db.username", "user113");
        properties.setProperty("db.password", "L@b$-113");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        config.validate();

        assertTrue(config.isFirebird());
        assertEquals(
            "jdbc:firebirdsql://192.168.1.6:3050/emp?encoding=UTF8",
            config.getJdbcUrl()
        );
        assertEquals("user113", config.getConnectionProperties().getProperty("user"));
        assertEquals("L@b$-113", config.getConnectionProperties().getProperty("password"));
    }

    @Test
    @DisplayName("Профиль PostgreSQL выбирается одной строкой db.type")
    void testPostgresqlProfileSelection() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "postgresql");
        properties.setProperty("db.postgresql.host", "pg.example.org");
        properties.setProperty("db.postgresql.port", "5433");
        properties.setProperty("db.postgresql.name", "todo_pg");
        properties.setProperty("db.postgresql.username", "pg_user");
        properties.setProperty("db.postgresql.password", "pg_secret");
        properties.setProperty("db.postgresql.sslMode", "disable");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        assertEquals("jdbc:postgresql://pg.example.org:5433/todo_pg?sslmode=disable", config.getJdbcUrl());
        assertEquals("pg_user", config.getConnectionProperties().getProperty("user"));
    }

    @Test
    @DisplayName("Профиль MySQL выбирается одной строкой db.type")
    void testMysqlProfileSelection() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "mysql");
        properties.setProperty("db.mysql.host", "mysql.local");
        properties.setProperty("db.mysql.port", "3307");
        properties.setProperty("db.mysql.name", "todo_mysql");
        properties.setProperty("db.mysql.username", "mysql_user");
        properties.setProperty("db.mysql.password", "mysql_secret");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        assertEquals(
            "jdbc:mysql://mysql.local:3307/todo_mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            config.getJdbcUrl()
        );
        assertEquals("mysql_user", config.getConnectionProperties().getProperty("user"));
    }

    @Test
    @DisplayName("Профиль Firebird выбирается одной строкой db.type")
    void testFirebirdProfileSelection() {
        Properties properties = new Properties();
        properties.setProperty("db.type", "firebird");
        properties.setProperty("db.firebird.host", "192.168.1.6");
        properties.setProperty("db.firebird.port", "3050");
        properties.setProperty("db.firebird.name", "emp");
        properties.setProperty("db.firebird.username", "user113");
        properties.setProperty("db.firebird.password", "secret");

        DatabaseConfig config = DatabaseConfig.fromProperties(
            properties,
            Path.of("data", "db.properties"),
            Path.of("data", "todolist.db")
        );

        assertEquals("jdbc:firebirdsql://192.168.1.6:3050/emp?encoding=UTF8", config.getJdbcUrl());
        assertEquals("user113", config.getConnectionProperties().getProperty("user"));
    }
}
