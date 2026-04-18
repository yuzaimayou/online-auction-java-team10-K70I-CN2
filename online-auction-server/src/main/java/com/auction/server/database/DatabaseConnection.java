package com.auction.server.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String DB_PATH = PROJECT_ROOT + File.separator + "dataBase" + File.separator + "auction.db";

    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private DatabaseConnection() {
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}