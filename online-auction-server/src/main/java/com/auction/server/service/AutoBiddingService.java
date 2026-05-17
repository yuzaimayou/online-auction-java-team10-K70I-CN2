package com.auction.server.service;

import com.auction.server.repository.UserRepository;
import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.AutoBid;
import com.auction.shared.model.auction.BidTransaction;
import com.auction.shared.model.account.User;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AutoBiddingService {

    private Map<String, AutoBid> autoBidMap;
    private Map<String, List<AutoBid>> auctionAutoBidsMap;
    private BiddingService biddingService;
    private UserRepository userRepository;
    private ReentrantReadWriteLock lock;

    public AutoBiddingService(BiddingService biddingService, UserRepository userRepository) {
        this.biddingService = biddingService;
        this.userRepository = userRepository;
        this.autoBidMap = new HashMap<>();
        this.auctionAutoBidsMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public String registerAutoBid(String auctionId, String bidderId,
                                  double maxBid, double increment) throws Exception {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("Bidder ID cannot be null or blank.");
        }

        Auction auction;
        try {
            auction = biddingService.getAuction(auctionId);
        } catch (Exception e) {
            throw new Exception("Error: Auction not found!");
        }

        double currentPrice = auction.getItem().getHighestCurrentPrice();

        if (increment < auction.getItem().getBidStep()) {
            throw new Exception(String.format(
                    "Your step must be at least %.0f", auction.getItem().getBidStep()));
        }
        if (increment >= maxBid) {
            throw new Exception(String.format(
                    "Increment (%.0f) must be less than maxBid (%.0f).", increment, maxBid));
        }
        if (maxBid <= currentPrice) {
            throw new Exception(String.format(
                    "maxBid (%.0f) must be greater than current price (%.0f).", maxBid, currentPrice));
        }
        if (currentPrice + increment > maxBid) {
            throw new Exception(String.format(
                    "First auto-bid (%.0f) would exceed maxBid (%.0f). Raise maxBid or lower increment.",
                    currentPrice + increment, maxBid));
        }

        User bidder = userRepository.findById(bidderId);
        if (bidder == null) {
            throw new Exception("Error: Bidder not found!");
        }
        if (bidder.getBalance() < maxBid) {
            throw new Exception(String.format(
                    "Insufficient balance (%.0f) for maxBid (%.0f).", bidder.getBalance(), maxBid));
        }

        AutoBid autoBid = new AutoBid(auctionId, bidderId, maxBid, increment);

        lock.writeLock().lock();
        try {
            cancelExistingAutoBidForUser(auctionId, bidderId);

            autoBidMap.put(autoBid.getAutoBidId(), autoBid);
            auctionAutoBidsMap
                    .computeIfAbsent(auctionId, k -> new ArrayList<>())
                    .add(autoBid);
        } finally {
            lock.writeLock().unlock();
        }

        System.out.printf("Auto-bid registered: %s | auth=%s | max=%.0f | inc=%.0f%n",
                autoBid.getAutoBidId(), bidderId, maxBid, increment);

        triggerAutoBids(auctionId, this.biddingService);

        return autoBid.getAutoBidId();
    }

    public void triggerAutoBids(String auctionId, BiddingService biddingService) throws Exception {
        // Collect bids to place; actual placeBid calls happen outside the lock to avoid
        // holding it during I/O or sleep.
        List<AutoBid> snapshot;
        lock.writeLock().lock();
        try {
            List<AutoBid> autoBids = auctionAutoBidsMap.get(auctionId);
            if (autoBids == null || autoBids.isEmpty()) return;
            snapshot = new ArrayList<>(autoBids);
            snapshot.sort(Comparator.comparing(AutoBid::getRegisteredAt));
        } finally {
            lock.writeLock().unlock();
        }

        Auction auction = biddingService.getAuction(auctionId);
        double currentPrice = auction.getItem().getHighestCurrentPrice();
        String currentWinnerId = auction.getHighestBidderId();

        boolean hasNewBid;
        int loopGuard = 0;

        do {
            hasNewBid = false;

            // [FIX BUG #11] Trước đây toàn bộ vòng lặp (kể cả Thread.sleep) nằm trong writeLock().
            // Điều này chặn tất cả thread khác trong suốt 50ms * số vòng lặp.
            // Nay: chỉ dùng lock khi đọc/ghi state của autoBidMap/auctionAutoBidsMap,
            // còn placeBid() và Thread.sleep() thực hiện ngoài lock.
            for (AutoBid autoBid : snapshot) {
                boolean isActive;
                lock.readLock().lock();
                try {
                    isActive = autoBid.isActive();
                } finally {
                    lock.readLock().unlock();
                }

                if (!isActive) continue;
                if (autoBid.getBidderId().equals(currentWinnerId)) continue;

                double nextBidAmount = currentPrice + autoBid.getIncrement();

                if (nextBidAmount > autoBid.getMaxBid()) {
                    lock.writeLock().lock();
                    try {
                        autoBid.setActive(false);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    System.out.printf("Auto-bid deactivated (max reached): %s | auth=%s%n",
                            autoBid.getAutoBidId(), autoBid.getBidderId());
                    continue;
                }

                User bidder = userRepository.findById(autoBid.getBidderId());
                if (bidder == null) {
                    lock.writeLock().lock();
                    try {
                        autoBid.setActive(false);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    System.err.println("Auto-bid deactivated: auth not found → " + autoBid.getBidderId());
                    continue;
                }

                if (bidder.getBalance() < nextBidAmount) {
                    lock.writeLock().lock();
                    try {
                        autoBid.setActive(false);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    System.err.printf("Auto-bid deactivated (insufficient balance): auth=%s | need=%.0f | have=%.0f%n",
                            bidder.getId(), nextBidAmount, bidder.getBalance());
                    continue;
                }

                try {
                    // placeBid() is called outside any lock to avoid deadlock / long lock hold.
                    BidTransaction bid = biddingService.placeBid(auctionId, bidder, nextBidAmount);
                    bid.setAutoBid(true);

                    System.out.printf("Auto-bid executed: %s | auth=%s | amount=%.0f%n",
                            autoBid.getAutoBidId(), bidder.getId(), nextBidAmount);

                    currentPrice = nextBidAmount;
                    currentWinnerId = bidder.getId();
                    hasNewBid = true;

                    biddingService.broadcastNewBid(auctionId, bid);

                    // [FIX BUG #11] Thread.sleep() nằm ngoài writeLock — không còn chặn lock.
                    Thread.sleep(50);

                } catch (Exception e) {
                    lock.writeLock().lock();
                    try {
                        autoBid.setActive(false);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    System.out.println("Auto-bid deactivated (placeBid failed): " + e.getMessage());
                }
            }

            loopGuard++;
        } while (hasNewBid && loopGuard < 20);
    }

    public void cancelAutoBid(String autoBidId) throws Exception {
        lock.writeLock().lock();
        try {
            if (!autoBidMap.containsKey(autoBidId)) {
                throw new Exception("Error: Auto-bid not found!");
            }

            AutoBid autoBid = autoBidMap.get(autoBidId);
            autoBid.setActive(false);
            autoBidMap.remove(autoBidId);

            System.out.println("Auto-bid cancelled: " + autoBidId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cancelExistingAutoBidForUser(String auctionId, String bidderId) {
        List<AutoBid> autoBids = auctionAutoBidsMap.get(auctionId);
        if (autoBids == null) return;
        for (AutoBid existing : autoBids) {
            if (existing.getBidderId().equals(bidderId) && existing.isActive()) {
                existing.setActive(false);
                autoBidMap.remove(existing.getAutoBidId());
                System.out.printf("Previous auto-bid cancelled for re-register: %s | auth=%s%n",
                        existing.getAutoBidId(), bidderId);
            }
        }
    }
}