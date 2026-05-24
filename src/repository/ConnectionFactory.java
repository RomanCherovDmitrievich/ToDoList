package repository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Фабрика JDBC-соединений для выбранной СУБД.
 */
public final class ConnectionFactory {
    private ConnectionFactory() {
    }

    public static Connection open(DatabaseConfig config) throws SQLException {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig must not be null");
        }
        return config.openConnection();
    }
}
