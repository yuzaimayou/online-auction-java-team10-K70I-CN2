package com.auction.server.repository;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class WalletTransactionRepositoryTest extends RepositoryTestSupport {

    private void seedUser() throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, email, balance, frozen_balance, isVerify, rating)
                     VALUES('u1','alice','pass','User','alice@example.com',1000,0,1,4.5)
                     """)) {
            stmt.executeUpdate();
        }
    }

    @Test
    void shouldInsertWalletTransactionRowsAndDetectAuctionPayment() throws Exception {
        WalletTransactionRepository repo = new WalletTransactionRepository();
        seedUser();

        try (Connection conn = openConnection()) {
            repo.logDeposit(conn, "u1", 100.0, 900.0, 1000.0);
            repo.logFreeze(conn, "u1", 250.0, 1000.0, 750.0, "item-1");
            repo.logAuctionPayment(conn, "u1", 750.0, 750.0, 0.0, "item-1");

            try (PreparedStatement stmt = conn.prepareStatement("SELECT type, amount, reference_id FROM wallet_transactions ORDER BY id ASC")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(WalletTransactionRepository.TYPE_DEPOSIT, rs.getString("type"));
                    assertEquals(100.0, rs.getDouble("amount"));
                    assertNull(rs.getString("reference_id"));

                    assertTrue(rs.next());
                    assertEquals(WalletTransactionRepository.TYPE_FREEZE_BID, rs.getString("type"));
                    assertEquals("item-1", rs.getString("reference_id"));

                    assertTrue(rs.next());
                    assertEquals(WalletTransactionRepository.TYPE_AUCTION_PAYMENT, rs.getString("type"));
                    assertEquals("item-1", rs.getString("reference_id"));
                    assertFalse(rs.next());
                }
            }

            assertTrue(repo.existsAuctionPayment(conn, "item-1"));
            assertFalse(repo.existsAuctionPayment(conn, "item-2"));
        }
    }
}

