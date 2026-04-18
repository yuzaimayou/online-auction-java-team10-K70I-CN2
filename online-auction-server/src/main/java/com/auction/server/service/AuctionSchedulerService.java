package com.auction.server.service;

import com.auction.shared.model.auction.Auction;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service tự động kết thúc phiên đấu giá
 * Requirement 3.1.4: Tự động đóng phiên khi hết thời gian
 */
public class AuctionSchedulerService {
    private Map<String, Auction> auctionMap;
    private ScheduledExecutorService scheduler;
    private List<AuctionEndListener> listeners;

    public interface AuctionEndListener {
        void onAuctionEnded(Auction auction);
    }

    public AuctionSchedulerService(Map<String, Auction> auctionMap) {
        this.auctionMap = auctionMap;
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.listeners = new ArrayList<>();
    }

    /**
     * Đăng ký listener để lắng nghe khi phiên kết thúc
     */
    public void addListener(AuctionEndListener listener) {
        listeners.add(listener);
    }

    /**
     * Requirement 3.1.4: Lên lịch kết thúc tự động
     */
    public void scheduleAuctionEnd(String auctionId) {
        if (!auctionMap.containsKey(auctionId)) {
            System.err.println("Auction " + auctionId + " not found!");
            return;
        }

        Auction auction = auctionMap.get(auctionId);
        LocalDateTime endTime = auction.getItem().getEndTime();
        LocalDateTime now = LocalDateTime.now();

        // [ERROR HANDLING] Kiểm tra end time
        if (endTime.isBefore(now)) {
            System.err.println("Auction end time has already passed!");
            return;
        }

        // Tính thời gian chờ (giây)
        long delaySeconds = java.time.temporal.ChronoUnit.SECONDS.between(now, endTime);

        System.out.println("✓ Scheduled end: " + auctionId + " in " + delaySeconds + "s");

        // Lên lịch kết thúc
        scheduler.schedule(() -> {
            try {
                auction.endAuction();
                notifyListeners(auction);
                System.out.println("Auto-ended auction: " + auctionId);
            } catch (Exception e) {
                System.err.println("Error ending auction: " + e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Kết thúc ngay lập tức
     */
    public void endNow(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }

        Auction auction = auctionMap.get(auctionId);
        auction.endAuction();
        notifyListeners(auction);
    }

    /**
     * Thông báo đến listeners
     */
    private void notifyListeners(Auction auction) {
        for (AuctionEndListener listener : listeners) {
            listener.onAuctionEnded(auction);
        }
    }

    /**
     * Tắt scheduler
     */
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
}