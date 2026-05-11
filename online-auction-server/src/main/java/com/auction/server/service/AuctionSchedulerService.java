package com.auction.server.service;

import com.auction.server.MainServer;
import com.auction.server.repository.ItemRepository;

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
    private final MainServer server;

    public AuctionSchedulerService(MainServer server) {
        this.server = server;
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
        itemRepository.updateStatus();
    }
}