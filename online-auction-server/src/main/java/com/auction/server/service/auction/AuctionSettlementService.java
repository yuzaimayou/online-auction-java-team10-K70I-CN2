package com.auction.server.service.auction;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

// Service thanh toán khi phiên đấu giá kết thúc.
// Toàn bộ luồng chạy trong 1 transaction: trừ tiền người thắng,
// cộng tiền người bán, đánh dấu ENDED, ghi log.
public class AuctionSettlementService {
    private static final Logger LOGGER = Logger.getLogger(AuctionSettlementService.class.getName());

    private final ItemRepository              itemRepo;
    private final WalletRepository            walletRepo;
    private final WalletTransactionRepository txLogRepo;

    public AuctionSettlementService() {
        this.itemRepo = ItemRepository.getInstance();
        this.walletRepo = new WalletRepository();
        this.txLogRepo  = new WalletTransactionRepository();
    }

    // Thanh toán cho phiên đấu giá đã hết giờ.
    public SettlementResult settleAuction(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return SettlementResult.fail("Item id không hợp lệ");
        }

        synchronized (com.auction.server.service.auction.AuctionLockManager.getItemLock(itemId)) {
            Connection conn = null;
            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                // Đọc item
                Item item = itemRepo.findById(conn, itemId);
                if (item == null) {
                    rollback(conn);
                    return SettlementResult.fail("Không tìm thấy item: " + itemId);
                }

                boolean expired = java.time.LocalDateTime.now().isAfter(item.getEndTime());
                AuctionStatus status = item.getStatus();
                if (!expired && status != AuctionStatus.ENDED) {
                    rollback(conn);
                    return SettlementResult.fail("Phiên chưa kết thúc (status=" + status.name() + ")");
                }

                if (status == AuctionStatus.BANNED) {
                    rollback(conn);
                    return SettlementResult.fail("Item bị BAN, không thể thanh toán");
                }

                String winnerId     = item.getCurrentBidderId();
                double winningPrice = item.getHighestCurrentPrice();
                String sellerId     = item.getSellerId();

                // Idempotency check: if auction payment already occurred, do not double-charge/double-settle
                if (txLogRepo.existsAuctionPayment(conn, itemId)) {
                    conn.commit();
                    return SettlementResult.success(itemId, winnerId, sellerId, winningPrice);
                }

                // Không có ai đặt giá — chỉ đóng phiên
                if (winnerId == null || winnerId.isBlank()) {
                    itemRepo.markEnded(conn, itemId);
                    conn.commit();
                    return SettlementResult.noBids(itemId);
                }

                // Trừ giá thắng từ frozen_balance người thắng
                double[] winnerBal = walletRepo.getBalances(conn, winnerId);
                double winnerFrozenBefore = winnerBal[1];

                if (!walletRepo.deductFromFrozen(conn, winnerId, winningPrice)) {
                    rollback(conn);
                    return SettlementResult.fail("Không thể trừ tiền người thắng");
                }

                // Cộng tiền vào balance người bán
                double[] sellerBal = walletRepo.getBalances(conn, sellerId);
                double sellerBalBefore = sellerBal[0];

                if (!walletRepo.creditBalance(conn, sellerId, winningPrice)) {
                    rollback(conn);
                    return SettlementResult.fail("Không thể cộng tiền người bán");
                }

                // Đánh dấu item kết thúc
                itemRepo.markEnded(conn, itemId);

                // Ghi log cho người thắng và người bán
                txLogRepo.logAuctionPayment(conn, winnerId, winningPrice,
                        winnerFrozenBefore, winnerFrozenBefore - winningPrice, itemId);
                txLogRepo.logAuctionPayment(conn, sellerId, winningPrice,
                        sellerBalBefore, sellerBalBefore + winningPrice, itemId);

                conn.commit();
                LOGGER.info(String.format("[Settlement] item=%s winner=%s seller=%s price=%.2f",
                        itemId, winnerId, sellerId, winningPrice));
                return SettlementResult.success(itemId, winnerId, sellerId, winningPrice);

            } catch (Exception e) {
                rollback(conn);
                LOGGER.log(Level.SEVERE, "Failed to settle auction item " + itemId, e);
                return SettlementResult.fail("Lỗi hệ thống: " + e.getMessage());
            } finally {
                close(conn);
            }
        }
    }

    private static void rollback(Connection conn) {
        if (conn == null) return;
        try { conn.rollback(); } catch (Exception ignored) {}
    }

    private static void close(Connection conn) {
        if (conn == null) return;
        try { conn.close(); } catch (Exception ignored) {}
    }

    // Kết quả thanh toán
    public static final class SettlementResult {
        public final boolean success;
        public final boolean hadBids;
        public final String  itemId;
        public final String  winnerId;
        public final String  sellerId;
        public final double  winningPrice;
        public final String  errorMessage;

        private SettlementResult(boolean success, boolean hadBids, String itemId,
                                 String winnerId, String sellerId,
                                 double winningPrice, String errorMessage) {
            this.success      = success;
            this.hadBids      = hadBids;
            this.itemId       = itemId;
            this.winnerId     = winnerId;
            this.sellerId     = sellerId;
            this.winningPrice = winningPrice;
            this.errorMessage = errorMessage;
        }

        public static SettlementResult success(String itemId, String winnerId,
                                               String sellerId, double price) {
            return new SettlementResult(true, true, itemId, winnerId, sellerId, price, null);
        }

        public static SettlementResult noBids(String itemId) {
            return new SettlementResult(true, false, itemId, null, null, 0, null);
        }

        public static SettlementResult fail(String reason) {
            return new SettlementResult(false, false, null, null, null, 0, reason);
        }

        @Override
        public String toString() {
            if (!success) return "SettlementResult{FAIL, reason=" + errorMessage + "}";
            if (!hadBids) return "SettlementResult{OK, không có bid, item=" + itemId + "}";
            return String.format("SettlementResult{OK, item=%s, winner=%s, seller=%s, price=%.2f}",
                    itemId, winnerId, sellerId, winningPrice);
        }
    }
}
