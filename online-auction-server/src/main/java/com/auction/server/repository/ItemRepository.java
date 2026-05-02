package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.model.product.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

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
                Connection conn = DatabaseConnection.connect();
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
            stmt.setString(11, item.getImagePath());
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
                    image_path = ?,
                    status=?,
                
                WHERE id = ?
                """;

        try (
                Connection conn = DatabaseConnection.connect();
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
            stmt.setString(10, item.getImagePath());
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
                Connection conn = DatabaseConnection.connect();
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
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, itemId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

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
                            rs.getString("image_path")
                    );
                    item.setId(rs.getString("id"));
                    return item;
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
                            rs.getString("image_path")
                    );
                    item.setId(rs.getString("id"));
                    return item;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Item> findAllBySellerId(String sellerID) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, sellerID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
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
                            rs.getString("image_path")
                    );
                    item.setId(rs.getString("id"));
                    items.add(item);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<Item> findAllItems() {

        List<Item> items = new ArrayList<>();

        String sql = "SELECT * FROM items";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {

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
                        rs.getString("image_path")
                );
                item.setId(rs.getString("id"));

                items.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    public void updateCurrentPrice(String itemId, double newPrice) {

        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try (
                Connection conn = DatabaseConnection.connect();
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
        String selectEndedSql = "SELECT id FROM items WHERE status = 'ACTIVE' AND datetime(end_time) <= datetime('now','localtime')";
        String selectLiveSql = "SELECT id FROM items WHERE status = 'PENDING' AND datetime(start_time) <= datetime('now','localtime')";

        try (Connection conn = DatabaseConnection.connect()) {
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
        }
        return updatedId;
    }

}