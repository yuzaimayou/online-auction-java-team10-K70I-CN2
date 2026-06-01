package com.auction.server.database;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseInit {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInit.class.getName());

    public static void init() {

        String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    isVerify BOOLEAN NOT NULL DEFAULT 0,
                    email TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Active',
                    balance REAL NOT NULL DEFAULT 0,
                    frozen_balance REAL NOT NULL DEFAULT 0
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
                    image_path TEXT NOT NULL DEFAULT '',
                    create_at TEXT,
                    search_name TEXT,
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

        // Wallet transaction ledger — immutable audit log.
        // Types: DEPOSIT, FREEZE_BID, UNFREEZE_BID, AUCTION_PAYMENT
        String walletTransactionsTable = """
                CREATE TABLE IF NOT EXISTS wallet_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    balance_before REAL NOT NULL,
                    balance_after REAL NOT NULL,
                    reference_id TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement()
        ) {
            stmt.execute("PRAGMA journal_mode=WAL;");  // Better concurrency for SQLite
            stmt.execute(usersTable);
            stmt.execute(itemsTable);
            stmt.execute(bidsTable);
            stmt.execute(autoBidsTable);
            stmt.execute(walletTransactionsTable);

            migrateUsersTable(stmt);
            migrateItemsTable(stmt);
            migrateAutoBidsTable(stmt);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database initialization failed", e);
        }
    }

    // ---- Migration helpers (safe ALTER TABLE — ignored if column already exists) ----

    private static void migrateUsersTable(Statement stmt) {
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN email TEXT");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: users.email - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN isVerify INTEGER NOT NULL DEFAULT 0");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: users.isVerify - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN status TEXT NOT NULL DEFAULT 'Active'");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: users.status - " + e.getMessage());
        }
        // Wallet columns
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN balance REAL NOT NULL DEFAULT 0");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: users.balance - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN frozen_balance REAL NOT NULL DEFAULT 0");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: users.frozen_balance - " + e.getMessage());
        }
    }

    private static void migrateItemsTable(Statement stmt) {
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN category TEXT NOT NULL DEFAULT 'other'");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: items.category - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN bid_step REAL NOT NULL DEFAULT 1");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: items.bid_step - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN image_path TEXT NOT NULL DEFAULT ''");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: items.image_path - " + e.getMessage());
        }
        // [FIX BUG #4] Trước đây default là 'PENDING' — không tồn tại trong AuctionStatus enum.
        // AuctionStatus.valueOf("PENDING") ném IllegalArgumentException.
        // Nay đổi thành 'UPCOMING' để khớp với AuctionStatus.UPCOMING.
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN status TEXT NOT NULL DEFAULT 'UPCOMING'");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: items.status - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN create_at TEXT");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: items.create_at - " + e.getMessage());
        }
        // Current highest bidder — NULL means no bids yet
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN current_bidder_id TEXT");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: items.current_bidder_id - " + e.getMessage());
        }
    }

    private static void migrateAutoBidsTable(Statement stmt) {
        try {
            stmt.execute("ALTER TABLE auto_bids ADD COLUMN registered_at TEXT NOT NULL DEFAULT ''");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: auto_bids.registered_at - " + e.getMessage());
        }
        try {
            stmt.execute("ALTER TABLE auto_bids ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1");
        } catch (Exception e) {
            LOGGER.fine("Migration skipped: auto_bids.is_active - " + e.getMessage());
        }
    }
}
