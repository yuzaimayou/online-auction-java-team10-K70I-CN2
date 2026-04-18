package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service gia hạn phiên đấu giá khi có bid phút cuối
 * Requirement 3.2.3: Gia hạn phiên đấu giá (Anti-sniping Algorithm)
 */
public class AntiSnipingService {
    private Map<String, Auction> auctionMap;
    private final int LAST_SECONDS_THRESHOLD = 60;
    private final int EXTENSION_SECONDS = 60;
    private Map<String, LocalDateTime> extensionHistory;

    public AntiSnipingService(Map<String, Auction> auctionMap) {
        this.auctionMap = auctionMap;
        this.extensionHistory = new ConcurrentHashMap<>();
    }

    /**
     * Requirement 3.2.3: Kiểm tra và gia hạn nếu cần
     */
    public void checkAndExtendIfNeeded(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }

        Auction auction = auctionMap.get(auctionId);
        LocalDateTime endTime = auction.getItem().getEndTime();
        LocalDateTime now = LocalDateTime.now();

        long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);

        // Requirement 3.2.3: Nếu có bid trong X giây cuối
        if (secondsRemaining > 0 && secondsRemaining <= LAST_SECONDS_THRESHOLD) {
            BidTransaction lastBid = auction.getLatestBid();
            if (lastBid != null) {
                long secondsSinceBid = ChronoUnit.SECONDS.between(lastBid.getBidTime(), now);

                if (secondsSinceBid <= LAST_SECONDS_THRESHOLD) {
                    extendAuction(auctionId, EXTENSION_SECONDS);
                }
            }
        }
    }

    /**
     * Requirement 3.2.3: Gia hạn phiên đấu giá
     */
    private void extendAuction(String auctionId, int extensionSeconds) throws Exception {
        Auction auction = auctionMap.get(auctionId);
        if (auction == null) {
            throw new Exception("Error: Auction not found!");
        }

        LocalDateTime currentEndTime = auction.getItem().getEndTime();
        LocalDateTime newEndTime = currentEndTime.plusSeconds(extensionSeconds);

        System.out.println("  ANTI-SNIPING TRIGGERED!");
        System.out.println("  Original end time: " + currentEndTime);
        System.out.println("  New end time: " + newEndTime);
        System.out.println("  Extension: " + extensionSeconds + " seconds");

        if (extensionHistory.containsKey(auctionId)) {
            LocalDateTime lastExtension = extensionHistory.get(auctionId);
            long minutesSinceLastExtension = ChronoUnit.MINUTES.between(lastExtension, LocalDateTime.now());

            if (minutesSinceLastExtension < 1) {
                System.out.println(" Extension too frequent - skipped");
                return;
            }
        }

        extensionHistory.put(auctionId, LocalDateTime.now());

        System.out.println(" Auction extended successfully");
    }

    /**
     * Custom anti-sniping setting
     */
    public void setAntiSnipingParameters(int lastSecondsThreshold, int extensionSeconds) {
        if (lastSecondsThreshold <= 0 || extensionSeconds <= 0) {
            throw new IllegalArgumentException("Error: Parameters must be positive!");
        }
    }

    /**
     * Lấy lịch sử gia hạn
     */
    public Map<String, LocalDateTime> getExtensionHistory() {
        return new HashMap<>(extensionHistory);
    }
}