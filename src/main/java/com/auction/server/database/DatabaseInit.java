package com.auction.server.database;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInit {

    public static void init() {

        String usersTable = """
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            password TEXT NOT NULL,
            role TEXT NOT NULL
        );
        """;

        String itemsTable = """
        CREATE TABLE IF NOT EXISTS items (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT,
            start_price REAL NOT NULL,
            current_price REAL NOT NULL,
            seller_id TEXT NOT NULL,
            start_time TEXT,
            end_time TEXT,
            FOREIGN KEY (seller_id) REFERENCES users(id)
        );
        """;

        String bidsTable = """
        CREATE TABLE IF NOT EXISTS bids (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            item_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            bid_price REAL NOT NULL,
            bid_time TEXT,
            FOREIGN KEY (item_id) REFERENCES items(id),
            FOREIGN KEY (user_id) REFERENCES users(id)
        );
        """;

        try (
                Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement()
        ) {

            stmt.execute(usersTable);
            stmt.execute(itemsTable);
            stmt.execute(bidsTable);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}