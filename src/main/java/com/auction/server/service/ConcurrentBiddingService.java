package com.auction.server.service;

import com.auction.shared.model.account.Bidder;
import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service xử lý đấu giá đồng thời
 * Requirement 3.2.2: Xử lý đấu giá đồng thời (Concurrent Bidding)
 */
public class ConcurrentBiddingService {
    private BiddingService biddingService;
    private final AtomicLong bidCounter = new AtomicLong(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentBiddingService(BiddingService biddingService) {
        this.biddingService = biddingService;
    }

    /**
     * Requirement 3.2.2: Thread-safe placeBid
     * Xử lý: Lost update, Giá bị rollback, Hai người cùng thắng
     */
    public synchronized BidTransaction placeBidSafely(String auctionId, Bidder bidder,
                                                      double bidAmount) throws Exception {
        lock.writeLock().lock();
        try {
            Auction auction = biddingService.getAuction(auctionId);

            double currentPrice = auction.getItem().getHighestCurrentPrice();
            if (bidAmount <= currentPrice) {
                throw new Exception("Error: Bid (" + bidAmount +
                        ") must be greater than current price (" + currentPrice + ")!");
            }

            if (bidder.getBalance() < bidAmount) {
                throw new Exception("Error: Insufficient balance!");
            }

            long bidVersion = bidCounter.incrementAndGet();

            BidTransaction bid = biddingService.placeBid(auctionId, bidder, bidAmount);

            System.out.println("Concurrent bid placed (v" + bidVersion + "): " +
                    bidder.getUsername() + " | " + bidAmount);

            return bid;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Requirement 3.2.2: Xác định người thắng an toàn
     * Đảm bảo không hai người cùng thắng
     */
    public String determineWinnerSafely(String auctionId) throws Exception {
        lock.readLock().lock();
        try {
            Auction auction = biddingService.getAuction(auctionId);

            BidTransaction highestBid = auction.getHighestBid();

            if (highestBid == null) {
                return null;
            }

            System.out.println("Winner determined: " + highestBid.getBidderId() +
                    " | Final price: " + highestBid.getBidAmount());

            return highestBid.getBidderId();

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Lấy current version (bid counter)
     */
    public long getCurrentBidVersion() {
        return bidCounter.get();
    }
}