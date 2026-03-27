package com.auction.shared.model;

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
        // [ERROR HANDLING] Prevent negative balance
        if (balance < 0) {
            throw new IllegalArgumentException("Error: Balance cannot be negative!");
        }
        this.balance = balance;
    }

    // Bidding function with strict validations
    public void placeBid(Item item, double amount) {
        // [ERROR HANDLING] Check if the item is null
        if (item == null) {
            throw new IllegalArgumentException("Error: Target item cannot be null!");
        }

        // Bid amount must be positive
        if (amount <= 0) {
            throw new IllegalArgumentException("Error: Bid amount must be greater than zero!");
        }

        // Prevent Shill Bidding (bidding on own items)
        if (item.isOwner(this.getId())) {
            // IllegalStateException: invalid status
            throw new IllegalStateException("Error: User " + this.username + " cannot bid on their own item!");
        }

        // Ensure the bid beats the current highest price
        if (amount <= item.getHighestCurrentPrice()) {
            throw new IllegalArgumentException("Error: Bid amount (" + amount + ") must be greater than the current highest price (" + item.getHighestCurrentPrice() + ")!");
        }

        // If all checks pass, process the bid
        item.setHighestCurrentPrice(amount);
        System.out.println("Success: " + this.username + " placed a bid of " + amount + " for item: " + item.getName());
    }
}