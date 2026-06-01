package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.*;
import com.auction.server.util.AuctionLockManager;
import com.auction.shared.model.account.User;

import java.sql.Connection;

// Service xử lý nạp tiền và truy vấn số dư.
// Bidding logic (freeze/unfreeze trong giao dịch đặt giá) được thực hiện bởi BidService.
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txLogRepo;
    private final ItemRepository itemRepo;
    private final BidRepository bidRepo;

    public WalletService() {
        this.walletRepo = new WalletRepository();
        this.txLogRepo = new WalletTransactionRepository();
        this.itemRepo = ItemRepository.getInstance();
        this.bidRepo = new BidRepository();
    }

    // Nạp tiền vào tài khoản (chạy trong transaction riêng).
    public boolean deposit(String userId, double amount) {
        if (userId == null || userId.isBlank() || amount <= 0)
            return false;
        UserRepository userRepo = new UserRepository();
        User user = userRepo.findById(userId);
        if (user == null || "Suspended".equalsIgnoreCase(user.getStatus())) {
            System.out.println("Deposit rejected: account suspended - " + userId);
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
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        } finally {
            close(conn);
        }
    }
}
