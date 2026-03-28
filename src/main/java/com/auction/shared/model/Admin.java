package com.auction.shared.model;

public class Admin extends User {

    // Constructor calls parameters of parent class
    public Admin(String id, String username, String password) {
        super(id, username, password, "Admin");
    }

    // Operating system functions
    public void deleteUser(String userId) {
        //Block immediately if the provided user ID is null or empty
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid user ID to delete (cannot be empty)!");
        }

        System.out.println("Admin deleted user: " + userId);
    }

    public void approveAuction(String auctionId) {
        //Block immediately if the provided auction ID is null or empty
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid auction ID to approve (cannot be empty)!");
        }

        System.out.println("Admin approved auction: " + auctionId);
    }
}