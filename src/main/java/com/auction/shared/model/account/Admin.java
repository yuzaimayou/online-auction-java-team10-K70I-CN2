package com.auction.shared.model.account;

public class Admin extends User {

    public Admin(String id, String username, String password) {
        super(id, username, password);
        this.role = "Admin"; // Set specific role
    }

    /**
     * Delete a user from the system
     * Validates user ID and prevents self-deletion
     */
    public void deleteUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid user ID to delete (cannot be empty)!");
        }

        if (userId.equals(this.id)) {
            throw new IllegalStateException("Error: Admin cannot delete themselves!");
        }

        System.out.println("Admin " + this.username + " deleted user: " + userId);
    }

    /**
     * Approve an auction
     * Validates auction ID
     */
    public void approveAuction(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid auction ID to approve (cannot be empty)!");
        }

        System.out.println("Admin " + this.username + " approved auction: " + auctionId);
    }

    /**
     * Suspend a user account
     * Validates user ID
     */
    public void suspendUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid user ID to suspend (cannot be empty)!");
        }

        if (userId.equals(this.id)) {
            throw new IllegalStateException("Error: Admin cannot suspend themselves!");
        }

        System.out.println("Admin " + this.username + " suspended user: " + userId);
    }
}