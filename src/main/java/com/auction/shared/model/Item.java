package com.auction.shared.model;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double highestCurrentPrice;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected String sellerId;

    public Item(String id, String name, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, String sellerId) {
        super(id); // Entity class will handle ID validation

        //Validate item name
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Item name cannot be null or empty!");
        }

        //Validate description
        if (description == null) {
            throw new IllegalArgumentException("Error: Description cannot be null!");
        }

        // [ERROR HANDLING] Validate starting price
        if (startingPrice < 0) {
            throw new IllegalArgumentException("Error: Starting price cannot be negative!");
        }

        // [ERROR HANDLING] Validate time constraints
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Error: Start time and end time cannot be null!");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Error: End time must be strictly after the start time!");
        }

        // [ERROR HANDLING] Validate seller ID
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Seller ID cannot be null or empty!");
        }

        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.highestCurrentPrice = startingPrice; // Initial highest price is the starting price
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public double getHighestCurrentPrice() {
        return highestCurrentPrice;
    }

    public void setHighestCurrentPrice(double highestCurrentPrice) {
        // [ERROR HANDLING] Ensure new price is not lower than the starting price
        if (highestCurrentPrice < this.startingPrice) {
            throw new IllegalArgumentException("Error: Highest current price cannot be lower than the starting price!");
        }
        this.highestCurrentPrice = highestCurrentPrice;
    }

    public String getSellerId() {
        return sellerId;
    }

    // Getters for time (needed for auction time validation)
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getDescription() {
        return description;
    }

    // Verify if someone is the product owner (to prevent shill bidding)
    public boolean isOwner(String userId) {
        return this.sellerId != null && this.sellerId.equals(userId);
    }

    // Abstract function for printing details (Polymorphism)
    public abstract void printItemDetails();
}