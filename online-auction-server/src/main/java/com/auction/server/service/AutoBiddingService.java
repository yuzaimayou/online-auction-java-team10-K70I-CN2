package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.AutoBid;
import com.auction.shared.model.auction.BidTransaction;
import com.auction.shared.model.account.User;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * IN-PROGRESS FEATURE
 * Note: This class is not yet wired into the active database-backed socket flow.
 */
public class AutoBiddingService {
    private Map<String, AutoBid> autoBidMap;
    private Map<String, List<AutoBid>> auctionAutoBidsMap;
    private BiddingService biddingService;
    private ReentrantReadWriteLock lock;

    public AutoBiddingService(BiddingService biddingService) {
        this.biddingService = biddingService;
        this.autoBidMap = new HashMap<>();
        this.auctionAutoBidsMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public String registerAutoBid(String auctionId, User bidder, double maxBid, double increment) throws Exception {
        if (bidder == null) {
            throw new IllegalArgumentException("Error: Bidder cannot be null!");
        }

        try {
            biddingService.getAuction(auctionId);
        } catch (Exception e) {
            throw new Exception("Error: Auction not found!");
        }

        if (bidder.getBalance() < maxBid) {
            throw new Exception("Error: Balance (" + bidder.getBalance() +
                    ") less than maxBid (" + maxBid + ")!");
        }

        AutoBid autoBid = new AutoBid(auctionId, bidder.getId(), maxBid, increment);

        lock.writeLock().lock();
        try {
            autoBidMap.put(autoBid.getAutoBidId(), autoBid);
            auctionAutoBidsMap.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(autoBid);
        } finally {
            lock.writeLock().unlock();
        }

        System.out.println("Auto-bid registered: " + autoBid.getAutoBidId() +
                " | Max: " + maxBid + " | Increment: " + increment);
        return autoBid.getAutoBidId();
    }

    public void triggerAutoBids(String auctionId, BiddingService biddingService) throws Exception {
        lock.writeLock().lock();
        try {
            List<AutoBid> autoBids = auctionAutoBidsMap.get(auctionId);
            if (autoBids == null || autoBids.isEmpty()) {
                return;
            }

            Auction auction = biddingService.getAuction(auctionId);
            double currentPrice = auction.getItem().getHighestCurrentPrice();
            String currentWinnerId = auction.getHighestBidderId();

            autoBids.sort(Comparator.comparing(AutoBid::getRegisteredAt));

            boolean hasNewBid;
            int loopGuard = 0;

            do {
                hasNewBid = false;
                for (AutoBid autoBid : autoBids) {
                    if (!autoBid.isActive()) {
                        continue;
                    }

                    if (autoBid.getBidderId().equals(currentWinnerId)) continue;

                    double nextBidAmount = currentPrice + autoBid.getIncrement();

                    if (nextBidAmount > autoBid.getMaxBid()) {
                        autoBid.setActive(false);
                        continue;
                    }

                    try {

                        User bidder = new User(autoBid.getBidderId(), "bidder", "pass");

                        BidTransaction bid = biddingService.placeBid(
                                auctionId, bidder, nextBidAmount);
                        bid.setAutoBid(true);

                        System.out.println("Auto-bid executed: " + autoBid.getAutoBidId() +
                            " | Amount: " + nextBidAmount);

                        currentPrice = nextBidAmount;
                        currentWinnerId = bidder.getId();
                        hasNewBid = true;

                        // Gửi realtime về client
                        biddingService.broadcastNewBid(auctionId, bid);
                        // Delay nhẹ chống spam
                        Thread.sleep(50);


                    } catch (Exception e) {
                        System.out.println("Auto-bid failed: " + e.getMessage());
                        autoBid.setActive(false);
                    }
                }

                loopGuard++;
            } while (hasNewBid && loopGuard < 20); // Loop cạnh tranh + chống loop vô hạn

        } finally {
            lock.writeLock().unlock();
        }
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

    public List<AutoBid> getAutoBidsForAuction(String auctionId) {
        lock.readLock().lock();
        try {
            List<AutoBid> autoBids = auctionAutoBidsMap.get(auctionId);
            return (autoBids != null) ? new ArrayList<>(autoBids) : new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
}