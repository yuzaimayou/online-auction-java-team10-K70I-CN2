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

    // Ghi một dòng log giao dịch ví.
    // amount luôn là số dương; reference_id thường là item_id (có thể null với DEPOSIT).
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

    // Log khi đóng băng tiền (người đặt giá mới)
    public void logFreeze(Connection conn, String userId, double amount,
                          double balanceBefore, double balanceAfter,
                          String itemId) throws Exception {
        insert(conn, userId, TYPE_FREEZE_BID, amount, balanceBefore, balanceAfter, itemId);
    }

    // Log khi hoàn tiền (người thua cuộc trước đó)
    public void logUnfreeze(Connection conn, String userId, double amount,
                            double balanceBefore, double balanceAfter,
                            String itemId) throws Exception {
        insert(conn, userId, TYPE_UNFREEZE_BID, amount, balanceBefore, balanceAfter, itemId);
    }

    // Log khi kết thúc đấu giá — trừ tiền người thắng / cộng tiền người bán
    public void logAuctionPayment(Connection conn, String userId, double amount,
                                  double balanceBefore, double balanceAfter,
                                  String itemId) throws Exception {
        insert(conn, userId, TYPE_AUCTION_PAYMENT, amount, balanceBefore, balanceAfter, itemId);
    }

    // Log khi nạp tiền
    public void logDeposit(Connection conn, String userId, double amount,
                           double balanceBefore, double balanceAfter) throws Exception {
        insert(conn, userId, TYPE_DEPOSIT, amount, balanceBefore, balanceAfter, null);
    }
}
