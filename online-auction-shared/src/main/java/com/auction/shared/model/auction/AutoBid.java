package com.auction.shared.model.auction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model để lưu trữ thông tin auto-bid
 * Requirement 3.2.1: Auto-Bidding
 */
/**
 * Prototype/demo class only. Not part of the production bidding flow.
 *
 * <p>The production bid placement flow is BidService -> AutoBidResolver ->
 * AuctionRoomManager in the server module. Production auto-bid config is stored
 * in the database-backed auto_bids table and exchanged with AutoBidPayload.
 * This model is kept for the historical in-memory prototype.</p>
 *
 * @deprecated Use the database-backed auto_bids table and AutoBidPayload.
 */
@Deprecated
public class AutoBid {
    private String autoBidId;
    private String auctionId;
    private String bidderId;
    private double maxBid;
    private double increment;
    private LocalDateTime registeredAt;
    private boolean isActive;

    public AutoBid(String auctionId, String bidderId, double maxBid, double increment) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Auction ID cannot be empty!");
        }
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Bidder ID cannot be empty!");
        }
        if (maxBid <= 0) {
            throw new IllegalArgumentException("Error: Max bid must be positive!");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("Error: Increment must be positive!");
        }

        this.autoBidId = UUID.randomUUID().toString();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
        this.isActive = true;
    }

    public String getAutoBidId() {
        return autoBidId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Requirement 3.2.1: Tính giá auto-bid tiếp theo
     * Không vượt quá maxBid
     */
    public double calculateNextBidAmount(double currentPrice) throws Exception {
        if (currentPrice < 0) {
            throw new Exception("Error: Current price cannot be negative!");
        }

        double suggestedBid = currentPrice + increment;

        if (suggestedBid > maxBid) {
            return maxBid;
        }

        return suggestedBid;
    }

    @Override
    public String toString() {
        return "AutoBid{" +
                "id='" + autoBidId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", maxBid=" + maxBid +
                ", increment=" + increment +
                ", registeredAt=" + registeredAt +
                ", isActive=" + isActive +
                '}';
    }
}
