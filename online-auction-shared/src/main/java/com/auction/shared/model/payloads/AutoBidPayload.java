package com.auction.shared.model.payloads;

/**
 * Socket payload for the production server-backed auto-bid flow.
 *
 * <p>Used by AUTO_BID_REGISTER, GET_AUTO_BID_STATUS, and CANCEL_AUTO_BID.
 * Production bid placement runs through BidService -> AutoBidResolver ->
 * AuctionRoomManager in the server module.</p>
 */
public class AutoBidPayload {
    private String itemId;
    private String userId;
    private Double maxBid;
    private Double increment;
    private Boolean isActive;

    public AutoBidPayload() {
    }

    public AutoBidPayload(String itemId, String userId, Double maxBid, Double increment) {
        this.itemId = itemId;
        this.userId = userId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public AutoBidPayload(String itemId, String userId, Double maxBid, Double increment, Boolean isActive) {
        this.itemId = itemId;
        this.userId = userId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.isActive = isActive;
    }

    public String getItemId() {
        return itemId;
    }

    public String getUserId() {
        return userId;
    }

    public Double getMaxBid() {
        return maxBid;
    }

    public Double getIncrement() {
        return increment;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}

