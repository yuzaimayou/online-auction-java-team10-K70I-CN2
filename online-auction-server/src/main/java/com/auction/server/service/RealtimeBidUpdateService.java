package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * IN-PROGRESS FEATURE
 * Note: This class is not yet wired into the active database-backed socket flow.
 *
 * Service cập nhật realtime khi có bid mới
 * Requirement 3.2.4: Realtime Update (Observer Pattern)
 */
public class RealtimeBidUpdateService {
    private Map<String, List<BidObserver>> observerMap;

    /**
     * Observer interface - Requirement 3.2.4
     */
    public interface BidObserver {
        void onNewBid(Auction auction, BidTransaction bid);
        void onPriceChanged(Auction auction, double newPrice);
        void onAuctionEnded(Auction auction);
    }

    public RealtimeBidUpdateService() {
        this.observerMap = new HashMap<>();
    }

    /**
     * Requirement 3.2.4: Đăng ký observer
     */
    public synchronized void addObserver(String auctionId, BidObserver observer) {
        if (auctionId == null || observer == null) {
            throw new IllegalArgumentException("Error: Auction ID and observer cannot be null!");
        }

        observerMap.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                .add(observer);

        System.out.println(" Observer registered for auction: " + auctionId);
    }

    /**
     * Gỡ đăng ký observer
     */
    public synchronized void removeObserver(String auctionId, BidObserver observer) {
        if (observerMap.containsKey(auctionId)) {
            observerMap.get(auctionId).remove(observer);
        }
    }

    /**
     * Requirement 3.2.4: Thông báo bid mới cho tất cả observers
     * Thread-safe notify
     */
    public void notifyNewBid(Auction auction, BidTransaction bid) {
        String auctionId = auction.getAuctionId();

        if (!observerMap.containsKey(auctionId)) {
            return;
        }

        List<BidObserver> observers = observerMap.get(auctionId);

        for (BidObserver observer : observers) {
            try {
                observer.onNewBid(auction, bid);
            } catch (Exception e) {
                System.err.println(" Error notifying observer: " + e.getMessage());
            }
        }

        System.out.println(" Notified " + observers.size() + " observers about new bid");
    }

    /**
     * Requirement 3.2.4: Thông báo giá thay đổi
     * Requirement 3.2.5: Real-time Price Curve update
     */
    public void notifyPriceChanged(Auction auction, double newPrice) {
        String auctionId = auction.getAuctionId();

        if (!observerMap.containsKey(auctionId)) {
            return;
        }

        List<BidObserver> observers = observerMap.get(auctionId);

        for (BidObserver observer : observers) {
            try {
                observer.onPriceChanged(auction, newPrice);
            } catch (Exception e) {
                System.err.println(" Error notifying price change: " + e.getMessage());
            }
        }

        System.out.println(" Price change notified to " + observers.size() + " observers");
    }

    /**
     * Thông báo auction kết thúc
     */
    public void notifyAuctionEnded(Auction auction) {
        String auctionId = auction.getAuctionId();

        if (!observerMap.containsKey(auctionId)) {
            return;
        }

        List<BidObserver> observers = new ArrayList<>(observerMap.get(auctionId));

        for (BidObserver observer : observers) {
            try {
                observer.onAuctionEnded(auction);
            } catch (Exception e) {
                System.err.println("Error notifying auction end: " + e.getMessage());
            }
        }

        observerMap.remove(auctionId);
        System.out.println(" Auction ended - notified " + observers.size() + " observers");
    }

    /**
     * Lấy số lượng observers
     */
    public int getObserverCount(String auctionId) {
        return observerMap.getOrDefault(auctionId, new ArrayList<>()).size();
    }
}