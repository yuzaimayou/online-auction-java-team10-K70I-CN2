package com.auction.server.service.auction;

import com.auction.server.MainServer;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service tự động kết thúc phiên đấu giá
 * Requirement 3.1.4: Tự động đóng phiên khi hết thời gian
 */
public class AuctionSchedulerService {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final AuctionSettlementService settlementService = new AuctionSettlementService();

    public AuctionSchedulerService(MainServer server) {
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndUpdateStatuses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void checkAndUpdateStatuses() {
        List<String> updatedIds = itemRepository.updateStatus();
        if (updatedIds != null) {
            for (String id : updatedIds) {
                try {
                    Item item = itemRepository.findById(id);
                    if (item != null && item.getStatus() == AuctionStatus.ENDED) {
                        System.out.println("[Scheduler] Settle auction item: " + id);
                        settlementService.settleAuction(id);
                    }
                } catch (Exception e) {
                    System.err.println("[Scheduler] Error settling item " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}