package com.auction.shared.model.item;

import java.time.LocalDateTime;
import com.auction.shared.model.enums.AuctionStatus;

public class ItemSummary {
    private String id;
    private String name;
    private String category;
    private double currentPrice;
    private String thumbnailUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public ItemSummary(String id, String name, String category, double currentPrice, String thumbnailUrl, LocalDateTime startTime, LocalDateTime endTime) {
        this(id, name, category, currentPrice, thumbnailUrl, startTime, endTime,
                AuctionStatus.compute(startTime, endTime));
    }

    public ItemSummary(String id, String name, String category, double currentPrice, String thumbnailUrl, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.thumbnailUrl = thumbnailUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status == null ? AuctionStatus.compute(startTime, endTime) : status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }


    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public AuctionStatus getStatus() {
        return status == null ? AuctionStatus.compute(startTime, endTime) : status;
    }

}
