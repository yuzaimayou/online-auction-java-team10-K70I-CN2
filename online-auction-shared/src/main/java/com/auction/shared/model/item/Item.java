package com.auction.shared.model.item;

import com.auction.shared.model.base.Entity;
import com.auction.shared.model.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Item extends Entity {

    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String sellerId;
    private String category;
    private double bidStep;
    private List<String> imagesPath;
    private String currentBidderId;
    private AuctionStatus status;
    private LocalDateTime create_at;
    private double myLastBid = 0.0;

    public Item(String name, String description,
                double startingPrice,
                LocalDateTime startTime,
                LocalDateTime endTime,
                String sellerId,
                String category,
                double bidStep,
                List<String> imagesPath) {

        super(UUID.randomUUID().toString());
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Item name cannot be null or empty!");
        }
        if (description == null) {
            throw new IllegalArgumentException("Error: Description cannot be null!");
        }
        if (startingPrice < 0) {
            throw new IllegalArgumentException("Error: Starting price cannot be negative!");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Error: Start time and end time cannot be null!");
        }

        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("Error: Start time cannot be in the past!");
        }
        if (endTime.isBefore(now)) {
            throw new IllegalArgumentException("Error: End time cannot be in the past!");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Error: End time must be strictly after the start time!");
        }
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Seller ID cannot be null or empty!");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Category cannot be null or empty!");
        }
        if (bidStep <= 0) {
            throw new IllegalArgumentException("Error: Bid step must be greater than 0!");
        }
        if (bidStep > startingPrice) {
            throw new IllegalArgumentException("Error: Bid step cannot be greater than starting price!");
        }
        if (imagesPath == null || imagesPath.isEmpty()) {
            throw new IllegalArgumentException("Error: Image path cannot be null or empty!");
        }

        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.category = category;
        this.bidStep = bidStep;
        this.imagesPath = imagesPath;
        this.create_at = LocalDateTime.now();
        this.status = AuctionStatus.compute(startTime, endTime);
    }

    public Item(String name, String description,
                double startingPrice, double currentPrice,
                LocalDateTime startTime, LocalDateTime endTime,
                String sellerId,
                String category,
                double bidStep,
                List<String> imagesPath) {

        super(UUID.randomUUID().toString());
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.category = category;
        this.bidStep = bidStep;
        this.imagesPath = imagesPath;
    }

    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        if (currentPrice < this.startingPrice) {
            throw new IllegalArgumentException("Error: Current price cannot be lower than the starting price!");
        }
        this.currentPrice = currentPrice;
    }

    public double getHighestCurrentPrice() {
        return getCurrentPrice();
    }

    public void setHighestCurrentPrice(double highestCurrentPrice) {
        setCurrentPrice(highestCurrentPrice);
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getCategory() {
        return category;
    }

    public double getBidStep() {
        return bidStep;
    }

    public List<String> getImagesPath() {
        return imagesPath;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("Error: End time cannot be null!");
        }
        this.endTime = endTime;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOwner(String userId) {
        return sellerId != null && sellerId.equals(userId);
    }

    public String getCurrentBidderId() {
        return currentBidderId;
    }

    public void setCurrentBidderId(String playerId) {
        this.currentBidderId = playerId;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreate_at() {
        return create_at;
    }

    public double getMyLastBid() { return myLastBid; }

    public void setMyLastBid(double myLastBid) { this.myLastBid = myLastBid; }

    public void printItemDetails() {
        System.out.println("Item ID: " + getId());
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Starting price: " + startingPrice);
        System.out.println("Current price: " + currentPrice);
        System.out.println("Seller: " + sellerId);
        System.out.println("Category: " + category);
        System.out.println("Bid step: " + bidStep);
        System.out.println("Image path: " + imagesPath);
        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);
    }

    // getStatus() trả về AuctionStatus - ưu tiên BANNED từ DB, còn lại tính động
    public AuctionStatus getStatus() {
        if (this.status == AuctionStatus.BANNED) {
            return AuctionStatus.BANNED;
        }
        return AuctionStatus.compute(startTime, endTime);
    }

    public AuctionStatus getStoredStatus() {
        return status;
    }
}