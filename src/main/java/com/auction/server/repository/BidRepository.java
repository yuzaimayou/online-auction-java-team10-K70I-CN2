package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class BidRepository {

    public boolean createBid(int itemId, String userId, double bidPrice, String bidTime) {

        String sql = "INSERT INTO bids(item_id,user_id,bid_price,bid_time) VALUES(?,?,?,?)";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setInt(1, itemId);
            stmt.setString(2, userId);
            stmt.setDouble(3, bidPrice);
            stmt.setString(4, bidTime);

            stmt.executeUpdate();

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}