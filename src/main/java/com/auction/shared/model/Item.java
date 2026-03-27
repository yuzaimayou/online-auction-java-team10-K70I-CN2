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
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.highestCurrentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getName() { return name; }
    public double getHighestCurrentPrice() { return highestCurrentPrice; }

    public void setHighestCurrentPrice(double highestCurrentPrice) {
        this.highestCurrentPrice = highestCurrentPrice;
    }

    public String getSellerId() { return sellerId; }

    //Verify if someone is the product owner (to prevent shill bidding)
    public boolean isOwner(String userId) {
        return this.sellerId != null && this.sellerId.equals(userId);
    }

    // Abstract function for printing details (Polymorphism)
    public abstract void printItemDetails();
}