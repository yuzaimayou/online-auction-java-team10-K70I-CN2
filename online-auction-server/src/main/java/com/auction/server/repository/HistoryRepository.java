package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.dto.BidHistoryItemDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistoryRepository {
    private static final Logger LOGGER = Logger.getLogger(HistoryRepository.class.getName());
    private static volatile HistoryRepository instance;

    public static HistoryRepository getInstance() {
        if (instance == null) {
            synchronized (HistoryRepository.class) {
                if (instance == null) {
                    instance = new HistoryRepository();
                }
            }
        }
        return instance;
    }

    private BidHistoryItemDTO mapRowToBidDTO(ResultSet rs) throws SQLException {
        String itemId = rs.getString("item_id");
        String userName = rs.getString("username");
        double bidPrice = rs.getDouble("bid_price");

        String bidTimeStr = rs.getString("bid_time");
        LocalDateTime bidTime = LocalDateTime.MIN;

        if (bidTimeStr != null && !bidTimeStr.isBlank()) {
            try {
                if (bidTimeStr.contains(" ")) {
                    bidTimeStr = bidTimeStr.replace(" ", "T");
                }
                if (bidTimeStr.contains(".") && bidTimeStr.length() > 29) {
                    bidTimeStr = bidTimeStr.substring(0, 29);
                }
                bidTime = LocalDateTime.parse(bidTimeStr);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse bid time: " + bidTimeStr + " - Error: " + e.getMessage(), e);
                bidTime = LocalDateTime.MIN;
            }
        }

        return new BidHistoryItemDTO(itemId, userName, bidPrice, bidTime);
    }

    public List<BidHistoryItemDTO> getBidHistoryForItem(String itemId) {
        String sql = """
                SELECT b.item_id, u.username, b.bid_price, b.bid_time
                FROM bids b
                JOIN users u ON b.user_id = u.id
                WHERE b.item_id = ?
                ORDER BY datetime(replace(b.bid_time, 'T', ' ')) DESC, b.id DESC
                """;
        List<BidHistoryItemDTO> bidHistoryItemDTOS = new ArrayList<>();
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidHistoryItemDTO bid = mapRowToBidDTO(rs);
                    bidHistoryItemDTOS.add(bid);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load bid history for item " + itemId, e);
        }
        return bidHistoryItemDTOS;
    }
}
