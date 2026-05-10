package com.auction.shared.model.account;

import com.auction.shared.model.item.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User extends Person {
    protected double balance;
    protected double frozenBalance;  // Money held against current highest bid
    protected double rating; // Seller reputation score (from 1.0 to 5.0)
    protected List<Double> reviewScores; // Keep track of all reviews
    protected String email;
    protected boolean isVerify;

    public User(String id, String username, String password) {

        super(id, username, password);
        this.role = "User"; // Set specific role
        this.balance = 0.0;
        this.rating = 5.0; // New users start with perfect rating
        this.reviewScores = new ArrayList<>();
        this.email = null;
        this.isVerify = true;
    }

    public User(String id, String username, String password, String email, boolean isEnable) {
        this(id, username, password);
        this.email = email;
        this.isVerify = isEnable;
    }

    // --- Specific Getters & Setters ---
    @Override
    public String getUsername() {
        return super.getUsername();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        // Prevent negative balance
        if (balance < 0) {
            throw new IllegalArgumentException("Error: Balance cannot be negative!");
        }
        this.balance = balance;
    }

    public double getFrozenBalance() {
        return frozenBalance;
    }

    public void setFrozenBalance(double frozenBalance) {
        if (frozenBalance < 0) {
            throw new IllegalArgumentException("Error: Frozen balance cannot be negative!");
        }
        this.frozenBalance = frozenBalance;
    }

    /** Returns the spendable balance (excludes frozen funds). */
    public double getAvailableBalance() {
        return balance;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        // [ERROR HANDLING] Validate rating range
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Error: Rating must be between 1.0 and 5.0!");
        }
        this.rating = rating;
    }

    public List<Double> getReviewScores() {
        return new ArrayList<>(reviewScores);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = (email == null) ? null : email.trim();
    }

    public boolean isVerify() {
        return isVerify;
    }

    protected void setEnable(boolean enable) {
        isVerify = enable;
    }

    public int getReviewCount() {
        return reviewScores.size();
    }

    public double getAverageRating() {
        if (reviewScores.isEmpty()) {
            return 5.0;
        }
        return reviewScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(5.0);
    }

    // ========== BIDDING FUNCTIONALITY ==========

    /**
     * Place a bid on an item
     */
    public void placeBid(Item item, double amount) {
        if (item == null) {
            throw new IllegalArgumentException("Error: Target item cannot be null!");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Error: Bid amount must be greater than zero!");
        }

        if (LocalDateTime.now().isBefore(item.getStartTime())) {
            throw new IllegalStateException("Error: Auction has not started yet! Start time: " + item.getStartTime());
        }

        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalStateException("Error: Auction has already ended! End time: " + item.getEndTime());
        }

        if (item.isOwner(this.getId())) {
            throw new IllegalStateException("Error: User " + this.username + " cannot bid on their own item!");
        }

        if (amount <= item.getHighestCurrentPrice()) {
            throw new IllegalArgumentException("Error: Bid amount (" + amount + ") must be greater than the current highest price (" + item.getHighestCurrentPrice() + ")!");
        }

        if (this.balance < amount) {
            throw new IllegalStateException("Error: Insufficient balance! Required: " + amount + ", Available: " + this.balance);
        }

        item.setHighestCurrentPrice(amount);
        System.out.println("Success: " + this.username + " placed a bid of " + amount + " for item: " + item.getName());
    }

    // ========== SELLING FUNCTIONALITY ==========

    /**
     * Create a new auction item
     */
    public void createItem(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Item name cannot be null or empty!");
        }

        if (this.rating < 2.0) {
            throw new IllegalStateException("Error: User " + this.username + " has insufficient rating (" + String.format("%.2f", this.rating) + ") to list new items. Required: 2.0+");
        }

        System.out.println("Success: User " + this.username + " just listed a new item: " + itemName);
    }

    /**
     * Update seller rating based on buyer review
     */
    public void updateRating(double newReviewScore) {
        if (newReviewScore < 1.0 || newReviewScore > 5.0) {
            throw new IllegalArgumentException("Error: New review score must be between 1.0 and 5.0!");
        }

        reviewScores.add(newReviewScore);

        this.rating = reviewScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(5.0);

        System.out.println("✓ System: Rating for user " + this.username + " updated to " + String.format("%.2f", this.rating) + " (Total reviews: " + reviewScores.size() + ")");
    }

    @Override
    public String toString() {
        return "BidderandSeller{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", isEnable=" + isVerify +
                ", balance=" + balance +
                ", rating=" + String.format("%.2f", rating) +
                ", reviews=" + reviewScores.size() +
                ", role='" + role + '\'' +
                '}';
    }
}