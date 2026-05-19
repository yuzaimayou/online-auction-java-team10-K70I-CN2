package com.auction.shared.model.payloads;

public class AuctionExtendedPayload {
    private String itemId;
    private String newEndTime;

    public AuctionExtendedPayload() {}

    public AuctionExtendedPayload(String itemId, String newEndTime) {
        this.itemId = itemId;
        this.newEndTime = newEndTime;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getNewEndTime() {
        return newEndTime;
    }

    public void setNewEndTime(String newEndTime) {
        this.newEndTime = newEndTime;
    }
}
