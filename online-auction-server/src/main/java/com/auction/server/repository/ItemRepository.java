package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.server.util.StringUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    private static final ItemRepository instance = new ItemRepository();
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ItemRepository.class.getName());

    private ItemRepository() {
    }

    public static ItemRepository getInstance() {
        return instance;
    }

    private Gson gson = GsonUtil.getInstance();


    public boolean updateCurrentBidder(Connection conn, String itemId,
                                       double newPrice, String newBidderId) {
        String sql = """
                UPDATE items
                SET current_price     = ?,
                    current_bidder_id = ?
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setString(2, newBidderId);
            stmt.setString(3, itemId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }

    private ItemSummary mapRowToItemSummary(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String category = rs.getString("category");
        double currentPrice = rs.getDouble("current_price");
        String thumbnailUrl = null;
        String imagesData = rs.getString("image_path");


        if (imagesData != null && !imagesData.isBlank()) {
            List<String> imagePaths = gson.fromJson(imagesData, new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
            if (imagePaths != null && !imagePaths.isEmpty()) {
                thumbnailUrl = imagePaths.get(0);
            }
        }

        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));

        // 🌟 Kiểm tra xem cột seller_username có tồn tại trong ResultSet của câu SQL này không
        String sellerUsername = null;
        try {
            sellerUsername = rs.getString("seller_username");
            System.out.println("Seller: " + sellerUsername);
        } catch (SQLException e) {
            System.out.println("seller_username column NOT FOUND");
        }

        // 🌟 [FIX] Đọc status từ DB trước (để giữ đúng BANNED), chỉ compute() khi cột status không có trong ResultSet.
        // Nếu dùng AuctionStatus.compute() thẳng thì BANNED sẽ bị overwrite thành ONGOING/UPCOMING/ENDED.
        com.auction.shared.model.enums.AuctionStatus auctionStatus = null;
        try {
            String dbStatus = rs.getString("status");
            if (dbStatus != null && !dbStatus.isBlank()) {
                try {
                    auctionStatus = com.auction.shared.model.enums.AuctionStatus.valueOf(dbStatus.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // status DB không map được sang enum → fallback compute
                }
            }
        } catch (SQLException ignored) {
            // Cột status không tồn tại trong ResultSet của câu SQL này → compute bình thường
        }
        if (auctionStatus == null) {
            auctionStatus = com.auction.shared.model.enums.AuctionStatus.compute(startTime, endTime);
        }

        // 🌟 Gọi Constructor mới có trường sellerUsername
        return new ItemSummary(
                id,
                name,
                category,
                currentPrice,
                thumbnailUrl,
                startTime,
                endTime,
                auctionStatus,
                sellerUsername
        );

    }

    private List<ItemSummary> executeSummaryQuery(String sql, Object... params) {
        List<ItemSummary> summaries = new ArrayList<>();

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemSummary itemSummary = mapRowToItemSummary(rs);
                    summaries.add(itemSummary);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to execute summary query", e);
        }

        return summaries;
    }

    public boolean createItem(Item item) {

        String sql = """
                INSERT INTO items(
                    id,
                    name,
                    description,
                    start_price,
                    current_price,
                    seller_id,
                    start_time,
                    end_time,
                    category,
                    bid_step,
                    image_path,
                    status,
                    create_at,
                    search_name
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            String name = item.getName();
            String search_name = StringUtil.removeAccents(name);

            stmt.setString(1, item.getId());
            stmt.setString(2, name);
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getStartingPrice());
            stmt.setDouble(5, item.getHighestCurrentPrice());
            stmt.setString(6, item.getSellerId());
            stmt.setString(7, item.getStartTime().toString());
            stmt.setString(8, item.getEndTime().toString());
            stmt.setString(9, item.getCategory());
            stmt.setDouble(10, item.getBidStep());
            stmt.setString(11, gson.toJson(item.getImagesPath()));
            stmt.setString(12, item.getStatus());
            stmt.setString(13, item.getCreate_at().toString());
            stmt.setString(14, search_name);

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }

    public boolean updateItem(Item item, String itemId) {
        String sql = """
                UPDATE items SET 
                    name = ?, 
                    description = ?, 
                    start_price = ?, 
                    current_price = ?, 
                    seller_id = ?, 
                    start_time = ?, 
                    end_time = ?, 
                    category = ?, 
                    bid_step = ?, 
                    image_path = ?,
                    status = ?,
                    search_name = ?
                WHERE id = ?
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingPrice());
            stmt.setDouble(4, item.getHighestCurrentPrice());
            stmt.setString(5, item.getSellerId());
            stmt.setString(6, item.getStartTime().toString());
            stmt.setString(7, item.getEndTime().toString());
            stmt.setString(8, item.getCategory());
            stmt.setDouble(9, item.getBidStep());
            stmt.setString(10, gson.toJson(item.getImagesPath()));
            stmt.setString(11, item.getStatus());
            stmt.setString(12, StringUtil.removeAccents(item.getName()));
            stmt.setString(13, itemId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }

    public boolean deleteItem(String id) {
        String sql = """
                DELETE FROM items
                WHERE id = ?
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }

    public Item findById(String itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to execute summary query", e);
        }
        return null;
    }

    public Item findById(Connection conn, String itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to execute summary query", e);
        }
        return null;
    }

    public String getCurrentBidderId(Connection conn, String itemId) throws SQLException {
        String sql = "SELECT current_bidder_id FROM items WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("current_bidder_id");
                }
            }
        }
        return null;
    }

    public List<ItemSummary> findAllBySellerId(String sellerID) {
        String sql = """
                SELECT id,
                       name,
                       category,
                       current_price, 
                       image_path,
                       start_time,
                       end_time
                FROM items 
                WHERE seller_id = ?
                """;

        return executeSummaryQuery(sql, sellerID);
    }

    public List<ItemSummary> searchItems(List<String> keywords, String category, int offset) throws SQLException {
        List<ItemSummary> items = new ArrayList<>();

        boolean filterCategory = category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL");

        StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE status != '" + AuctionStatus.BANNED.name() + "' AND (");
        for (int i = 0; i < keywords.size(); i++) {
            sql.append("LOWER(search_name) LIKE ?");
            if (i < keywords.size() - 1) sql.append(" AND ");
        }
        sql.append(")");
        if (filterCategory) {
            sql.append(" AND LOWER(category) = LOWER(?)");
        }
        sql.append(" ORDER BY id DESC LIMIT 10 OFFSET ?");

        LOGGER.fine(sql.toString());
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())
        ) {
            int paramIndex = 1;
            for (String keyword : keywords) {
                stmt.setString(paramIndex++, "%" + keyword + "%");
            }
            if (filterCategory) {
                stmt.setString(paramIndex++, category);
            }
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToItemSummary(rs));
                }
            }
        }
        return items;
    }

    /**
     * @deprecated Dùng {@link #searchItems(List, String, int)} thay thế.
     */
    @Deprecated
    public List<ItemSummary> searchItems(List<String> keywords, int offset) throws SQLException {
        return searchItems(keywords, null, offset);
    }

    /**
     * Dành cho trang chủ / public: loại bỏ sản phẩm BANNED.
     * [FIX] Áp dụng đúng sortOrder, offset, và category filter.
     *
     * @param category null hoặc "ALL" = không filter, chuỗi khác = filter theo category
     */
    public List<ItemSummary> findAllItems(String sortOrder, int offset, String category) {
        // Whitelist sort order để tránh SQL injection
        String safeSort = switch (sortOrder == null ? "" : sortOrder.trim().toLowerCase()) {
            case "start_time desc"    -> "start_time DESC";
            case "current_price asc"  -> "current_price ASC";
            case "current_price desc" -> "current_price DESC";
            default                   -> "end_time ASC";   // mặc định
        };

        boolean filterCategory = category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL");

        String sql = String.format("""
                SELECT id,
                       name,
                       category,
                       current_price,
                       image_path,
                       start_time,
                       end_time
                FROM items
                WHERE status != '%s'
                %s
                ORDER BY %s
                LIMIT 10 OFFSET ?
                """,
                AuctionStatus.BANNED.name(),
                filterCategory ? "AND LOWER(category) = LOWER(?)" : "",
                safeSort);

        if (filterCategory) {
            return executeSummaryQuery(sql, category, offset);
        }
        return executeSummaryQuery(sql, offset);
    }

    /**
     * @deprecated Dùng {@link #findAllItems(String, int, String)} thay thế.
     */
    @Deprecated
    public List<ItemSummary> findAllItems(String sortOrder, int offset) {
        return findAllItems(sortOrder, offset, null);
    }

    /**
     * [NEW] Dành cho Admin panel: trả về TẤT CẢ sản phẩm kể cả BANNED
     * để admin có thể xem và quản lý.
     * Được gọi từ ItemService.getItems() khi caller = "ADMIN".
     */
    public List<ItemSummary> findAllItemsForAdmin(String sortOrder, int offset) {
        // Whitelist sort order để tránh SQL injection (thêm alias 'i.' vào trước trường sort)
        String safeSort = switch (sortOrder == null ? "" : sortOrder.trim().toLowerCase()) {
            case "start_time desc"    -> "i.start_time DESC";
            case "current_price asc"  -> "i.current_price ASC";
            case "current_price desc" -> "i.current_price DESC";
            default                   -> "i.end_time ASC";
        };

        // 🌟 Thay đổi: Dùng LEFT JOIN để lấy trường username từ bảng users đặt tên alias là seller_username
        // [FIX] Bổ sung i.status vào SELECT để mapRowToItemSummary đọc đúng trạng thái BANNED từ DB
        // thay vì tính lại bằng AuctionStatus.compute() (vốn không biết về BANNED).
        String sql = String.format("""
            SELECT i.id,
                   i.name,
                   i.category,
                   i.current_price,
                   i.image_path,
                   i.start_time,
                   i.end_time,
                   i.status,
                   u.username AS seller_username
            FROM items i
            LEFT JOIN users u ON i.seller_id = u.id
            ORDER BY %s
            LIMIT 10 OFFSET ?
            """, safeSort);

        return executeSummaryQuery(sql, offset);
    }

    public List<String> getImgName(String itemId) {
        String sql = """
                SELECT image_path
                FROM items
                WHERE id = ?
                """;
        List<String> imagePaths = new ArrayList<>();
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String pathsData = rs.getString("image_path");

                    if (pathsData != null && !pathsData.isEmpty()) {
                        imagePaths = gson.fromJson(pathsData, new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to execute summary query", e);
        }
        return imagePaths;
    }


    private Item mapRow(ResultSet rs) throws Exception {
        String pathsData = rs.getString("image_path");
        List<String> imagePaths = new ArrayList<>();
        if (pathsData != null && !pathsData.isEmpty()) {
            try {
                imagePaths = gson.fromJson(pathsData, new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
                if (imagePaths == null) {
                    imagePaths = new ArrayList<>();
                }
            } catch (Exception e) {
                imagePaths.add(pathsData);
            }
        }

        Item item = new Item(
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("start_price"),
                rs.getDouble("current_price"),
                LocalDateTime.parse(rs.getString("start_time")),
                LocalDateTime.parse(rs.getString("end_time")),
                rs.getString("seller_id"),
                rs.getString("category"),
                rs.getDouble("bid_step"),
                imagePaths
        );
        item.setId(rs.getString("id"));
        item.setCurrentTopPLayerId(rs.getString("current_bidder_id"));
        String dbStatus = rs.getString("status");
        if (dbStatus != null) item.setStatus(dbStatus);
        return item;
    }

    // Đánh dấu item là ENDED (gọi khi thanh toán kết thúc đấu giá)
    public boolean markEnded(Connection conn, String itemId) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AuctionStatus.ENDED.name());
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }


    public void updateCurrentPrice(String itemId, double newPrice) {

        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setDouble(1, newPrice);
            stmt.setString(2, itemId);

            stmt.executeUpdate();

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to execute summary query", e);
        }
    }

    public boolean updateCurrentPrice(Connection conn, String itemId, double newPrice) {

        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setString(2, itemId);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }

    public boolean extendEndTime(Connection conn, String itemId, LocalDateTime newEndTime) {
        String sql = "UPDATE items SET end_time = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newEndTime.toString());
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current bidder", e);
            return false;
        }
    }

    public List<String> updateStatus() {
        List<String> updatedId = new ArrayList<>();

        String selectAboutToEndSql =
                "SELECT id FROM items WHERE status = ? AND datetime(end_time)   <= datetime('now','localtime')";
        String selectAboutToLiveSql =
                "SELECT id FROM items WHERE status = ? AND datetime(start_time) <= datetime('now','localtime') AND datetime(end_time) > datetime('now','localtime')";
        String updateEndedSql =
                "UPDATE items SET status = ? WHERE status = ? AND datetime(end_time)   <= datetime('now','localtime')";
        String updateOngoingSql =
                "UPDATE items SET status = ? WHERE status = ? AND datetime(start_time) <= datetime('now','localtime') AND datetime(end_time) > datetime('now','localtime')";

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(selectAboutToEndSql)) {
                ps.setString(1, AuctionStatus.ONGOING.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) updatedId.add(rs.getString("id"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateEndedSql)) {
                ps.setString(1, AuctionStatus.ENDED.name());
                ps.setString(2, AuctionStatus.ONGOING.name());
                int rows = ps.executeUpdate();
                logStatusUpdate("ONGOING->ENDED", rows);
            }

            try (PreparedStatement ps = conn.prepareStatement(selectAboutToLiveSql)) {
                ps.setString(1, AuctionStatus.UPCOMING.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) updatedId.add(rs.getString("id"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateOngoingSql)) {
                ps.setString(1, AuctionStatus.ONGOING.name());
                ps.setString(2, AuctionStatus.UPCOMING.name());
                int rows = ps.executeUpdate();
                logStatusUpdate("UPCOMING->ONGOING", rows);
            }

        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update item statuses", e);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Unexpected error while updating item statuses", e);
        }
        return updatedId;
    }

    private void logStatusUpdate(String transition, int rows) {
        String message = String.format("[updateStatus] %s: %d row(s) updated", transition, rows);
        if (rows > 0) {
            LOGGER.info(message);
        } else {
            LOGGER.fine(message);
        }
    }

    public double getUserLastBid(String itemId, String userId) {
        String sql = "SELECT MAX(bid_price) AS highest_bid FROM bids WHERE item_id = ? AND user_id = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("highest_bid");
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to execute summary query", e);
        }
        return 0.0;
    }

    public boolean updateStatus(String itemId, AuctionStatus status) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, status.name());
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update item status", e);
            return false;
        }
    }

    public boolean updateStatus(Connection conn, String itemId, AuctionStatus status) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update item status", e);
            return false;
        }
    }
}