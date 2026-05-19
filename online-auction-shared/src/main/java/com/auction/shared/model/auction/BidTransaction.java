package com.auction.shared.model.auction;

import java.time.LocalDateTime;

/**
 * Lưu trữ thông tin từng lần đặt giá
 * Requirement 3.1.3: Tham gia đấu giá
 */
/**
 * Shared bid-history row model.
 *
 * <p>Production bid placement is handled by BidService -> AutoBidResolver ->
 * AuctionRoomManager in the server module. This class is still used by the
 * client bid-history UI and by prototype/demo auction classes, so it is not
 * deprecated.</p>
 */
public class BidTransaction {
    private String transactionId;
    private String auctionId;
    private String bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;
    private boolean isAutoBid;

    public BidTransaction(String transactionId, String auctionId, String bidderId,
                          double bidAmount, LocalDateTime bidTime) {
        // [ERROR HANDLING] Validate all parameters
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Transaction ID cannot be empty!");
        }
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Auction ID cannot be empty!");
        }
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Bidder ID cannot be empty!");
        }
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Error: Bid amount must be positive!");
        }
        if (bidTime == null) {
            throw new IllegalArgumentException("Error: Bid time cannot be null!");
        }

        this.transactionId = transactionId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isAutoBid = false;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public boolean isAutoBid() {
        return isAutoBid;
    }

    public void setAutoBid(boolean autoBid) {
        isAutoBid = autoBid;
    }

    @Override
    public String toString() {
        return "BidTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                ", isAutoBid=" + isAutoBid +
                '}';
    }
}
