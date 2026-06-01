package com.auction.server.database;

import com.auction.server.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static HikariDataSource dataSource;

    public static void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + AppConfig.DB_PATH);
        config.setConnectionInitSql(
                "PRAGMA journal_mode=WAL; " +
                        "PRAGMA busy_timeout=5000; " +
                        "PRAGMA synchronous=NORMAL;");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        dataSource = new HikariDataSource(config);
        LOGGER.info("Database connection pool initialized with SQLite at " + AppConfig.DB_PATH);

    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager not initialized. Call init() first.");
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            LOGGER.info("Database connection pool shut down.");
        }
    }
}
