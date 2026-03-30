package com.auction.shared.model;

import java.time.LocalDateTime;

public class Item extends Entity {

    private String name;
    private String description;
    private double startingPrice;
    private double highestCurrentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String sellerId;

    // Constructor khi tạo item mới
    public Item(String id, String name, String description,
                double startingPrice,
                LocalDateTime startTime,
                LocalDateTime endTime,
                String sellerId) {

        super(id);

        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Item name cannot be empty");

        if (description == null)
            throw new IllegalArgumentException("Description cannot be null");

        if (startingPrice < 0)
            throw new IllegalArgumentException("Starting price cannot be negative");

        if (startTime == null || endTime == null)
            throw new IllegalArgumentException("Start time and end time cannot be null");

        if (endTime.isBefore(startTime))
            throw new IllegalArgumentException("End time must be after start time");

        if (sellerId == null || sellerId.trim().isEmpty())
            throw new IllegalArgumentException("Seller id cannot be empty");

        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.highestCurrentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    // Constructor khi load từ database
    public Item(String id, String name, String description,
                double startingPrice, double highestCurrentPrice,
                LocalDateTime startTime, LocalDateTime endTime,
                String sellerId) {

        super(id);

        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.highestCurrentPrice = highestCurrentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getHighestCurrentPrice() {
        return highestCurrentPrice;
    }

    public void setHighestCurrentPrice(double highestCurrentPrice) {
        if (highestCurrentPrice < startingPrice)
            throw new IllegalArgumentException("Current price cannot be lower than starting price");

        this.highestCurrentPrice = highestCurrentPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getSellerId() {
        return sellerId;
    }

    public boolean isOwner(String userId) {
        return sellerId != null && sellerId.equals(userId);
    }

    public void printItemDetails() {

        System.out.println("Item ID: " + getId());
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Starting price: " + startingPrice);
        System.out.println("Current price: " + highestCurrentPrice);
        System.out.println("Seller: " + sellerId);
        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);
    }
}