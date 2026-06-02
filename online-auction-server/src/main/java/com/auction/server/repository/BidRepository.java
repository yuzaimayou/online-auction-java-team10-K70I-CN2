package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.MyBidSummary;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository chịu trách nhiệm DUY NHẤT là đọc/ghi dữ liệu bid và auto-bid.
 * KHÔNG chứa business logic, KHÔNG validate dữ liệu nghiệp vụ.
 *
 * Nguyên tắc sử dụng Connection:
 *  - Các method nhận Connection từ ngoài vào → nằm trong transaction của caller.
 *  - Các method KHÔNG nhận Connection → tự mở/đóng connection riêng,
 *    CHỈ dùng cho các thao tác đứng độc lập (query trạng thái, cancel...).
 */
public class BidRepository {

    private static final Logger LOGGER = Logger.getLogger(BidRepository.class.getName());
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_START_TIME = "start_time";
    private static final String COLUMN_END_TIME = "end_time";
    private static final String COLUMN_CURRENT_PRICE = "current_price";
    private final Gson gson = GsonUtil.getInstance();

    // Inner class

    /**
     * Snapshot cấu hình auto-bid được đọc từ DB.
     * Bất biến sau khi tạo — không có setter.
     */
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

        public String getUserId()  { return userId; }
        public double getMaxBid() { return maxBid; }
        public double getIncrement() { return increment; }
        public LocalDateTime getRegisteredAt(){ return registeredAt; }
    }

    // Bid
    /**
     * Ghi một bid mới — dùng connection riêng (ngoài transaction).
     * Chỉ dùng cho các luồng không cần transaction bao quanh.
     */
    public boolean createBid(String itemId, String userId, double bidPrice, String bidTime) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return createBid(conn, itemId, userId, bidPrice, bidTime);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create bid (standalone)", e);
            return false;
        }
    }

    /**
     * Ghi một bid mới trong transaction của caller.
     * Ném Exception ra ngoài để caller có thể rollback đúng cách.
     */
    public boolean createBid(Connection conn, String itemId, String userId,
                             double bidPrice, String bidTime) throws Exception {
        String sql = "INSERT INTO bids(item_id, user_id, bid_price, bid_time) VALUES(?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            stmt.setDouble(3, bidPrice);
            stmt.setString(4, bidTime);
            stmt.executeUpdate();
            return true;
        }
        // Không catch ở đây — để caller bắt và rollback
    }

    /**
     * Tìm user đặt bid gần nhất cho một item.
     * Dùng trong transaction của caller để tránh dirty read.
     */
    public String findLastBidder(Connection conn, String itemId) throws Exception {
        String sql = """
                SELECT user_id
                FROM bids
                WHERE item_id = ?
                ORDER BY bid_time DESC, id DESC
                LIMIT 1
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("user_id") : null;
            }
        }
    }

    // Auto-bid — upsert / deactivate

    /**
     * Upsert auto-bid config — dùng connection riêng.
     */
    public boolean upsertAutoBid(String itemId, String userId,
                                 double maxBid, double increment, String registeredAt) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return upsertAutoBid(conn, itemId, userId, maxBid, increment, registeredAt);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert auto bid (standalone)", e);
            return false;
        }
    }

    /**
     * Upsert auto-bid config trong transaction của caller.
     * Ném Exception ra ngoài để caller rollback đúng cách.
     */
    public boolean upsertAutoBid(Connection conn, String itemId, String userId,
                                 double maxBid, double increment, String registeredAt) throws Exception {
        String sql = """
                INSERT INTO auto_bids(item_id, user_id, max_bid, increment, registered_at, is_active)
                VALUES(?, ?, ?, ?, ?, 1)
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
        }
    }

    /**
     * Deactivate auto-bid cho một user cụ thể — dùng connection riêng.
     * Dùng cho luồng cancel độc lập (không cần transaction bao quanh).
     */
    public boolean deactivateAutoBidIfPresent(String itemId, String userId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return deactivateAutoBidIfPresent(conn, itemId, userId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deactivate auto bid if present (standalone)", e);
            return false;
        }
    }

    /**
     * Deactivate auto-bid cho một user trong transaction của caller.
     */
    public boolean deactivateAutoBidIfPresent(Connection conn, String itemId, String userId) throws Exception {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE item_id = ? AND user_id = ? AND is_active = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            int rows = stmt.executeUpdate();
            LOGGER.info(String.format(
                    "[AUTO_BID_CANCEL][DB_DEACTIVATE] time=%s itemId=%s userId=%s rowsUpdated=%d",
                    LocalDateTime.now(), itemId, userId, rows));
            return true;
        }
    }

    /**
     * Deactivate toàn bộ auto-bid của một item trong transaction của caller.
     * Dùng khi auction kết thúc.
     */
    public boolean deactivateAllAutoBids(Connection conn, String itemId) throws Exception {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE item_id = ? AND is_active = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            int rows = stmt.executeUpdate();
            LOGGER.info(String.format(
                    "[AUTO_BID_CANCEL_ALL][DB_DEACTIVATE] time=%s itemId=%s rowsUpdated=%d",
                    LocalDateTime.now(), itemId, rows));
            return true;
        }
    }

    /**
     * Deactivate toàn bộ auto-bid của một user trong transaction của caller.
     * Dùng khi user bị ban.
     */
    public boolean deactivateAllAutoBidsForUser(Connection conn, String userId) throws Exception {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE user_id = ? AND is_active = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            int rows = stmt.executeUpdate();
            LOGGER.info(String.format(
                    "[AUTO_BID_CANCEL_ALL_USER][DB_DEACTIVATE] time=%s userId=%s rowsUpdated=%d",
                    LocalDateTime.now(), userId, rows));
            return true;
        }
    }
    // Auto-bid — query
    /**
     * Lấy danh sách tất cả auto-bid đang active cho một item.
     * Kết quả được sort theo registered_at ASC (người đăng sớm hơn được ưu tiên).
     *
     * Ném Exception ra ngoài — caller (BidService) sẽ rollback nếu cần.
     */
    public List<AutoBidConfig> findActiveAutoBids(Connection conn, String itemId) throws Exception {
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
                    String rawDate = rs.getString("registered_at");
                    LocalDateTime registeredAt = parseDateTime(itemId, rs.getString("user_id"), rawDate);

                    // FIX: Bỏ qua record có registered_at không parse được thay vì
                    // cho registeredAt = MIN rồi đẩy lên đầu hàng ưu tiên sai.
                    if (registeredAt == null) {
                        LOGGER.warning(String.format(
                                "[AUTO_BID][SKIP_INVALID_DATE] itemId=%s userId=%s rawDate=%s",
                                itemId, rs.getString("user_id"), rawDate));
                        continue;
                    }

                    autoBids.add(new AutoBidConfig(
                            rs.getString("user_id"),
                            rs.getDouble("max_bid"),
                            rs.getDouble("increment"),
                            registeredAt
                    ));
                }
            }

            LOGGER.fine(String.format(
                    "[AUTO_BID_ROUND][ACTIVE_CONFIGS] time=%s itemId=%s count=%d users=%s",
                    LocalDateTime.now(),
                    itemId,
                    autoBids.size(),
                    autoBids.stream().map(AutoBidConfig::getUserId).toList()));
        }

        return autoBids;
    }

    /**
     * Tìm auto-bid đang active của một user cho một item cụ thể.
     * Dùng connection riêng — chỉ để query trạng thái, không cần transaction.
     */
    public AutoBidPayload findActiveAutoBid(String itemId, String userId) {
        String sql = """
                SELECT item_id, user_id, max_bid, increment, is_active
                FROM auto_bids
                WHERE item_id = ? AND user_id = ? AND is_active = 1
                LIMIT 1
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AutoBidPayload(
                            rs.getString("item_id"),
                            rs.getString("user_id"),
                            rs.getDouble("max_bid"),
                            rs.getDouble("increment"),
                            rs.getInt("is_active") == 1
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find active auto bid", e);
        }

        return null;
    }

    public List<MyBidSummary> findMyBids(String userId) {
        String sql = """
        SELECT
            i.id,
            i.name,
            i.image_path,
            i.current_price,
            i.status,
            i.start_time,
            i.end_time,
            MAX(b.bid_price)              AS my_highest_bid,
            (i.current_bidder_id = ?)     AS is_winner
        FROM bids b
        JOIN items i ON b.item_id = i.id
        WHERE b.user_id = ?
        GROUP BY i.id
        ORDER BY i.end_time DESC
        """;

        List<MyBidSummary> result = new ArrayList<>();
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, userId);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String thumbnailUrl = extractThumbnail(rs.getString(COLUMN_IMAGE_PATH));

                    AuctionStatus status = null;
                    try {
                        String dbStatus = rs.getString(COLUMN_STATUS);
                        if (dbStatus != null) status = AuctionStatus.valueOf(dbStatus.toUpperCase());
                    } catch (Exception ignored) {}

                    LocalDateTime startTime = LocalDateTime.parse(rs.getString(COLUMN_START_TIME));
                    LocalDateTime endTime   = LocalDateTime.parse(rs.getString(COLUMN_END_TIME));
                    if (status == null) {
                        status = AuctionStatus.compute(startTime, endTime);
                    }

                    result.add(new MyBidSummary(
                            rs.getString(COLUMN_ID),
                            rs.getString(COLUMN_NAME),
                            thumbnailUrl,
                            rs.getDouble(COLUMN_CURRENT_PRICE),
                            rs.getDouble("my_highest_bid"),
                            rs.getInt("is_winner") == 1,
                            status,
                            endTime
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "findMyBids failed", e);
        }
        return result;
    }

    // Helper

    private String extractThumbnail(String imagesData) {
        if (imagesData == null || imagesData.isBlank()) {
            return null;
        }

        try {
            List<String> imagePaths = gson.fromJson(imagesData, new com.google.gson.reflect.TypeToken<List<String>>() {
            }.getType());
            if (imagePaths != null && !imagePaths.isEmpty()) {
                return imagePaths.get(0);
            }
            return null;
        } catch (Exception e) {
            return imagesData;
        }
    }

    /**
     * Parse datetime string từ DB.
     *
     * FIX: Trả về null thay vì LocalDateTime.MIN khi parse thất bại.
     * Caller sẽ bỏ qua record này thay vì cho nó lên đầu danh sách ưu tiên.
     */
    private LocalDateTime parseDateTime(String itemId, String userId, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(rawValue);
        } catch (DateTimeParseException e) {
            LOGGER.warning(String.format(
                    "[AUTO_BID][PARSE_DATE_FAIL] itemId=%s userId=%s rawValue=%s",
                    itemId, userId, rawValue));
            return null;
        }
    }
}
