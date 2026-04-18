package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {

    public static class AutoBidConfig {
        private final String userId;
        private final double maxBid;
        private final double increment;
        private final LocalDateTime registeredAt;

        public AutoBidConfig(String userId, double maxBid, double increment, LocalDateTime registeredAt) {
            this.userId = userId;
            this.maxBid = maxBid;
            this.increment = increment;
            this.registeredAt = registeredAt;
        }

        public String getUserId() {
            return userId;
        }

        public double getMaxBid() {
            return maxBid;
        }

        public double getIncrement() {
            return increment;
        }

        public LocalDateTime getRegisteredAt() {
            return registeredAt;
        }
    }

    public boolean createBid(String itemId, String userId, double bidPrice, String bidTime) {

        String sql = "INSERT INTO bids(item_id,user_id,bid_price,bid_time) VALUES(?,?,?,?)";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            stmt.setDouble(3, bidPrice);
            stmt.setString(4, bidTime);

            stmt.executeUpdate();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean createBid(Connection conn, String itemId, String userId, double bidPrice, String bidTime) {

        String sql = "INSERT INTO bids(item_id,user_id,bid_price,bid_time) VALUES(?,?,?,?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            stmt.setDouble(3, bidPrice);
            stmt.setString(4, bidTime);

            stmt.executeUpdate();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean upsertAutoBid(String itemId, String userId, double maxBid, double increment, String registeredAt) {
        try (Connection conn = DatabaseConnection.connect()) {
            return upsertAutoBid(conn, itemId, userId, maxBid, increment, registeredAt);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean upsertAutoBid(Connection conn, String itemId, String userId, double maxBid, double increment, String registeredAt) {

        String sql = """
                INSERT INTO auto_bids(item_id, user_id, max_bid, increment, registered_at, is_active)
                VALUES(?,?,?,?,?,1)
                ON CONFLICT(item_id, user_id) DO UPDATE SET
                    max_bid = excluded.max_bid,
                    increment = excluded.increment,
                    registered_at = excluded.registered_at,
                    is_active = 1
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            stmt.setDouble(3, maxBid);
            stmt.setDouble(4, increment);
            stmt.setString(5, registeredAt);

            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deactivateAutoBid(String itemId, String userId) {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE item_id = ? AND user_id = ?";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<AutoBidConfig> findActiveAutoBids(Connection conn, String itemId) {
        List<AutoBidConfig> autoBids = new ArrayList<>();

        String sql = """
                SELECT user_id, max_bid, increment, registered_at
                FROM auto_bids
                WHERE item_id = ? AND is_active = 1
                ORDER BY registered_at ASC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    autoBids.add(new AutoBidConfig(
                            rs.getString("user_id"),
                            rs.getDouble("max_bid"),
                            rs.getDouble("increment"),
                            parseDateTime(rs.getString("registered_at"))
                    ));
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }

        return autoBids;
    }

    private LocalDateTime parseDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return LocalDateTime.MIN;
        }

        try {
            return LocalDateTime.parse(rawValue);
        } catch (DateTimeParseException e) {
            return LocalDateTime.MIN;
        }
    }
}