package com.auction.shared.model.item;

import com.auction.shared.constant.ItemStatusConstants;

import java.time.LocalDateTime;

public class ItemSummary {
    private String id;
    private String name;
    private String category;
    private double currentPrice;
    private String thumbnailUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ItemSummary(String id, String name, String category, double currentPrice, String thumbnailUrl, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.category = category;
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

    public String getStatus() {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startTime)) {
            return ItemStatusConstants.UPCOMING;
        } else if (now.isAfter(endTime)) {
            return ItemStatusConstants.ENDED;
        } else {
            return ItemStatusConstants.ONGOING;
        }
    }

}
