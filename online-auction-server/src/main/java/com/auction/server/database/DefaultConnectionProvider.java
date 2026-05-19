package com.auction.server.database;

import java.sql.Connection;
import java.sql.SQLException;

public class DefaultConnectionProvider implements ConnectionProvider {
    @Override
    public Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }
}