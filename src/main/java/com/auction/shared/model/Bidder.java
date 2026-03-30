package com.auction.shared.model;

import java.time.LocalDateTime;

public class Bidder extends User {
    protected double balance;

    public Bidder(String id, String username, String password) {
        super(id, username, password, "Bidder");
        this.balance = 0.0;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        //Prevent negative balance
        if (balance < 0) {
            throw new IllegalArgumentException("Error: Balance cannot be negative!");
        }
        this.balance = balance;
    }

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
        //Check if the item is null
        if (item == null) {
            throw new IllegalArgumentException("Error: Target item cannot be null!");
        }

        //Bid amount must be positive
        if (amount <= 0) {
            throw new IllegalArgumentException("Error: Bid amount must be greater than zero!");
        }

        //Check if auction has not started yet (Requirement 3.1.5)
        if (LocalDateTime.now().isBefore(item.getStartTime())) {
            throw new IllegalStateException("Error: Auction has not started yet! " +
                    "Start time: " + item.getStartTime());
        }

        // ✨ [NEW] Check if auction has already ended (Requirement 3.1.5)
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalStateException("Error: Auction has already ended! " +
                    "End time: " + item.getEndTime());
        }

        // [ERROR HANDLING] Prevent Shill Bidding (bidding on own items) (Requirement 3.1.3)
        if (item.isOwner(this.getId())) {
            throw new IllegalStateException("Error: User " + this.username +
                    " cannot bid on their own item!");
        }

        //Ensure the bid beats the current highest price (Requirement 3.1.3)
        if (amount <= item.getHighestCurrentPrice()) {
            throw new IllegalArgumentException("Error: Bid amount (" + amount +
                    ") must be greater than the current highest price (" +
                    item.getHighestCurrentPrice() + ")!");
        }

        //Check if bidder has sufficient balance (Requirement 3.1.5)
        if (this.balance < amount) {
            throw new IllegalStateException("Error: Insufficient balance! " +
                    "Required: " + amount + ", Available: " + this.balance);
        }

        // If all checks pass, process the bid
        item.setHighestCurrentPrice(amount);
        System.out.println("Success: " + this.username + " placed a bid of " + amount +
                " for item: " + item.getName());
    }

    @Override
    public String toString() {
        return "Bidder{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", balance=" + balance +
                ", role='" + role + '\'' +
                '}';
    }
}