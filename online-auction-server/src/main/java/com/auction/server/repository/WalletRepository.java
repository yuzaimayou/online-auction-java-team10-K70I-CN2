package com.auction.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Mọi hàm thay đổi dữ liệu đều yêu cầu Connection từ bên ngoài để dùng chung transaction.
public class WalletRepository {

    // Lấy balance và frozen_balance của người dùng.
    public double[] getBalances(Connection conn, String userId) throws Exception {
        String sql = "SELECT balance, frozen_balance FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new Exception("Không tìm thấy auth: " + userId);
                }
                return new double[] { rs.getDouble("balance"), rs.getDouble("frozen_balance") };
            }
        }
    }

    // Đóng băng tiền trong ví khi người dùng đặt giá.
    public boolean freezeAmount(Connection conn, String userId, double amount) throws Exception {
        String sql = """
                UPDATE users
                SET balance        = balance        - ?,
                    frozen_balance = frozen_balance + ?
                WHERE id = ? AND balance >= ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setString(3, userId);
            stmt.setDouble(4, amount);
            return stmt.executeUpdate() > 0;
        }
    }

    // Hoàn lại tiền đã đóng băng cho người dùng.
    public boolean unfreezeAmount(Connection conn, String userId, double amount) throws Exception {
        String sql = """
                UPDATE users
                SET frozen_balance = frozen_balance - ?,
                    balance        = balance        + ?
                WHERE id = ? AND frozen_balance >= ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setString(3, userId);
            stmt.setDouble(4, amount);
            return stmt.executeUpdate() > 0;
        }
    }

    // Trừ tiền từ frozen_balance của người thắng khi kết thúc đấu giá.
    public boolean deductFromFrozen(Connection conn, String userId, double amount) throws Exception {
        String sql = """
                UPDATE users
                SET frozen_balance = frozen_balance - ?
                WHERE id = ? AND frozen_balance >= ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            stmt.setDouble(3, amount);
            return stmt.executeUpdate() > 0;
        }
    }

    // Cộng tiền vào balance của người bán.
    public boolean creditBalance(Connection conn, String userId, double amount) throws Exception {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Nạp tiền vào balance của người dùng.
    public boolean deposit(Connection conn, String userId, double amount) throws Exception {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}
