package com.auction.server.database;

import com.auction.server.config.AppConfig;
import com.auction.shared.constant.ItemStatusConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Utility tool to reset the database and seed fresh consistent development/testing data.
 * Does not run automatically on normal server startup.
 */
public class DatabaseSeed {

    public static void main(String[] args) {
        boolean force = false;
        for (String arg : args) {
            if ("--yes".equalsIgnoreCase(arg) || "-y".equalsIgnoreCase(arg) || "force".equalsIgnoreCase(arg)) {
                force = true;
            }
        }

        if (!force) {
            System.out.println("⚠️ WARNING: This will delete all existing data in the database!");
            System.out.print("Are you sure you want to proceed? (y/N): ");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String input = reader.readLine();
                if (input == null || (!input.trim().equalsIgnoreCase("y") && !input.trim().equalsIgnoreCase("yes"))) {
                    System.out.println("❌ Database reset cancelled.");
                    System.exit(0);
                }
            } catch (Exception e) {
                System.out.println("❌ Error reading confirmation. Aborting.");
                System.exit(1);
            }
        }

        try {
            AppConfig.initFolders();
            DatabaseManager.init();
            DatabaseInit.init();

            resetAndSeed();

            System.out.println("\n🎉 Database reset & seed completed successfully!");
        } catch (Exception e) {
            System.err.println("❌ Reset failed with error:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            DatabaseManager.shutdown();
        }
    }

    public static void resetAndSeed() throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                System.out.println("1. Cleaning old development/testing data...");
                // Respecting foreign key dependency order
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM bids")) { stmt.executeUpdate(); }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM auto_bids")) { stmt.executeUpdate(); }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM wallet_transactions")) { stmt.executeUpdate(); }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM items")) { stmt.executeUpdate(); }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM users")) { stmt.executeUpdate(); }

                System.out.println("2. Creating seed accounts...");
                // Roles are strictly ADMIN and USER. Passwords are all 123.
                insertUser(conn, "admin", "123", "ADMIN", "admin@test.local", 10000.0);
                insertUser(conn, "seller1", "123", "USER", "seller1@test.local", 10000.0);
                insertUser(conn, "test1", "123", "USER", "test1@test.local", 10000.0);
                insertUser(conn, "test2", "123", "USER", "test2@test.local", 10000.0);
                insertUser(conn, "test3", "123", "USER", "test3@test.local", 10000.0);

                System.out.println("3. Creating seed auction items...");
                LocalDateTime now = LocalDateTime.now();

                // A. Active Auction (laptop-auction)
                insertItem(conn,
                        "laptop-auction",
                        "Test Laptop Auction",
                        "Development seed auction for testing wallet lock/release and auto settlement",
                        10.0,
                        10.0,
                        getStableUserId("seller1"),
                        now.minusMinutes(5),
                        now.plusMinutes(30),
                        "electronics",
                        5.0,
                        ItemStatusConstants.ONGOING
                );

                // B. Upcoming Auction
                insertItem(conn,
                        "upcoming-camera",
                        "Upcoming Camera Auction",
                        "Upcoming auction that starts in 10 minutes",
                        100.0,
                        100.0,
                        getStableUserId("seller1"),
                        now.plusMinutes(10),
                        now.plusMinutes(40),
                        "electronics",
                        10.0,
                        ItemStatusConstants.UPCOMING
                );

                // C. Ended Auction with no bids
                insertItem(conn,
                        "ended-book",
                        "Ended Book Auction",
                        "Ended auction with no active bids",
                        20.0,
                        20.0,
                        getStableUserId("seller1"),
                        now.minusMinutes(60),
                        now.minusMinutes(5),
                        "books",
                        2.0,
                        ItemStatusConstants.ENDED
                );

                conn.commit();
                System.out.println("✅ All seed data committed to the database.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static String getStableUserId(String username) {
        return UUID.nameUUIDFromBytes(username.getBytes()).toString();
    }

    private static void insertUser(Connection conn, String username, String password, String role, String email, double balance) throws Exception {
        String sql = """
                INSERT INTO users(id, username, password, role, isVerify, email, balance, frozen_balance)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getStableUserId(username));
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.setInt(5, 1); // isVerify = true
            stmt.setString(6, email);
            stmt.setDouble(7, balance);
            stmt.setDouble(8, 0.0); // frozen balance always starts at 0
            stmt.executeUpdate();
        }
    }

    private static void insertItem(Connection conn,
                                   String labelId,
                                   String name,
                                   String description,
                                   double startPrice,
                                   double currentPrice,
                                   String sellerId,
                                   LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   String category,
                                   double bidStep,
                                   String status) throws Exception {
        String sql = """
                INSERT INTO items(id, name, description, start_price, current_price, seller_id, start_time, end_time,
                                  category, bid_step, image_path, create_at, top_player_id, search_name, status, current_bidder_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefixId(labelId));
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setDouble(4, startPrice);
            stmt.setDouble(5, currentPrice);
            stmt.setString(6, sellerId);
            stmt.setString(7, startTime.toString());
            stmt.setString(8, endTime.toString());
            stmt.setString(9, category);
            stmt.setDouble(10, bidStep);
            stmt.setString(11, "[]");
            stmt.setString(12, LocalDateTime.now().toString());
            stmt.setString(13, null); // top_player_id is null
            stmt.setString(14, name.toLowerCase());
            stmt.setString(15, status);
            stmt.setString(16, null); // current_bidder_id starts as null
            stmt.executeUpdate();
        }
    }

    private static String prefixId(String label) {
        return "seed-item-" + label;
    }
}
