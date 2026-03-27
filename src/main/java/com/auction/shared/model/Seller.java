package com.auction.shared.model;

public class Seller extends Bidder {
    private double rating; // Seller reputation score (from 1.0 to 5.0)

    public Seller(String id, String username, String password) {
        // Validation for id, username, and password is automatically handled by parent classes
        super(id, username, password);
        this.role = "Seller";
        this.rating = 5.0; // New sellers start with a perfect 5.0 rating
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

    public void createItem(String itemName) {
        // [ERROR HANDLING] Prevent empty or null item names
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Item name cannot be null or empty!");
        }

        // [ERROR HANDLING - BUSINESS LOGIC] Ban sellers with poor reputation from creating items
        if (this.rating < 2.0) {
            // Using IllegalStateException because the parameter is fine, but the user's state is not allowed
            throw new IllegalStateException("Error: Seller " + this.username + " has a rating too low (" + this.rating + ") to list new items!");
        }

        System.out.println("Success: " + this.username + " just listed a new product: " + itemName);
    }

    // A helpful method to calculate the new rating when a buyer leaves a review
    public void updateRating(double newReviewScore) {
        // [ERROR HANDLING] Validate the incoming review score
        if (newReviewScore < 1.0 || newReviewScore > 5.0) {
            throw new IllegalArgumentException("Error: New review score must be between 1.0 and 5.0!");
        }

        // Calculate average (simple approach)
        this.rating = (this.rating + newReviewScore) / 2.0;
        System.out.println("System: Rating for seller " + this.username + " updated to " + this.rating);
    }
}