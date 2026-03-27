package com.auction.shared.model;

public class Admin extends User {

    //Constructor calls 3 parameters of father
    public Admin(String id, String username, String password) {
        super(id, username, password, "Admin");
    }

    // Operating system funtions
    public void deleteUser(String userId) {
        System.out.println("Admin deleted user: " + userId);
    }

    public void approveAuction(String auctionId) {
        System.out.println("Admin approved auction: " + auctionId);
    }
}