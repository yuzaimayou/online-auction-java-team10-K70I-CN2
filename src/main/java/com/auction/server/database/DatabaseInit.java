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
            category TEXT NOT NULL DEFAULT 'other',
            bid_step REAL NOT NULL DEFAULT 1,
            max_price REAL NOT NULL DEFAULT 0,
            min_price REAL NOT NULL DEFAULT 0,
            image_path TEXT NOT NULL DEFAULT '',
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

        String autoBidsTable = """
        CREATE TABLE IF NOT EXISTS auto_bids (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            item_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            max_bid REAL NOT NULL,
            increment REAL NOT NULL,
            registered_at TEXT NOT NULL,
            is_active INTEGER NOT NULL DEFAULT 1,
            UNIQUE (item_id, user_id),
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
            stmt.execute(autoBidsTable);
            migrateItemsTable(stmt);
            migrateAutoBidsTable(stmt);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void migrateItemsTable(Statement stmt) {
        // Keep backward compatibility with databases created before new item columns existed.
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN category TEXT NOT NULL DEFAULT 'other'");
        } catch (Exception ignored) {
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN bid_step REAL NOT NULL DEFAULT 1");
        } catch (Exception ignored) {
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN max_price REAL NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN min_price REAL NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN image_path TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {
        }
    }

    private static void migrateAutoBidsTable(Statement stmt) {
        // Keep backward compatibility for DBs that had partial auto-bid schema.
        try {
            stmt.execute("ALTER TABLE auto_bids ADD COLUMN registered_at TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {
        }
        try {
            stmt.execute("ALTER TABLE auto_bids ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1");
        } catch (Exception ignored) {
        }
    }
}