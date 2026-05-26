package com.auction.shared.model.dto;

public class UserResponseDTO {
    private String id;
    private String username;
    private String email;
    private String role;
    private double balance;
    private double frozenBalance;
    private double rating;
    private boolean isVerify;

    public UserResponseDTO() {}

    public UserResponseDTO(String id, String username, String email, String role, 
                           double balance, double frozenBalance, double rating, boolean isVerify) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
        this.frozenBalance = frozenBalance;
        this.rating = rating;
        this.isVerify = isVerify;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getFrozenBalance() { return frozenBalance; }
    public void setFrozenBalance(double frozenBalance) { this.frozenBalance = frozenBalance; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public boolean isVerify() { return isVerify; }
    public void setVerify(boolean isVerify) { this.isVerify = isVerify; }
}
