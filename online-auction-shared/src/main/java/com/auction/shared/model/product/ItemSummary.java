package com.auction.shared.model.product;

import java.time.LocalDateTime;

public class ItemSummary {
    private String id;
    private String name;
    private double currentPrice;
    private String thumbnailUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ItemSummary(String id, String name, double currentPrice, String thumbnailUrl, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.thumbnailUrl = thumbnailUrl;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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

}
