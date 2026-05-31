package com.auction.server.repository;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class WalletRepositoryTest extends RepositoryTestSupport {

    private void seedUser(double balance, double frozenBalance) throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, email, balance, frozen_balance, isVerify, rating)
                     VALUES('u1','alice','pass','User','alice@example.com',?,?,?,?)
                     """)) {
            stmt.setDouble(1, balance);
            stmt.setDouble(2, frozenBalance);
            stmt.setInt(3, 1);
            stmt.setDouble(4, 4.5);
            stmt.executeUpdate();
        }
    }

    private double[] readBalances() throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT balance, frozen_balance FROM users WHERE id = 'u1'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return new double[]{rs.getDouble("balance"), rs.getDouble("frozen_balance")};
            }
        }
    }

    @Test
    void shouldReadFreezeUnfreezeAndDeductBalances() throws Exception {
        WalletRepository repo = new WalletRepository();
        seedUser(1000.0, 200.0);

        try (Connection conn = openConnection()) {
            assertArrayEquals(new double[]{1000.0, 200.0}, repo.getBalances(conn, "u1"));
            assertTrue(repo.freezeAmount(conn, "u1", 300.0));
            assertArrayEquals(new double[]{700.0, 500.0}, readBalances());
            assertTrue(repo.unfreezeAmount(conn, "u1", 100.0));
            assertArrayEquals(new double[]{800.0, 400.0}, readBalances());
            assertTrue(repo.deductFromFrozen(conn, "u1", 150.0));
            assertArrayEquals(new double[]{800.0, 250.0}, readBalances());
            assertTrue(repo.creditBalance(conn, "u1", 50.0));
            assertArrayEquals(new double[]{850.0, 250.0}, readBalances());
            assertTrue(repo.deposit(conn, "u1", 25.0));
            assertArrayEquals(new double[]{875.0, 250.0}, readBalances());
        }
    }

    @Test
    void shouldRejectOperationsWhenFundsAreInsufficient() throws Exception {
        WalletRepository repo = new WalletRepository();
        seedUser(100.0, 20.0);

        try (Connection conn = openConnection()) {
            assertFalse(repo.freezeAmount(conn, "u1", 200.0));
            assertFalse(repo.unfreezeAmount(conn, "u1", 50.0));
            assertFalse(repo.deductFromFrozen(conn, "u1", 50.0));
            assertArrayEquals(new double[]{100.0, 20.0}, readBalances());
        }
    }
}

