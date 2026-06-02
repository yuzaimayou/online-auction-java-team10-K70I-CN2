package com.auction.server.service.auction;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service tự động kết thúc phiên đấu giá
 * Requirement 3.1.4: Tự động đóng phiên khi hết thời gian
 */
public class AuctionSchedulerService {
    private static final Logger LOGGER = Logger.getLogger(AuctionSchedulerService.class.getName());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final AuctionSettlementService settlementService = new AuctionSettlementService();

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndUpdateStatuses();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Auction scheduler tick failed", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void checkAndUpdateStatuses() {
        itemRepository.updateStatus();
        List<String> expiredIds = itemRepository.findOngoingExpiredItemIds();
        if (expiredIds != null) {
            for (String id : expiredIds) {
                if (markExpiredAuctionEnded(id)) {
                    LOGGER.info("[Scheduler] Settle auction item: " + id);
                    settlementService.settleAuction(id);
                }
            }
        }
    }

    private boolean markExpiredAuctionEnded(String itemId) {
        synchronized (AuctionLockManager.getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    LocalDateTime now = LocalDateTime.now();
                    Item item = itemRepository.findById(conn, itemId);
                    if (item == null
                            || item.getStoredStatus() != AuctionStatus.ONGOING
                            || item.getEndTime().isAfter(now)) {
                        conn.rollback();
                        return false;
                    }

                    if (!itemRepository.markEndedIfStillExpired(conn, itemId, now)) {
                        conn.rollback();
                        return false;
                    }

                    conn.commit();
                    return true;
                } catch (Exception e) {
                    rollbackQuietly(conn);
                    LOGGER.log(Level.SEVERE, "[Scheduler] Error ending item " + itemId + ": " + e.getMessage(), e);
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "[Scheduler] Failed to open DB connection for item " + itemId, e);
                return false;
            }
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "[Scheduler] Rollback failed", e);
        }
    }
}
