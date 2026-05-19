package com.auction.shared.model.payloads;

// [FIX BUG #7] Đã xóa "import java.util.Timer;" — import thừa, không sử dụng ở đâu trong file này.

public class BidPayload {
    private String itemId;
    private String userId;
    private Double bidPrice;
    private String bidTime;

    public BidPayload() {
    }

    public BidPayload(String itemId, String userId, Double bidPrice, String bidTime) {
        this.itemId = itemId;
        this.userId = userId;
        this.bidPrice = bidPrice;
        this.bidTime = bidTime;
    }

    public BidPayload(String itemId, String userId, Double bidPrice) {
        this.itemId = itemId;
        this.userId = userId;
        this.bidPrice = bidPrice;
        this.bidTime = java.time.LocalDateTime.now().toString();
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