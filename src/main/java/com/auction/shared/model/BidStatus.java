package com.auction.shared.model;

public enum BidStatus {
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    OVERRIDDEN("Overridden"),
    AUTO_RETRACTED("AutoRetracted");

    private final String displayName;

    BidStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}