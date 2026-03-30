package com.auction.shared.model;

/**
 * Requirement 3.1.4: Chuyển trạng thái phiên đấu giá
 * OPEN → RUNNING → FINISHED → PAID / CANCELED
 */
public enum AuctionStatus {
    OPEN("Open", "Session has not started yet"),
    RUNNING("Running", "Session is underway"),
    FINISHED("Finished", "Session has ended"),
    PAID("Paid", "Paid"),
    CANCELED("Canceled", "Canceled");

    private final String displayName;
    private final String description;

    AuctionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    @Override
    public String toString() { return displayName; }
}