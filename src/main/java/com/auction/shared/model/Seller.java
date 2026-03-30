package com.auction.shared.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Seller extends Bidder {
    private double rating; // Seller reputation score (from 1.0 to 5.0)
    private List<Double> reviewScores; //Keep track of all reviews

    public Seller(String id, String username, String password) {
        // Validation for id, username, and password is automatically handled by parent classes
        super(id, username, password);
        this.role = "Seller";
        this.rating = 5.0; // New sellers start with a perfect 5.0 rating
        this.reviewScores = new ArrayList<>();
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

    /*
     * Create a new auction item
     * Prevents sellers with low reputation from listing items
     */
    public void createItem(String itemName) {
        // [ERROR HANDLING] Prevent empty or null item names
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Item name cannot be null or empty!");
        }

        // [ERROR HANDLING - BUSINESS LOGIC] Ban sellers with poor reputation from creating items
        if (this.rating < 2.0) {
            throw new IllegalStateException("Error: Seller " + this.username +
                    " has insufficient rating (" + String.format("%.2f", this.rating) +
                    ") to list new items. Required: 2.0+");
        }

        System.out.println("✓ Success: Seller " + this.username + " just listed a new product: " + itemName);
    }

    /*
     * Seller places a bid (can participate in auctions)
     * Delegates to parent Bidder.placeBid() but with seller-specific check
     * Override to prevent bidding on own items
     */
    @Override
    public void placeBid(Item item, double amount) {
        // [ERROR HANDLING] Prevent seller from bidding on their own item
        if (item.isOwner(this.getId())) {
            throw new IllegalStateException("Error: Seller " + this.username +
                    " cannot bid on their own item!");
        }

        // Delegate to parent class (Bidder) for all other validations
        super.placeBid(item, amount);
    }

    /**
     * Update seller rating based on buyer review
     * Now uses average of all reviews instead of simple average of last 2
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

        System.out.println("✓ System: Rating for seller " + this.username +
                " updated to " + String.format("%.2f", this.rating) +
                " (Total reviews: " + reviewScores.size() + ")");
    }

    /**
     * Get number of reviews
     */
    public int getReviewCount() {
        return reviewScores.size();
    }

    //Get average rating from all reviews
    public double getAverageRating() {
        if (reviewScores.isEmpty()) {
            return 5.0;
        }
        return reviewScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(5.0);
    }

    @Override
    public String toString() {
        return "Seller{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", balance=" + balance +
                ", rating=" + String.format("%.2f", rating) +
                ", reviews=" + reviewScores.size() +
                ", role='" + role + '\'' +
                '}';
    }
}