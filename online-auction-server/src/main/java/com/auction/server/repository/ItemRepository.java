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
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(ItemRepository.class.getName());
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_START_TIME = "start_time";
    private static final String COLUMN_END_TIME = "end_time";
    private static final String COLUMN_CURRENT_PRICE = "current_price";
    private static final String COLUMN_SELLER_USERNAME = "seller_username";

    // singleton
    private ItemRepository() {
    }

    public static ItemRepository getInstance() {
        return instance;
    }

    private Gson gson = GsonUtil.getInstance();

    // cập nhật giá hiện tại và người trả giá hiện tại
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

    // ánh xạ dữ liệu từ ResultSet sang ItemSummary
    private ItemSummary mapRowToItemSummary(ResultSet rs) throws SQLException {
        String id = rs.getString(COLUMN_ID);
        String name = rs.getString(COLUMN_NAME);
        String category = rs.getString("category");
        double currentPrice = rs.getDouble(COLUMN_CURRENT_PRICE);
        String imagesData = rs.getString(COLUMN_IMAGE_PATH);
        String thumbnailUrl = extractThumbnail(imagesData);

        LocalDateTime startTime = LocalDateTime.parse(rs.getString(COLUMN_START_TIME));
        LocalDateTime endTime = LocalDateTime.parse(rs.getString(COLUMN_END_TIME));

        String sellerUsername = readOptionalString(rs, COLUMN_SELLER_USERNAME);
        if (sellerUsername != null) {
            LOGGER.fine("Seller: " + sellerUsername);
        }

        AuctionStatus auctionStatus = resolveSummaryStatus(rs, startTime, endTime);

        return new ItemSummary(
                id,
                name,
                category,
                currentPrice,
                thumbnailUrl,
                startTime,
                endTime,
                auctionStatus,
                sellerUsername);

    }

    private List<String> parseImagePaths(String pathsData) {
        if (pathsData == null || pathsData.isBlank()) {
            return new ArrayList<>();
        }

        List<String> imagePaths = gson.fromJson(pathsData, new com.google.gson.reflect.TypeToken<List<String>>() {
        }.getType());
        return imagePaths == null ? new ArrayList<>() : imagePaths;
    }

    private String extractThumbnail(String imagesData) {
        if (imagesData == null || imagesData.isBlank()) {
            return null;
        }

        try {
            List<String> imagePaths = parseImagePaths(imagesData);
            if (!imagePaths.isEmpty()) {
                return imagePaths.get(0);
            }
            return null;
        } catch (Exception e) {
            return imagesData;
        }
    }

    private AuctionStatus resolveSummaryStatus(ResultSet rs, LocalDateTime startTime, LocalDateTime endTime) {
        String dbStatus = readOptionalString(rs, COLUMN_STATUS);
        if (dbStatus != null && !dbStatus.isBlank()) {
            try {
                return AuctionStatus.valueOf(dbStatus.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall back to computed status when DB value does not map to enum.
            }
        }
        return AuctionStatus.compute(startTime, endTime);
    }

    private String readOptionalString(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            LOGGER.fine(columnName + " column NOT FOUND");
            return null;
        }
    }

    private boolean executeBooleanUpdate(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                setUpdateParam(stmt, i + 1, params[i]);
            }
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void setUpdateParam(PreparedStatement stmt, int index, Object param) throws SQLException {
        if (param instanceof String value) {
            stmt.setString(index, value);
        } else if (param instanceof Double value) {
            stmt.setDouble(index, value);
        } else {
            stmt.setObject(index, param);
        }
    }

    private List<ItemSummary> executeSummaryQuery(String sql, Object... params) {
        List<ItemSummary> summaries = new ArrayList<>();

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            stmt.setString(12, item.getStatus().name());
            stmt.setString(13, item.getCreate_at().toString());
            stmt.setString(14, search_name);

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to create item", e);
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
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            stmt.setString(11, item.getStatus().name());
            stmt.setString(12, StringUtil.removeAccents(item.getName()));
            stmt.setString(13, itemId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update item", e);
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
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to delete item", e);
            return false;
        }
    }

    public Item findById(String itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to find item by id", e);
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
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to find item by id", e);
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

    public ItemSummary findItemSummaryById(String itemId) {
        String sql = """
                SELECT id,
                       name,
                       category,
                       current_price,
                       image_path,
                       start_time,
                       end_time
                FROM items
                WHERE id = ?
                """;
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItemSummary(rs);
                } else {
                    throw new SQLException("Item not found with id: " + itemId);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to find item summary by id", e);
            return null;
        }
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

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM items WHERE status != '" + AuctionStatus.BANNED.name() + "' AND (");
        for (int i = 0; i < keywords.size(); i++) {
            sql.append("LOWER(search_name) LIKE ?");
            if (i < keywords.size() - 1)
                sql.append(" AND ");
        }
        sql.append(")");
        if (filterCategory) {
            sql.append(" AND LOWER(category) = LOWER(?)");
        }
        sql.append(" ORDER BY id DESC LIMIT 10 OFFSET ?");

        LOGGER.fine(sql.toString());
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
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
     * @param category null hoặc "ALL" = không filter, chuỗi khác = filter theo
     *                 category
     */
    public List<ItemSummary> findAllItems(String sortOrder, int offset, String category) {
        // Whitelist sort order để tránh SQL injection
        String safeSort = switch (sortOrder == null ? "" : sortOrder.trim().toLowerCase()) {
            case "start_time desc" -> "start_time DESC";
            case "current_price asc" -> "current_price ASC";
            case "current_price desc" -> "current_price DESC";
            default -> "end_time ASC"; // mặc định
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
        String safeSort = switch (sortOrder == null ? "" : sortOrder.trim().toLowerCase()) {
            case "start_time desc" -> "i.start_time DESC";
            case "current_price asc" -> "i.current_price ASC";
            case "current_price desc" -> "i.current_price DESC";
            default -> "i.end_time ASC";
        };

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
                PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String pathsData = rs.getString(COLUMN_IMAGE_PATH);

                    try {
                        imagePaths = parseImagePaths(pathsData);
                    } catch (Exception e) {
                        if (pathsData != null && !pathsData.isBlank()) {
                            imagePaths.add(pathsData);
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Failed to read image paths for item " + itemId, e);
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to get image names", e);
        }
        return imagePaths;
    }

    private Item mapRow(ResultSet rs) throws Exception {
        String pathsData = rs.getString(COLUMN_IMAGE_PATH);
        List<String> imagePaths = new ArrayList<>();
        if (pathsData != null && !pathsData.isEmpty()) {
            try {
                imagePaths = parseImagePaths(pathsData);
            } catch (Exception e) {
                imagePaths.add(pathsData);
            }
        }

        Item item = new Item(
                rs.getString(COLUMN_NAME),
                rs.getString("description"),
                rs.getDouble("start_price"),
                rs.getDouble(COLUMN_CURRENT_PRICE),
                LocalDateTime.parse(rs.getString(COLUMN_START_TIME)),
                LocalDateTime.parse(rs.getString(COLUMN_END_TIME)),
                rs.getString("seller_id"),
                rs.getString("category"),
                rs.getDouble("bid_step"),
                imagePaths);
        item.setId(rs.getString(COLUMN_ID));
        item.setCurrentBidderId(rs.getString("current_bidder_id"));
        String dbStatus = rs.getString(COLUMN_STATUS);
        if (dbStatus != null)
            item.setStatus(AuctionStatus.fromString(dbStatus));
        return item;
    }

    // Đánh dấu item là ENDED (gọi khi thanh toán kết thúc đấu giá)
    public boolean markEnded(Connection conn, String itemId) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try {
            return executeBooleanUpdate(conn, sql, AuctionStatus.ENDED.name(), itemId);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to mark item ended", e);
            return false;
        }
    }

    public void updateCurrentPrice(String itemId, double newPrice) {

        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setString(2, itemId);

            stmt.executeUpdate();

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current price", e);
        }
    }

    public boolean updateCurrentPrice(Connection conn, String itemId, double newPrice) {

        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try {
            return executeBooleanUpdate(conn, sql, newPrice, itemId);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update current price", e);
            return false;
        }
    }

    public boolean extendEndTime(Connection conn, String itemId, LocalDateTime newEndTime) {
        String sql = "UPDATE items SET end_time = ? WHERE id = ?";
        try {
            return executeBooleanUpdate(conn, sql, newEndTime.toString(), itemId);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to extend item end time", e);
            return false;
        }
    }

    public List<String> updateStatus() {
        List<String> updatedId = new ArrayList<>();

        String selectAboutToEndSql = "SELECT id FROM items WHERE status = ? AND datetime(end_time)   <= datetime('now','localtime')";
        String selectAboutToLiveSql = "SELECT id FROM items WHERE status = ? AND datetime(start_time) <= datetime('now','localtime') AND datetime(end_time) > datetime('now','localtime')";
        String updateEndedSql = "UPDATE items SET status = ? WHERE status = ? AND datetime(end_time)   <= datetime('now','localtime')";
        String updateOngoingSql = "UPDATE items SET status = ? WHERE status = ? AND datetime(start_time) <= datetime('now','localtime') AND datetime(end_time) > datetime('now','localtime')";

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(selectAboutToEndSql)) {
                ps.setString(1, AuctionStatus.ONGOING.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        updatedId.add(rs.getString("id"));
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
                    while (rs.next())
                        updatedId.add(rs.getString("id"));
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
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        try {
            return executeBooleanUpdate(conn, sql, status.name(), itemId);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to update item status", e);
            return false;
        }
    }

}
