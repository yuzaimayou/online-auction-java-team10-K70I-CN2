package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.dto.BidHistoryItemDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


public class HistoryRepository {
    private static HistoryRepository instance;

    public static HistoryRepository getInstance() {
        if (instance == null) {
            instance = new HistoryRepository();
        }
        return instance;
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

    private BidHistoryItemDTO mapRowToBidDTO(ResultSet rs) throws SQLException {
        String itemId = rs.getString("item_id");
        String userName = rs.getString("username");
        double bidPrice = rs.getDouble("bid_price");
        String bidTimeStr = rs.getString("bid_time");
        return new BidHistoryItemDTO(itemId, userName, bidPrice, parseDateTime(bidTimeStr));
    }

    public List<BidHistoryItemDTO> getBidHistoryForItem(String itemId) {
        String sql = """
                SELECT b.item_id, u.username, b.bid_price, b.bid_time
                FROM bids b
                JOIN users u ON b.user_id = u.id
                WHERE b.item_id = ?
                ORDER BY b.bid_time DESC
                """;
        List<BidHistoryItemDTO> bidHistoryItemDTOS = new ArrayList<>();
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidHistoryItemDTO bid = mapRowToBidDTO(rs);
                    bidHistoryItemDTOS.add(bid);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bidHistoryItemDTOS;
    }
}
