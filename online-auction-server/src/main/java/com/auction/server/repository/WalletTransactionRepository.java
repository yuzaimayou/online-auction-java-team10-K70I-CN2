package com.auction.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

// Repository ghi log vào bảng wallet_transactions.
// Mọi hàm đều yêu cầu Connection từ bên ngoài để tham gia cùng transaction với bid.
public class WalletTransactionRepository {

    // Các loại giao dịch
    public static final String TYPE_DEPOSIT         = "DEPOSIT";
    public static final String TYPE_FREEZE_BID      = "FREEZE_BID";
    public static final String TYPE_UNFREEZE_BID    = "UNFREEZE_BID";
    public static final String TYPE_AUCTION_PAYMENT = "AUCTION_PAYMENT";

    private static final String INSERT_SQL = """
            INSERT INTO wallet_transactions
                (user_id, type, amount, balance_before, balance_after, reference_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    // Ghi một bản ghi giao dịch ví vào bảng wallet_transactions.
    public void insert(Connection conn,
                       String userId,
                       String type,
                       double amount,
                       double balanceBefore,
                       double balanceAfter,
                       String referenceId) throws Exception {

        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            stmt.setString(1, userId);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.setDouble(4, balanceBefore);
            stmt.setDouble(5, balanceAfter);
            stmt.setString(6, referenceId);
            stmt.setString(7, LocalDateTime.now().toString());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new Exception("Ghi wallet_transactions thất bại, auth=" + userId);
            }
        }
    }

    // Ghi log khi đóng băng tiền cho lượt đặt giá.
    public void logFreeze(Connection conn, String userId, double amount,
                          double balanceBefore, double balanceAfter,
                          String itemId) throws Exception {
        insert(conn, userId, TYPE_FREEZE_BID, amount, balanceBefore, balanceAfter, itemId);
    }

    // Ghi log khi hoàn lại tiền đã đóng băng.
    public void logUnfreeze(Connection conn, String userId, double amount,
                            double balanceBefore, double balanceAfter,
                            String itemId) throws Exception {
        insert(conn, userId, TYPE_UNFREEZE_BID, amount, balanceBefore, balanceAfter, itemId);
    }

    // Ghi log thanh toán khi đấu giá kết thúc.
    public void logAuctionPayment(Connection conn, String userId, double amount,
                                  double balanceBefore, double balanceAfter,
                                  String itemId) throws Exception {
        insert(conn, userId, TYPE_AUCTION_PAYMENT, amount, balanceBefore, balanceAfter, itemId);
    }

    // Ghi log khi người dùng nạp tiền vào ví.
    public void logDeposit(Connection conn, String userId, double amount,
                           double balanceBefore, double balanceAfter) throws Exception {
        insert(conn, userId, TYPE_DEPOSIT, amount, balanceBefore, balanceAfter, null);
    }

    // Kiểm tra item đã có giao dịch thanh toán đấu giá hay chưa.
    public boolean existsAuctionPayment(Connection conn, String itemId) throws Exception {
        String sql = "SELECT 1 FROM wallet_transactions WHERE type = ? AND reference_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, TYPE_AUCTION_PAYMENT);
            stmt.setString(2, itemId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
