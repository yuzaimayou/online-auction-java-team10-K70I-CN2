package com.auction.shared.model.account;

import com.auction.shared.model.item.Item;

import java.time.LocalDateTime;

public class User extends Person {
    protected double balance;
    protected double frozenBalance; // Money held against current highest bid
    protected double rating; // Seller reputation score (from 1.0 to 5.0)
    protected String email;
    protected boolean isVerify;
    protected String status;

    public User(String id, String username, String password) {
        super(id, username, password);
        this.balance = 0.0;
        this.rating = 5.0; // New users start with perfect rating
        this.email = null;
        this.isVerify = true;
    }

    public User(String id, String username, String password, String email, boolean isEnable) {
        this(id, username, password);
        this.email = email;
        this.isVerify = isEnable;
    }

    @Override
    protected String getDefaultRole() {
        return "User";
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
        return balance - frozenBalance;
    }

    public double getRating() {
        return rating;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public void setRating(double rating) {
        // [ERROR HANDLING] Validate rating range
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Error: Rating must be between 1.0 and 5.0!");
        }
        this.rating = rating;
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

    public void setEnable(boolean enable) {
        isVerify = enable;
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
            throw new IllegalArgumentException("Error: Bid amount (" + amount
                    + ") must be greater than the current highest price (" + item.getHighestCurrentPrice() + ")!");
        }

        if (this.getAvailableBalance() < amount)
            throw new IllegalStateException(
                    "Error: Insufficient balance! Required: " + amount + ", Available: " + this.getAvailableBalance());

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
            throw new IllegalStateException("Error: User " + this.username + " has insufficient rating ("
                    + String.format("%.2f", this.rating) + ") to list new items. Required: 2.0+");
        }

        System.out.println("Success: User " + this.username + " just listed a new item: " + itemName);
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', email='" + email +
                "', isVerify=" + isVerify + ", balance=" + balance +
                ", frozenBalance=" + frozenBalance +
                ", availableBalance=" + getAvailableBalance() +
                ", rating=" + String.format("%.2f", rating) +
                ", role='" + getRole() + "'}";
    }
}