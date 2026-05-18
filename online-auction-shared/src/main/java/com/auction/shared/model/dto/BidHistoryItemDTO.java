package com.auction.shared.model.dto;

import java.time.LocalDateTime;

public class BidHistoryItemDTO {
    public String itemId, userName;
    public double bidPrice;
    public LocalDateTime bidTime;

    public BidHistoryItemDTO(String itemId, String userName, double bidPrice, LocalDateTime bidTime) {
        this.itemId = itemId;
        this.userName = userName;
        this.bidPrice = bidPrice;
        this.bidTime = bidTime;
    }

}
