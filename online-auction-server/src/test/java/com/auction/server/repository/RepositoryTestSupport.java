package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;

abstract class RepositoryTestSupport {

    private Path dbPath;

    @BeforeEach
    void setUpRepositoryDb() throws Exception {
        Path rootDir = Paths.get(System.getProperty("user.dir"), "target", "repository-tests", Long.toString(System.nanoTime()));
        Files.createDirectories(rootDir);
        System.setProperty("auth.dir", rootDir.toString());
        dbPath = rootDir.resolve("dataBase").resolve("auction.db");
        Files.createDirectories(dbPath.getParent());
        Files.createFile(dbPath);
        createSchema(dbPath);
        DatabaseManager.init();
    }

    @AfterEach
    void tearDownRepositoryDb() {
        DatabaseManager.shutdown();
        System.clearProperty("auth.dir");
        if (dbPath != null) {
            try {
                Files.walk(dbPath.getParent().getParent())
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception ignored) {
            }
        }
    }

    protected Connection openConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }


    private void createSchema(Path dbPath) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("PRAGMA foreign_keys = ON");

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        password TEXT NOT NULL,
                        role TEXT,
                        email TEXT,
                        balance REAL DEFAULT 0,
                        frozen_balance REAL DEFAULT 0,
                        isVerify INTEGER DEFAULT 0,
                        rating REAL DEFAULT 5
                    )
                    """);

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS items (
                        id TEXT PRIMARY KEY,
                        name TEXT,
                        description TEXT,
                        start_price REAL,
                        current_price REAL,
                        seller_id TEXT,
                        start_time TEXT,
                        end_time TEXT,
                        category TEXT,
                        bid_step REAL,
                        image_path TEXT,
                        status TEXT,
                        create_at TEXT,
                        search_name TEXT,
                        current_bidder_id TEXT
                    )
                    """);

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bids (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id TEXT,
                        user_id TEXT,
                        bid_price REAL,
                        bid_time TEXT
                    )
                    """);

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS auto_bids (
                        item_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        max_bid REAL,
                        increment REAL,
                        registered_at TEXT,
                        is_active INTEGER DEFAULT 1,
                        PRIMARY KEY (item_id, user_id)
                    )
                    """);

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS wallet_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id TEXT,
                        type TEXT,
                        amount REAL,
                        balance_before REAL,
                        balance_after REAL,
                        reference_id TEXT,
                        created_at TEXT
                    )
                    """);
        }
    }
}

