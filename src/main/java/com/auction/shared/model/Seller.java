package com.auction.shared.model;

public class Seller extends Bidder {
    private double rating; // Seller reputation score

    public Seller(String id, String username, String password) {
        super(id, username, password);
        this.role = "Seller";
        this.rating = 5.0;
    }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public void createItem(String itemName) {
        System.out.println(this.username + " Just listed a new product: " + itemName);
    }
}