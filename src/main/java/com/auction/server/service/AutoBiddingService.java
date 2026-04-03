package com.auction.server.service;

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
        lock.readLock().lock();
        try {
            List<AutoBid> autoBids = auctionAutoBidsMap.get(auctionId);
            if (autoBids == null || autoBids.isEmpty()) {
                return;
            }

            Auction auction = biddingService.getAuction(auctionId);
            double currentPrice = auction.getItem().getHighestCurrentPrice();

            autoBids.sort(Comparator.comparing(AutoBid::getRegisteredAt));

            for (AutoBid autoBid : autoBids) {
                if (!autoBid.isActive()) {
                    continue;
                }

                try {
                    double nextBidAmount = autoBid.calculateNextBidAmount(currentPrice);

                    if (nextBidAmount <= currentPrice) {
                        autoBid.setActive(false);
                        System.out.println("Auto-bid reached max limit: " + autoBid.getAutoBidId());
                        continue;
                    }

                    User bidder = new User(autoBid.getBidderId(), "bidder", "pass");
                    bidder.setBalance(autoBid.getMaxBid());

                    BidTransaction bid = biddingService.placeBid(
                            auctionId, bidder, nextBidAmount);
                    bid.setAutoBid(true);

                    System.out.println("Auto-bid executed: " + autoBid.getAutoBidId() +
                            " | Amount: " + nextBidAmount);

                    currentPrice = nextBidAmount;

                } catch (Exception e) {
                    System.out.println("Auto-bid failed: " + e.getMessage());
                    autoBid.setActive(false);
                }
            }
        } finally {
            lock.readLock().unlock();
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