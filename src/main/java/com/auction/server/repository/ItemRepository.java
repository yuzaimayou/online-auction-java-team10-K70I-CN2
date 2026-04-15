package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.model.product.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            max_price,
            min_price,
            image_path
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
            stmt.setDouble(11, item.getMaxPrice());
            stmt.setDouble(12, item.getMinPrice());
            stmt.setString(13, item.getImagePath());

            stmt.executeUpdate();
            return true;

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
                            rs.getDouble("max_price"),
                            rs.getDouble("min_price"),
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
                            rs.getDouble("max_price"),
                            rs.getDouble("min_price"),
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
                        rs.getDouble("max_price"),
                        rs.getDouble("min_price"),
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
}