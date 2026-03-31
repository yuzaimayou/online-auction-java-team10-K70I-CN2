package com.auction.shared.model.account;

import com.auction.shared.model.base.Entity;
import com.auction.shared.model.product.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User extends Entity {
    protected String username;
    protected String password;
    protected String role;
    protected double balance;
    protected double rating; // Seller reputation score (from 1.0 to 5.0)
    protected List<Double> reviewScores; // Keep track of all reviews

    public User(String id, String username, String password) {
        super(id);

        // [ERROR HANDLING] Validate username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Username cannot be null or empty!");
        }

        // [ERROR HANDLING] Validate password
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Password cannot be null or empty!");
        }

        this.username = username;
        this.password = password;
        this.role = "User";
        this.balance = 0.0;
        this.rating = 5.0; // New users start with perfect rating
        this.reviewScores = new ArrayList<>();
    }

    // --- Getters & Setters ---
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated username cannot be null or empty!");
        }
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated password cannot be null or empty!");
        }
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated role cannot be null or empty!");
        }
        this.role = role;
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
     * [ERROR HANDLING] Comprehensive validation:
     * - Item existence
     * - Bid amount validity (positive, higher than current price)
     * - Shill bidding prevention (no bidding on own items)
     * - Time constraints (auction must be active)
     * - Balance check (sufficient funds)
     */
    public void placeBid(Item item, double amount) {
        // Check if the item is null
        if (item == null) {
            throw new IllegalArgumentException("Error: Target item cannot be null!");
        }

        // Bid amount must be positive
        if (amount <= 0) {
            throw new IllegalArgumentException("Error: Bid amount must be greater than zero!");
        }

        // Check if auction has not started yet (Requirement 3.1.5)
        if (LocalDateTime.now().isBefore(item.getStartTime())) {
            throw new IllegalStateException("Error: Auction has not started yet! " +
                    "Start time: " + item.getStartTime());
        }

        // Check if auction has already ended (Requirement 3.1.5)
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalStateException("Error: Auction has already ended! " +
                    "End time: " + item.getEndTime());
        }

        // [ERROR HANDLING] Prevent Shill Bidding (bidding on own items) (Requirement 3.1.3)
        if (item.isOwner(this.getId())) {
            throw new IllegalStateException("Error: User " + this.username +
                    " cannot bid on their own item!");
        }

        // Ensure the bid beats the current highest price (Requirement 3.1.3)
        if (amount <= item.getHighestCurrentPrice()) {
            throw new IllegalArgumentException("Error: Bid amount (" + amount +
                    ") must be greater than the current highest price (" +
                    item.getHighestCurrentPrice() + ")!");
        }

        // Check if bidder has sufficient balance (Requirement 3.1.5)
        if (this.balance < amount) {
            throw new IllegalStateException("Error: Insufficient balance! " +
                    "Required: " + amount + ", Available: " + this.balance);
        }

        // If all checks pass, process the bid
        item.setHighestCurrentPrice(amount);
        System.out.println("Success: " + this.username + " placed a bid of " + amount +
                " for item: " + item.getName());
    }

    // ========== SELLING FUNCTIONALITY ==========

    /**
     * Create a new auction item
     * Prevents users with low reputation from listing items
     */
    public void createItem(String itemName) {
        // [ERROR HANDLING] Prevent empty or null item names
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Item name cannot be null or empty!");
        }

        // [ERROR HANDLING - BUSINESS LOGIC] Ban users with poor reputation from creating items
        if (this.rating < 2.0) {
            throw new IllegalStateException("Error: User " + this.username +
                    " has insufficient rating (" + String.format("%.2f", this.rating) +
                    ") to list new items. Required: 2.0+");
        }

        System.out.println("Success: User " + this.username + " just listed a new product: " + itemName);
    }

    /**
     * Update seller rating based on buyer review
     * Uses average of all reviews
     */
    public void updateRating(double newReviewScore) {
        // [ERROR HANDLING] Validate the incoming review score
        if (newReviewScore < 1.0 || newReviewScore > 5.0) {
            throw new IllegalArgumentException("Error: New review score must be between 1.0 and 5.0!");
        }

        // Add review to history
        reviewScores.add(newReviewScore);

        // Calculate average from all reviews
        this.rating = reviewScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(5.0);

        System.out.println("✓ System: Rating for user " + this.username +
                " updated to " + String.format("%.2f", this.rating) +
                " (Total reviews: " + reviewScores.size() + ")");
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", balance=" + balance +
                ", rating=" + String.format("%.2f", rating) +
                ", reviews=" + reviewScores.size() +
                ", role='" + role + '\'' +
                '}';
    }
}