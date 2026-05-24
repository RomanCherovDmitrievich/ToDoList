package repository;

import util.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * Настройки подключения к БД.
 */
public final class DatabaseConfig {
    public enum Type {
        SQLITE,
        POSTGRESQL,
        MYSQL,
        FIREBIRD,
        CUSTOM
    }

    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String FIREBIRD_DRIVER = "org.firebirdsql.jdbc.FBDriver";

    private final Type type;
    private final Path configFile;
    private final Path sqlitePath;
    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;
    private final String jdbcUrl;
    private final String driverClass;
    private final String sslMode;

    private DatabaseConfig(
        Type type,
        Path configFile,
        Path sqlitePath,
        String host,
        int port,
        String databaseName,
        String username,
        String password,
        String jdbcUrl,
        String driverClass,
        String sslMode
    ) {
        this.type = type;
        this.configFile = configFile;
        this.sqlitePath = sqlitePath;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.jdbcUrl = jdbcUrl;
        this.driverClass = driverClass;
        this.sslMode = sslMode;
    }

    public static DatabaseConfig load() {
        Path configFile = PathResolver.getDatabaseConfigFile();
        Properties properties = new Properties();

        if (Files.exists(configFile)) {
            try (InputStream input = Files.newInputStream(configFile)) {
                properties.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read DB config: " + configFile, e);
            }
        }

        applyEnvironmentOverride(properties, "db.type", "TODOLIST_DB_TYPE");
        applyEnvironmentOverride(properties, "db.sqlitePath", "TODOLIST_DB_SQLITE_PATH");
        applyEnvironmentOverride(properties, "db.host", "TODOLIST_DB_HOST");
        applyEnvironmentOverride(properties, "db.port", "TODOLIST_DB_PORT");
        applyEnvironmentOverride(properties, "db.name", "TODOLIST_DB_NAME");
        applyEnvironmentOverride(properties, "db.username", "TODOLIST_DB_USERNAME");
        applyEnvironmentOverride(properties, "db.password", "TODOLIST_DB_PASSWORD");
        applyEnvironmentOverride(properties, "db.jdbcUrl", "TODOLIST_DB_JDBC_URL");
        applyEnvironmentOverride(properties, "db.driverClass", "TODOLIST_DB_DRIVER_CLASS");
        applyEnvironmentOverride(properties, "db.sslMode", "TODOLIST_DB_SSL_MODE");

        return fromProperties(properties, configFile, PathResolver.getDatabaseFile());
    }

    public static DatabaseConfig fromProperties(Properties properties, Path configFile, Path defaultSqlitePath) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(configFile, "configFile");
        Objects.requireNonNull(defaultSqlitePath, "defaultSqlitePath");

        Type type = parseType(read(properties, "db.type", "sqlite"));
        Path sqlitePath = resolveSqlitePath(readByType(properties, type, "sqlitePath", "db.sqlitePath", ""), defaultSqlitePath);
        String host = readByType(properties, type, "host", "db.host", "");
        int port = parsePort(readByType(properties, type, "port", "db.port", ""), defaultPort(type));
        String databaseName = readByType(properties, type, "name", "db.name", "");
        String username = readByType(properties, type, "username", "db.username", "");
        String password = readByType(properties, type, "password", "db.password", "");
        String jdbcUrl = readByType(properties, type, "jdbcUrl", "db.jdbcUrl", "");
        String driverClass = readByType(properties, type, "driverClass", "db.driverClass", "");
        String sslMode = readByType(properties, type, "sslMode", "db.sslMode", "require");

        return new DatabaseConfig(
            type,
            configFile.toAbsolutePath(),
            sqlitePath,
            host,
            port,
            databaseName,
            username,
            password,
            jdbcUrl,
            driverClass,
            sslMode
        );
    }

    public void validate() {
        switch (type) {
            case SQLITE -> {
                if (sqlitePath == null) {
                    throw new IllegalStateException("SQLite path is not configured");
                }
            }
            case POSTGRESQL -> {
                if (host.isBlank() || databaseName.isBlank() || username.isBlank()) {
                    throw new IllegalStateException(
                        "PostgreSQL config is incomplete. Fill db.host, db.name and db.username in " + configFile
                    );
                }
            }
            case MYSQL -> {
                if (host.isBlank() || databaseName.isBlank() || username.isBlank()) {
                    throw new IllegalStateException(
                        "MySQL config is incomplete. Fill db.host, db.name and db.username in " + configFile
                    );
                }
            }
            case FIREBIRD -> {
                if (host.isBlank() || databaseName.isBlank() || username.isBlank()) {
                    throw new IllegalStateException(
                        "Firebird/Red DB config is incomplete. Fill db.host, db.name and db.username in " + configFile
                    );
                }
            }
            case CUSTOM -> {
                if (jdbcUrl.isBlank() || getDriverClass().isBlank()) {
                    throw new IllegalStateException(
                        "Custom JDBC config is incomplete. Fill db.jdbcUrl and db.driverClass in " + configFile
                    );
                }
            }
            default -> throw new IllegalStateException("Unsupported DB type: " + type);
        }
    }

    public Connection openConnection() throws SQLException {
        Properties connectionProperties = getConnectionProperties();
        String resolvedJdbcUrl = getJdbcUrl();
        if (connectionProperties.isEmpty()) {
            return DriverManager.getConnection(resolvedJdbcUrl);
        }
        return DriverManager.getConnection(resolvedJdbcUrl, connectionProperties);
    }

    public void loadDriver() throws ClassNotFoundException {
        Class.forName(getDriverClass());
    }

    public Properties getConnectionProperties() {
        Properties properties = new Properties();
        if (!username.isBlank()) {
            properties.setProperty("user", username);
        }
        if (!password.isBlank()) {
            properties.setProperty("password", password);
        }
        return properties;
    }

    public String getJdbcUrl() {
        return switch (type) {
            case SQLITE -> "jdbc:sqlite:" + sqlitePath.toAbsolutePath();
            case POSTGRESQL -> {
                StringBuilder builder = new StringBuilder("jdbc:postgresql://")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append("/")
                    .append(databaseName);
                if (!sslMode.isBlank()) {
                    builder.append("?sslmode=").append(sslMode);
                }
                yield builder.toString();
            }
            case MYSQL -> new StringBuilder("jdbc:mysql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(databaseName)
                .append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC")
                .toString();
            case FIREBIRD -> new StringBuilder("jdbc:firebirdsql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(databaseName)
                .append("?encoding=UTF8")
                .toString();
            case CUSTOM -> jdbcUrl;
        };
    }

    public String getDriverClass() {
        return switch (type) {
            case SQLITE -> SQLITE_DRIVER;
            case POSTGRESQL -> POSTGRES_DRIVER;
            case MYSQL -> MYSQL_DRIVER;
            case FIREBIRD -> FIREBIRD_DRIVER;
            case CUSTOM -> driverClass;
        };
    }

    public String describe() {
        return switch (type) {
            case SQLITE -> "SQLite -> " + sqlitePath.toAbsolutePath();
            case POSTGRESQL -> "PostgreSQL -> " + host + ":" + port + "/" + databaseName + " (sslmode=" + sslMode + ")";
            case MYSQL -> "MySQL -> " + host + ":" + port + "/" + databaseName;
            case FIREBIRD -> "Firebird/Red DB -> " + host + ":" + port + "/" + databaseName;
            case CUSTOM -> "Custom JDBC -> " + jdbcUrl;
        };
    }

    public Type getType() {
        return type;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public Path getSqlitePath() {
        return sqlitePath;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSslMode() {
        return sslMode;
    }

    public boolean isSqlite() {
        return type == Type.SQLITE;
    }

    public boolean isPostgresql() {
        return type == Type.POSTGRESQL;
    }

    public boolean isCustom() {
        return type == Type.CUSTOM;
    }

    public boolean isMysql() {
        return type == Type.MYSQL;
    }

    public boolean isFirebird() {
        return type == Type.FIREBIRD;
    }

    private static void applyEnvironmentOverride(Properties properties, String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            properties.setProperty(propertyKey, envValue);
        }
    }

    private static String read(Properties properties, String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }

    private static String readByType(
        Properties properties,
        Type type,
        String suffix,
        String fallbackKey,
        String defaultValue
    ) {
        String prefix = switch (type) {
            case SQLITE -> "db.sqlite.";
            case POSTGRESQL -> "db.postgresql.";
            case MYSQL -> "db.mysql.";
            case FIREBIRD -> "db.firebird.";
            case CUSTOM -> "db.custom.";
        };
        String typedKey = prefix + suffix;
        String typedValue = properties.getProperty(typedKey);
        if (typedValue != null && !typedValue.isBlank()) {
            return typedValue.trim();
        }
        return read(properties, fallbackKey, defaultValue);
    }

    private static Type parseType(String value) {
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "", "sqlite" -> Type.SQLITE;
            case "postgres", "postgresql" -> Type.POSTGRESQL;
            case "mysql" -> Type.MYSQL;
            case "firebird", "red", "reddb", "red_db" -> Type.FIREBIRD;
            case "custom", "jdbc" -> Type.CUSTOM;
            default -> throw new IllegalArgumentException("Unsupported db.type: " + value);
        };
    }

    private static int defaultPort(Type type) {
        return switch (type) {
            case POSTGRESQL -> 5432;
            case MYSQL -> 3306;
            case FIREBIRD -> 3050;
            default -> 0;
        };
    }

    private static int parsePort(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private static Path resolveSqlitePath(String configuredPath, Path defaultPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return defaultPath.toAbsolutePath();
        }
        Path path = Paths.get(configuredPath.trim());
        return path.isAbsolute() ? path : path.toAbsolutePath();
    }
}
