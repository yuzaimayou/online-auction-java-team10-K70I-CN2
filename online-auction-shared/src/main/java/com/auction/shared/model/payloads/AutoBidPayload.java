package com.auction.shared.model.payloads;

/**
 * IN-PROGRESS FEATURE
 * Note: This class is not yet wired into the active database-backed socket flow.
 */
public class AutoBidPayload {
    private String itemId;
    private String userId;
    private Double maxBid;
    private Double increment;

    public AutoBidPayload() {
    }

    public AutoBidPayload(String itemId, String userId, Double maxBid, Double increment) {
        this.itemId = itemId;
        this.userId = userId;
        this.maxBid = maxBid;
        this.increment = increment;
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
}

