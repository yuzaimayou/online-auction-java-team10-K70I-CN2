package com.auction.shared.model.item;

import com.auction.shared.model.enums.AuctionStatus;
import java.time.LocalDateTime;

public class MyBidSummary {
    private String itemId;
    private String itemName;
    private String thumbnailUrl;
    private double currentPrice;
    private double myHighestBid;
    private boolean isWinner;
    private AuctionStatus status;
    private LocalDateTime endTime;

    public MyBidSummary(String itemId, String itemName, String thumbnailUrl,
                        double currentPrice, double myHighestBid,
                        boolean isWinner, AuctionStatus status, LocalDateTime endTime) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.thumbnailUrl = thumbnailUrl;
        this.currentPrice = currentPrice;
        this.myHighestBid = myHighestBid;
        this.isWinner = isWinner;
        this.status = status;
        this.endTime = endTime;
    }

    public String getItemId()         { return itemId; }
    public String getItemName()       { return itemName; }
    public String getThumbnailUrl()   { return thumbnailUrl; }
    public double getCurrentPrice()   { return currentPrice; }
    public double getMyHighestBid()   { return myHighestBid; }
    public boolean isWinner()         { return isWinner; }
    public AuctionStatus getStatus()  { return status; }
    public LocalDateTime getEndTime() { return endTime; }
}