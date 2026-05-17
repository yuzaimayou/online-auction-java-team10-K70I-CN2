package com.auction.shared.model.enums;

/**
 * Requirement 3.1.4: Chuyển trạng thái phiên đấu giá
 * OPEN → RUNNING → FINISHED → PAID / CANCELED
 */
public enum AuctionStatus {
    UPCOMING("Upcoming", "Session has not started yet"),
    ONGOING("Ongoing", "Session is underway"),
    ENDED("Ended", "Session has ended"),
    PAID("Paid", "Paid"),
    CANCELED("Canceled", "Canceled");

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
            return AuctionStatus.valueOf(value.toUpperCase());
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
}