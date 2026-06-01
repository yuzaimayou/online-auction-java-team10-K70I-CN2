package com.auction.server.service.auction;

import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;

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
        List<String> updatedIds = itemRepository.updateStatus();
        if (updatedIds != null) {
            for (String id : updatedIds) {
                try {
                    Item item = itemRepository.findById(id);
                    if (item != null && item.getStatus() == AuctionStatus.ENDED) {
                        LOGGER.info("[Scheduler] Settle auction item: " + id);
                        settlementService.settleAuction(id);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "[Scheduler] Error settling item " + id + ": " + e.getMessage(), e);
                }
            }
        }
    }
}
