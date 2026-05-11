package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
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
    private static ItemRepository instance;

    private ItemRepository() {
    }

    public static ItemRepository getInstance() {
        if (instance == null) {
            instance = new ItemRepository();
        }
        return instance;
    }

    private Gson gson = new GsonUtil().getInstance();


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
            e.printStackTrace();
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

        List<String> imagePaths = gson.fromJson(imagesData, List.class);
        if (imagePaths != null && !imagePaths.isEmpty()) {
            thumbnailUrl = imagePaths.get(0);
        }

        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));

        ItemSummary itemSummary = new ItemSummary(
                id,
                name,
                category,
                currentPrice,
                thumbnailUrl,
                startTime,
                endTime
        );
        return itemSummary;
    }

    private List<ItemSummary> executeSummaryQuery(String sql, Object... params) {
        List<ItemSummary> summaries = new ArrayList<>();

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            // Set
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
            e.printStackTrace();
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
                    create_at
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
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

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
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
                    images_path = ?,
                    status=?,
                
                WHERE id = ?
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            // Set các tham số cần cập nhật (Thứ tự từ 1 đến 10)
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

            // Set tham số ID cho điều kiện WHERE (Thứ tự 11)
            stmt.setString(12, itemId);

            // executeUpdate() trả về số dòng bị ảnh hưởng.
            // Nếu > 0 nghĩa là update thành công (có tìm thấy ID)
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
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
            // Set tham số ID cần xóa
            stmt.setString(1, id);

            // Nếu xóa thành công (ID có tồn tại), rowsAffected sẽ > 0
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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

    public List<ItemSummary> findAllItems() {
        String sql = """
                SELECT id,
                       name,
                       category,
                       current_price,
                       image_path,
                       start_time,
                       end_time 
                FROM items
                """;

        return executeSummaryQuery(sql);
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
                        imagePaths = gson.fromJson(pathsData, List.class);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imagePaths;
    }


    private Item mapRow(ResultSet rs) throws Exception {
        String pathsData = rs.getString("image_path");
        List<String> imagePaths = new ArrayList<>();
        if (pathsData != null && !pathsData.isEmpty()) {
            try {
                // Try to parse as JSON list (new format)
                imagePaths = gson.fromJson(pathsData, List.class);
                if (imagePaths == null) {
                    imagePaths = new ArrayList<>();
                }
            } catch (Exception e) {
                // Fallback for old format (single string path)
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
        String sql = "UPDATE items SET status = 'ENDED' WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public boolean updateCurrentPrice(Connection conn, String itemId, double newPrice) {

        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setString(2, itemId);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> updateStatus() {
        List<String> updatedId = new ArrayList<>();
        String selectEndedSql = "SELECT id FROM items WHERE status = 'ONGOING' AND datetime(end_time) <= datetime('now','localtime')";
        String selectLiveSql = "SELECT id FROM items WHERE status = 'UPCOMING' AND datetime(start_time) <= datetime('now','localtime')";

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(selectEndedSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    updatedId.add(rs.getString("id"));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(selectLiveSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    updatedId.add(rs.getString("id"));
                }
            }

            for (String id : updatedId) {

            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updatedId;
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
            e.printStackTrace();
        }
        return 0.0;
    }
}