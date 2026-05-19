package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: Production anti-sniping logic currently lives inline inside {@link BidService}.
 * This class is reserved for a planned future extraction to separate anti-sniping 
 * concerns from the main bidding flow.
 *
 * Currently, it contains an in-progress helper for the anti-sniping algorithm.
 * 
 * Service gia hạn phien dau gia khi co bid phut cuoi
 * Requirement 3.2.3: Gia han phien dau gia (Anti-sniping Algorithm)
 */
public class AntiSnipingService {
    private final Map<String, Auction> auctionMap;
    private final int LAST_SECONDS_THRESHOLD = 60;
    private final int EXTENSION_SECONDS = 60;
    private final Map<String, LocalDateTime> extensionHistory;

    public AntiSnipingService(Map<String, Auction> auctionMap) {
        this.auctionMap = auctionMap;
        this.extensionHistory = new ConcurrentHashMap<>();
    }

    /**
     * Goi ngay khi bid moi thanh cong
     */
    public synchronized void processNewBid(String auctionId, BidTransaction newBid) throws Exception {
        Auction auction = auctionMap.get(auctionId);
        if (auction == null) {
            throw new Exception("Error: Auction not found!");
        }

        LocalDateTime endTime = auction.getItem().getEndTime();
        LocalDateTime bidTime = newBid.getBidTime();

        long secondsBeforeEnd = ChronoUnit.SECONDS.between(bidTime, endTime);

        if (secondsBeforeEnd > 0 && secondsBeforeEnd <= LAST_SECONDS_THRESHOLD) {
            extendAuction(auction, EXTENSION_SECONDS);
        }
    }

    /**
     * Gia han phien dau gia
     */
    private void extendAuction(Auction auction, int extensionSeconds) {
        String auctionId = auction.getAuctionId();
        LocalDateTime currentEndTime = auction.getItem().getEndTime();
        LocalDateTime newEndTime = currentEndTime.plusSeconds(extensionSeconds);

        // Quan trong: cap nhat endTime vao object de he thong thay doi thuc te
        auction.getItem().setEndTime(newEndTime);

        extensionHistory.put(auctionId, LocalDateTime.now());

        System.out.println("  ANTI-SNIPING TRIGGERED!");
        System.out.println("  Original end time: " + currentEndTime);
        System.out.println("  New end time: " + newEndTime);
        System.out.println("  Extension: " + extensionSeconds + " seconds");
        System.out.println("  Auction extended successfully");
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
     * Lay lich su gia han
     */
    public Map<String, LocalDateTime> getExtensionHistory() {
        return new HashMap<>(extensionHistory);
    }
}
