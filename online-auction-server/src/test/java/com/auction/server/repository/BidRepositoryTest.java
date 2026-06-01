package com.auction.server.repository;

import com.auction.shared.model.payloads.AutoBidPayload;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidRepositoryTest extends RepositoryTestSupport {

    private void seedUserAndItem() throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement userStmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, email, balance, frozen_balance, isVerify, rating)
                     VALUES('u1','alice','pass','User','alice@example.com',1000,0,1,4.5)
                     """);
             PreparedStatement itemStmt = conn.prepareStatement("""
                     INSERT INTO items(id, name, description, start_price, current_price, seller_id, start_time, end_time, category, bid_step, image_path, status, create_at, search_name, current_bidder_id)
                     VALUES('item-1','Laptop','Gaming laptop',1000,1000,'seller-1',?,?,?,?,?,?,?,?,?)
                     """)) {
            userStmt.executeUpdate();
            itemStmt.setString(1, LocalDateTime.now().minusHours(2).toString());
            itemStmt.setString(2, LocalDateTime.now().plusHours(2).toString());
            itemStmt.setString(3, "Electronics");
            itemStmt.setDouble(4, 50.0);
            itemStmt.setString(5, "[\"img.png\"]");
            itemStmt.setString(6, "ONGOING");
            itemStmt.setString(7, LocalDateTime.now().toString());
            itemStmt.setString(8, "Laptop");
            itemStmt.setString(9, null);
            itemStmt.executeUpdate();
        }
    }

    @Test
    void shouldCreateBidAndFindLastBidder() throws Exception {
        BidRepository repo = new BidRepository();
        seedUserAndItem();

        try (Connection conn = openConnection()) {
            assertTrue(repo.createBid(conn, "item-1", "u1", 1200.0, "2026-05-31T10:00:00"));
            assertTrue(repo.createBid(conn, "item-1", "u2", 1300.0, "2026-05-31T10:05:00"));
            assertEquals("u2", repo.findLastBidder(conn, "item-1"));
        }
    }

    @Test
    void shouldUpsertDeactivateAndReadAutoBids() throws Exception {
        BidRepository repo = new BidRepository();
        seedUserAndItem();

        try (Connection conn = openConnection()) {
            assertTrue(repo.upsertAutoBid(conn, "item-1", "u1", 2000.0, 100.0, "2026-05-31T09:00:00"));
            assertTrue(repo.upsertAutoBid(conn, "item-1", "u2", 1500.0, 50.0, "invalid-date"));

            List<BidRepository.AutoBidConfig> active = repo.findActiveAutoBids(conn, "item-1");
            assertEquals(1, active.size());
            BidRepository.AutoBidConfig first = active.getFirst();
            assertAll(
                    () -> assertEquals("u1", first.getUserId()),
                    () -> assertEquals(2000.0, first.getMaxBid()),
                    () -> assertEquals(100.0, first.getIncrement())
            );

            AutoBidPayload payload = repo.findActiveAutoBid("item-1", "u1");
            assertNotNull(payload);
            assertEquals("u1", payload.getUserId());
            assertTrue(payload.getIsActive());

            assertTrue(repo.deactivateAutoBidIfPresent(conn, "item-1", "u1"));
            try (PreparedStatement stmt = conn.prepareStatement("SELECT is_active FROM auto_bids WHERE item_id = ? AND user_id = ?")) {
                stmt.setString(1, "item-1");
                stmt.setString(2, "u1");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt("is_active"));
                }
            }
        }
    }
}

