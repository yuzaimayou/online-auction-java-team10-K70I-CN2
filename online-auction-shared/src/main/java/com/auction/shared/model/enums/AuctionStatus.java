package com.auction.shared.model.enums;

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

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
