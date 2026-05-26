package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.shared.constant.ItemStatusConstants;
import com.auction.shared.model.item.Item;

import java.sql.Connection;
import java.time.LocalDateTime;

// Service xử lý logic đóng băng tiền khi đặt giá và nạp tiền.
//
// Luồng đặt giá (1 transaction duy nhất):
//   1. Khoá item theo JVM để tránh race condition giữa các thread
//   2. Đọc thông tin item
//   3. Validate: trạng thái, thời gian, giá, quyền đặt
//   4. Kiểm tra số dư người đặt
//   5. Đóng băng tiền người đặt mới
//   6. Hoàn tiền ngay cho người đặt cao nhất trước đó
//   7. Cập nhật giá và bidder trên item
//   8. Ghi lịch sử bid
//   9. Ghi log wallet_transactions → COMMIT
public class WalletService {

    private final WalletRepository            walletRepo;
    private final WalletTransactionRepository txLogRepo;
    private final ItemRepository              itemRepo;
    private final BidRepository               bidRepo;

    public WalletService() {
        this.walletRepo = new WalletRepository();
        this.txLogRepo  = new WalletTransactionRepository();
        this.itemRepo = ItemRepository.getInstance();
        this.bidRepo    = new BidRepository();
    }

    // Đặt giá với cơ chế đóng băng tiền.
    // Trả về BidResult để caller biết thành công hay thất bại và lý do.
    public BidResult placeBid(String itemId, String bidderId, double bidPrice) {
        if (itemId == null || itemId.isBlank())    return BidResult.fail("Item id không hợp lệ");
        if (bidderId == null || bidderId.isBlank()) return BidResult.fail("Bidder id không hợp lệ");
        if (bidPrice <= 0)                          return BidResult.fail("Giá đặt phải dương");

        synchronized (getItemLock(itemId)) {
            Connection conn = null;
            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                // Bước 2: Đọc item trong transaction
                Item item = itemRepo.findById(conn, itemId);
                if (item == null) {
                    rollback(conn);
                    return BidResult.fail("Không tìm thấy item");
                }

                if (item.getSellerId().equals(bidderId)) {
                    rollback(conn);
                    return BidResult.fail("Người bán không được tự đặt giá");
                }

                if (bidderId.equals(item.getCurrentTopPLayerId())) {
                    rollback(conn);
                    return BidResult.fail("Bạn đang là người đặt giá cao nhất cho sản phẩm này");
                }

                String status = item.getStatus();
                if (!ItemStatusConstants.ONGOING.equalsIgnoreCase(status)) {
                    rollback(conn);
                    return BidResult.fail("Phiên đấu giá không đang hoạt động (status=" + status + ")");
                }

                if (LocalDateTime.now().isAfter(item.getEndTime())) {
                    rollback(conn);
                    return BidResult.fail("Phiên đấu giá đã kết thúc");
                }

                double minPrice = item.getHighestCurrentPrice() + item.getBidStep();
                if (bidPrice < minPrice - 1e-9) {
                    rollback(conn);
                    return BidResult.fail(String.format(
                            "Giá %.2f thấp hơn mức tối thiểu %.2f", bidPrice, minPrice));
                }

                // Bước 4: Kiểm tra số dư
                double[] bidderBalances = walletRepo.getBalances(conn, bidderId);
                double bidderBalance = bidderBalances[0];

                if (bidderBalance < bidPrice) {
                    rollback(conn);
                    return BidResult.fail(String.format(
                            "Số dư không đủ: có %.2f, cần %.2f", bidderBalance, bidPrice));
                }

                // Bước 5: Đóng băng tiền người đặt mới
                boolean frozeOk = walletRepo.freezeAmount(conn, bidderId, bidPrice);
                if (!frozeOk) {
                    rollback(conn);
                    return BidResult.fail("Đóng băng tiền thất bại (có thể do cập nhật đồng thời)");
                }

                // Bước 6: Hoàn tiền ngay cho người đặt cao nhất trước
                String prevBidderId = item.getCurrentTopPLayerId();
                double prevBidPrice = item.getHighestCurrentPrice();

                if (prevBidderId != null && !prevBidderId.isBlank()
                        && !prevBidderId.equals(bidderId)) {

                    boolean unfrozeOk = walletRepo.unfreezeAmount(conn, prevBidderId, prevBidPrice);
                    if (!unfrozeOk) {
                        rollback(conn);
                        return BidResult.fail("Hoàn tiền cho người đặt trước thất bại");
                    }

                    // Ghi log UNFREEZE cho người thua
                    double[] prevBal = walletRepo.getBalances(conn, prevBidderId);
                    txLogRepo.logUnfreeze(conn, prevBidderId, prevBidPrice,
                            prevBal[0] - prevBidPrice,
                            prevBal[0],
                            itemId);
                }

                // Bước 7: Cập nhật giá và bidder trên item
                boolean itemUpdated = itemRepo.updateCurrentBidder(conn, itemId, bidPrice, bidderId);
                if (!itemUpdated) {
                    rollback(conn);
                    return BidResult.fail("Cập nhật item thất bại");
                }

                // Bước 8: Ghi lịch sử bid
                String bidTime = LocalDateTime.now().toString();
                boolean bidInserted = bidRepo.createBid(conn, itemId, bidderId, bidPrice, bidTime);
                if (!bidInserted) {
                    rollback(conn);
                    return BidResult.fail("Ghi lịch sử bid thất bại");
                }

                // Bước 9: Ghi log FREEZE cho người đặt mới
                txLogRepo.logFreeze(conn, bidderId, bidPrice,
                        bidderBalance,
                        bidderBalance - bidPrice,
                        itemId);

                conn.commit();
                System.out.printf("[WalletService] Đặt giá thành công: item=%s bidder=%s price=%.2f%n",
                        itemId, bidderId, bidPrice);
                return BidResult.success(bidPrice);

            } catch (Exception e) {
                rollback(conn);
                System.err.println("[WalletService] Lỗi placeBid: " + e.getMessage());
                e.printStackTrace();
                return BidResult.fail("Lỗi hệ thống: " + e.getMessage());
            } finally {
                close(conn);
            }
        }
    }

    // Nạp tiền vào tài khoản (chạy trong transaction riêng).
    public boolean deposit(String userId, double amount) {
        if (userId == null || userId.isBlank() || amount <= 0) return false;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            double[] before = walletRepo.getBalances(conn, userId);
            boolean ok = walletRepo.deposit(conn, userId, amount);
            if (!ok) { rollback(conn); return false; }

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

    // Lấy khoá theo item để tuần tự hoá đặt giá
    private Object getItemLock(String itemId) {
        return com.auction.server.util.AuctionLockManager.getItemLock(itemId);
    }

    private static void rollback(Connection conn) {
        if (conn == null) return;
        try { conn.rollback(); } catch (Exception ignored) {}
    }

    private static void close(Connection conn) {
        if (conn == null) return;
        try { conn.close(); } catch (Exception ignored) {}
    }

    // Kết quả đặt giá — thay thế việc ném exception để điều khiển luồng
    public static final class BidResult {
        public final boolean success;
        public final double  acceptedPrice;   // hợp lệ khi success == true
        public final String  errorMessage;    // hợp lệ khi success == false

        private BidResult(boolean success, double acceptedPrice, String errorMessage) {
            this.success       = success;
            this.acceptedPrice = acceptedPrice;
            this.errorMessage  = errorMessage;
        }

        public static BidResult success(double price) {
            return new BidResult(true, price, null);
        }

        public static BidResult fail(String reason) {
            return new BidResult(false, 0, reason);
        }

        @Override
        public String toString() {
            return success
                    ? "BidResult{OK, price=" + acceptedPrice + "}"
                    : "BidResult{FAIL, reason=" + errorMessage + "}";
        }
    }
    // Đọc số dư hiện tại của user từ DB
    public double[] getBalance(String userId) {
        if (userId == null || userId.isBlank()) return null;
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
