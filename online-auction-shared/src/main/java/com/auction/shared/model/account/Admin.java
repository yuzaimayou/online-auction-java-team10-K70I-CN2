package com.auction.shared.model.account;

public class Admin extends User {

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    public Admin(String id, String username, String password, String email, boolean isVerify) {
        super(id, username, password, email, isVerify);
    }

    @Override
    protected String getDefaultRole() {
        return "Admin";
    }
    /**
     * Delete a auth from the system
     * Validates auth ID and prevents self-deletion
     */
    public void deleteUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid auth ID to delete (cannot be empty)!");
        }

        if (userId.equals(this.id)) {
            throw new IllegalStateException("Error: Admin cannot delete themselves!");
        }

        System.out.println("Admin " + this.username + " deleted auth: " + userId);
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
     * Suspend a auth account
     * Validates auth ID
     */
    public void suspendUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Invalid auth ID to suspend (cannot be empty)!");
        }

        if (userId.equals(this.id)) {
            throw new IllegalStateException("Error: Admin cannot suspend themselves!");
        }

        System.out.println("Admin " + this.username + " suspended auth: " + userId);
    }
    @Override
    public String toString() {
        return "Admin{id='" + id + "', username='" + username + "', email='" + email +
                "', isVerify=" + isVerify + ", balance=" + balance +
                ", role='" + getRole() + "'}";
    }
}