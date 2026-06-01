package com.auction.server.service.wallet;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.*;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

// Service xử lý nạp tiền và truy vấn số dư.
// Bidding logic (freeze/unfreeze trong giao dịch đặt giá) được thực hiện bởi BidService.
public class WalletService {
    private static final Logger LOGGER = Logger.getLogger(WalletService.class.getName());

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txLogRepo;

    public WalletService() {
        this.walletRepo = new WalletRepository();
        this.txLogRepo = new WalletTransactionRepository();
    }

    // Nạp tiền vào tài khoản (chạy trong transaction riêng).
    public boolean deposit(String userId, double amount) {
        if (userId == null || userId.isBlank() || amount <= 0)
            return false;
        UserRepository userRepo = new UserRepository();
        User user = userRepo.findById(userId);
        if (user == null || "Suspended".equalsIgnoreCase(user.getStatus())) {
            LOGGER.info("Deposit rejected: account suspended - " + userId);
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            double[] before = walletRepo.getBalances(conn, userId);
            boolean ok = walletRepo.deposit(conn, userId, amount);
            if (!ok) {
                rollback(conn);
                return false;
            }

            txLogRepo.logDeposit(conn, userId, amount, before[0], before[0] + amount);
            conn.commit();
            return true;
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, "Failed to deposit funds for user " + userId, e);
            return false;
        } finally {
            close(conn);
        }
    }

    private static void rollback(Connection conn) {
        if (conn == null)
            return;
        try {
            conn.rollback();
        } catch (Exception ignored) {
        }
    }

    private static void close(Connection conn) {
        if (conn == null)
            return;
        try {
            conn.close();
        } catch (Exception ignored) {
        }
    }

    // Đọc số dư hiện tại của user từ DB
    public double[] getBalance(String userId) {
        if (userId == null || userId.isBlank())
            return null;
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            return walletRepo.getBalances(conn, userId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get wallet balance for user " + userId, e);
            return null;
        } finally {
            close(conn);
        }
    }
}
