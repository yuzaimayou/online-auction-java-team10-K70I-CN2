package com.auction.shared.model.payloads;

public class BidPayload {
    private String itemId;
    private String userId;
    private Double bidPrice;
    private String bidTime;

    public BidPayload(String itemId, String userId, Double bidPrice, String bidTime) {
        this.itemId = itemId;
        this.userId = userId;
        this.bidPrice = bidPrice;
        this.bidTime = bidTime;
    }

    public String getItemId() {
        return itemId;
    }

    public String getUserId() {
        return userId;
    }

    public Double getBidPrice() {
        return bidPrice;
    }

    public String getBidTime() {
        return bidTime;
    }
}

