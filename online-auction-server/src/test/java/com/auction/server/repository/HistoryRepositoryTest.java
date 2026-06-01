package com.auction.server.repository;

import com.auction.shared.model.dto.BidHistoryItemDTO;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryRepositoryTest extends RepositoryTestSupport {

    private void seedData() throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement userStmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, email, balance, frozen_balance, isVerify, rating)
                     VALUES(?,?,?,?,?,?,?,?,?)
                     """);
             PreparedStatement itemStmt = conn.prepareStatement("""
                     INSERT INTO items(id, name, description, start_price, current_price, seller_id, start_time, end_time, category, bid_step, image_path, status, create_at, search_name, current_bidder_id)
                     VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                     """);
             PreparedStatement bidStmt = conn.prepareStatement("""
                     INSERT INTO bids(item_id, user_id, bid_price, bid_time)
                     VALUES(?,?,?,?)
                     """)) {
            userStmt.setString(1, "u1");
            userStmt.setString(2, "alice");
            userStmt.setString(3, "pass");
            userStmt.setString(4, "User");
            userStmt.setString(5, "alice@example.com");
            userStmt.setDouble(6, 1000);
            userStmt.setDouble(7, 0);
            userStmt.setInt(8, 1);
            userStmt.setDouble(9, 4.5);
            userStmt.executeUpdate();

            itemStmt.setString(1, "item-1");
            itemStmt.setString(2, "Laptop");
            itemStmt.setString(3, "Desc");
            itemStmt.setDouble(4, 1000);
            itemStmt.setDouble(5, 1200);
            itemStmt.setString(6, "seller-1");
            itemStmt.setString(7, LocalDateTime.now().minusHours(2).toString());
            itemStmt.setString(8, LocalDateTime.now().plusHours(2).toString());
            itemStmt.setString(9, "Electronics");
            itemStmt.setDouble(10, 50);
            itemStmt.setString(11, "[\"thumb.png\"]");
            itemStmt.setString(12, "ONGOING");
            itemStmt.setString(13, LocalDateTime.now().toString());
            itemStmt.setString(14, "Laptop");
            itemStmt.setString(15, "u2");
            itemStmt.executeUpdate();

            bidStmt.setString(1, "item-1");
            bidStmt.setString(2, "u1");
            bidStmt.setDouble(3, 1100);
            bidStmt.setString(4, "2026-05-31T10:00:00");
            bidStmt.executeUpdate();

            bidStmt.setString(1, "item-1");
            bidStmt.setString(2, "u1");
            bidStmt.setDouble(3, 1200);
            bidStmt.setString(4, "2026-05-31 10:05:00");
            bidStmt.executeUpdate();
        }
    }

    @Test
    void shouldReturnHistoryForItemOrderedByNewestFirst() throws Exception {
        HistoryRepository repo = HistoryRepository.getInstance();
        seedData();

        List<BidHistoryItemDTO> history = repo.getBidHistoryForItem("item-1");
        assertEquals(2, history.size());
        BidHistoryItemDTO first = history.getFirst();
        BidHistoryItemDTO last = history.getLast();
        assertAll(
                () -> assertEquals("item-1", first.itemId),
                () -> assertEquals("alice", first.userName),
                () -> assertEquals(1200.0, first.bidPrice),
                () -> assertTrue(first.bidTime.isAfter(last.bidTime) || first.bidTime.equals(last.bidTime)),
                () -> assertEquals(1100.0, last.bidPrice)
        );
    }
}

