package com.auction.server.repository;

import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemRepositoryTest extends RepositoryTestSupport {

    private Item seedItem(String id, String name, double currentPrice, String status) throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(3);
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO items(id, name, description, start_price, current_price, seller_id, start_time, end_time, category, bid_step, image_path, status, create_at, search_name, current_bidder_id)
                     VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, "Description");
            stmt.setDouble(4, 1000.0);
            stmt.setDouble(5, currentPrice);
            stmt.setString(6, "seller-1");
            stmt.setString(7, start.toString());
            stmt.setString(8, end.toString());
            stmt.setString(9, "Electronics");
            stmt.setDouble(10, 50.0);
            stmt.setString(11, "[\"thumb.png\"]");
            stmt.setString(12, status);
            stmt.setString(13, LocalDateTime.now().toString());
            stmt.setString(14, name);
            stmt.setString(15, "user-1");
            stmt.executeUpdate();
        }
        return new Item(name, "Description", 1000.0, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), "seller-1", "Electronics", 50.0, List.of("thumb.png"));
    }

    @Test
    void shouldUpdateCurrentBidderPriceStatusAndEndTime() throws Exception {
        ItemRepository repo = ItemRepository.getInstance();
        seedItem("item-1", "Laptop", 1000.0, AuctionStatus.ONGOING.toString());

        try (Connection conn = openConnection()) {
            assertTrue(repo.updateCurrentBidder(conn, "item-1", 1200.0, "u1"));
            assertTrue(repo.updateCurrentPrice(conn, "item-1", 1250.0));
            assertTrue(repo.extendEndTime(conn, "item-1", LocalDateTime.now().plusHours(5)));
            assertTrue(repo.updateStatus(conn, "item-1", AuctionStatus.BANNED.toString()));
            assertTrue(repo.markEnded(conn, "item-1"));

            try (PreparedStatement stmt = conn.prepareStatement("SELECT current_price, current_bidder_id, status, end_time FROM items WHERE id = ?")) {
                stmt.setString(1, "item-1");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1250.0, rs.getDouble("current_price"));
                    assertEquals("u1", rs.getString("current_bidder_id"));
                    assertEquals(AuctionStatus.ENDED.toString(), rs.getString("status"));
                    assertNotNull(rs.getString("end_time"));
                }
            }
        }
    }

    @Test
    void shouldMapItemAndReturnSummaries() throws Exception {
        ItemRepository repo = ItemRepository.getInstance();
        seedItem("item-1", "Laptop", 1200.0, AuctionStatus.ONGOING.toString());
        seedItem("item-2", "Phone", 900.0, AuctionStatus.BANNED.toString());

        try (Connection conn = openConnection()) {
            Item item = repo.findById(conn, "item-1");
            assertAll(
                    () -> assertNotNull(item),
                    () -> assertEquals("Laptop", item.getName()),
                    () -> assertEquals(1000.0, item.getStartingPrice()),
                    () -> assertEquals(1200.0, item.getHighestCurrentPrice()),
                    () -> assertEquals("seller-1", item.getSellerId()),
                    () -> assertEquals("Electronics", item.getCategory()),
                    () -> assertEquals("user-1", item.getCurrentTopPLayerId()),
                    () -> assertEquals(AuctionStatus.ONGOING.toString(), item.getStoredStatus())
            );
        }

        List<ItemSummary> adminItems = repo.findAllItemsForAdmin("current_price desc", 0);
        assertEquals(2, adminItems.size());
        assertEquals("Laptop", adminItems.get(0).getName());
        assertEquals("Phone", adminItems.get(1).getName());

        List<ItemSummary> publicItems = repo.findAllItems("current_price desc", 0, "ALL");
        assertEquals(1, publicItems.size());
        assertEquals("Laptop", publicItems.get(0).getName());
    }
}

