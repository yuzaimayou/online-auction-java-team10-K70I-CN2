package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.server.util.StringUtil;
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
    private static volatile ItemRepository instance; // [FIX BUG #8] volatile để đảm bảo thread-safe với double-checked locking

    private ItemRepository() {
    }

    // [FIX BUG #8] getInstance() trước đây không thread-safe, hai thread có thể tạo instance cùng lúc.
    // Dùng double-checked locking với volatile để fix.
    public static ItemRepository getInstance() {
        if (instance == null) {
            synchronized (ItemRepository.class) {
                if (instance == null) {
                    instance = new ItemRepository();
                }
            }
        }
        return instance;
    }

    // [FIX BUG #10] Trước đây: new GsonUtil().getInstance() — gọi static method qua instance mới (sai pattern).
    // Nay dùng GsonUtil.getInstance() đúng cách.
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

        LocalDateTime startTime =
                LocalDateTime.parse(rs.getString("start_time"));

        LocalDateTime endTime =
                LocalDateTime.parse(rs.getString("end_time"));

        // [FIX BUG #3] Trước đây dùng AuctionStatus.valueOf() ném IllegalArgumentException
        // nếu DB còn chứa giá trị cũ 'PENDING' (không tồn tại trong enum).
        // Nay dùng AuctionStatus.fromString() có fallback về UPCOMING.
        AuctionStatus status = AuctionStatus.fromString(rs.getString("status"));

        return new ItemSummary(
                id,
                name,
                category,
                currentPrice,
                thumbnailUrl,
                startTime,
                endTime,
                status
        );
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
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateItem(Item item, String itemId) {
        // [FIX BUG #1a] Trước đây column là "images_path" — sai, tên đúng trong DB là "image_path".
        // [FIX BUG #1b] Trước đây "search_name" thiếu "= ?" nên SQL bị lỗi cú pháp và luôn thất bại.
        // Số lượng tham số: 11 field + 1 WHERE id = 12 tham số tổng.
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
            stmt.setString(10, gson.toJson(item.getImagesPath())); // [FIX BUG #1a] image_path
            stmt.setString(11, item.getStatus());
            stmt.setString(12, StringUtil.removeAccents(item.getName())); // [FIX BUG #1b] search_name = ?

            // Set tham số ID cho điều kiện WHERE (tham số 13)
            stmt.setString(13, itemId);

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
            stmt.setString(1, id);

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

    // [FIX BUG #2] Trước đây query thiếu cột "status" trong SELECT.
    // mapRowToItemSummary() gọi rs.getString("status") → SQLException vì cột không có trong ResultSet.
    // Đã thêm "status" vào danh sách SELECT.
    public List<ItemSummary> findAllBySellerId(String sellerID) {
        String sql = """
                SELECT id,
                       name,
                       category,
                       current_price,
                       image_path,
                       start_time,
                       end_time,
                       status
                FROM items
                WHERE seller_id = ?
                """;

        return executeSummaryQuery(sql, sellerID);
    }

    public List<ItemSummary> searchItems(List<String> keywords, int offset) throws SQLException {
        List<ItemSummary> items = new ArrayList<>();
        //khoi tao cau lenh sql dong
        StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE ");
        for (int i = 0; i < keywords.size(); i++) {
            sql.append("LOWER(search_name) LIKE ?");
            if (i < keywords.size() - 1) sql.append(" AND ");
        }
        sql.append(" ORDER BY id DESC LIMIT 10 OFFSET ?");

        System.out.println(sql.toString());
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < keywords.size(); i++) {
                stmt.setString(i + 1, "%" + keywords.get(i) + "%");
            }
            stmt.setInt(keywords.size() + 1, offset);
            System.out.println(stmt.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapRowToItemSummary(rs));
            }
        }
        return items;
    }

    public List<ItemSummary> findAllItems(
            String sortOrder,
            int page,
            String category,
            String status
    ) {

        int limit = 20;
        int offset = page * limit;

        StringBuilder sql = new StringBuilder("""
        SELECT id,
               name,
               category,
               current_price,
               image_path,
               start_time,
               end_time,
               status
        FROM items
        WHERE 1=1
    """);

        List<Object> params = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(category) = LOWER(?)");
            params.add(category);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(status) = UPPER(?)");
            params.add(status);
        }

        sql.append(" ORDER BY ").append(sortOrder);
        sql.append(" LIMIT ? OFFSET ?");

        params.add(limit);
        params.add(offset);

        return executeSummaryQuery(sql.toString(), params.toArray());
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
                PreparedStatement stmt = conn.prepareStatement(sql)
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


    // [FIX BUG #5] mapRow() trước đây gọi constructor không có id rồi gọi setId() sau.
    // Nay truyền rs.getString("id") trực tiếp vào constructor mới Item(id, ...).
    private Item mapRow(ResultSet rs) throws Exception {
        String pathsData = rs.getString("image_path");
        List<String> imagePaths = new ArrayList<>();
        if (pathsData != null && !pathsData.isEmpty()) {
            try {
                imagePaths = gson.fromJson(pathsData, List.class);
                if (imagePaths == null) {
                    imagePaths = new ArrayList<>();
                }
            } catch (Exception e) {
                imagePaths.add(pathsData);
            }
        }

        Item item = new Item(
                rs.getString("id"),           // [FIX BUG #5] truyền id thật từ DB
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
        // setId() không còn cần thiết vì id đã được truyền vào constructor
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

        List<String> updatedIds = new ArrayList<>();

        String updateUpcomingToOngoing = """
        UPDATE items
        SET status = 'ONGOING'
        WHERE status = 'UPCOMING'
          AND datetime(start_time) <= datetime('now','localtime')
          AND datetime(end_time) > datetime('now','localtime')
    """;

        String updateOngoingToEnded = """
        UPDATE items
        SET status = 'ENDED'
        WHERE status = 'ONGOING'
          AND datetime(end_time) <= datetime('now','localtime')
    """;

        String getChangedIds = """
        SELECT id
        FROM items
        WHERE status IN ('ONGOING', 'ENDED')
    """;

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(updateUpcomingToOngoing)) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(updateOngoingToEnded)) {
                ps.executeUpdate();
            }

            try (
                    PreparedStatement ps = conn.prepareStatement(getChangedIds);
                    ResultSet rs = ps.executeQuery()
            ) {

                while (rs.next()) {
                    updatedIds.add(rs.getString("id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return updatedIds;
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