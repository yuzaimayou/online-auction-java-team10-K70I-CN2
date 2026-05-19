package com.auction.shared.model.enums;

import java.time.LocalDateTime;

public enum AuctionStatus {
    UPCOMING("UPCOMING", "Item auction has not started yet"),
    ONGOING("ONGOING", "Item auction is currently open for bidding"),
    ENDED("ENDED", "Item auction has finished"),
    BANNED("BANNED", "Item is blocked/disabled and cannot be bid");

    private final String displayName;
    private final String description;

    AuctionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AuctionStatus fromString(String value) {
        if (value == null) {
            return UPCOMING;
        }

        try {
            return AuctionStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return UPCOMING;
        }
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static AuctionStatus compute(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return ENDED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return UPCOMING;
        }
        if (now.isAfter(endTime) || now.isEqual(endTime)) {
            return ENDED;
        }
        return ONGOING;
    }
}
