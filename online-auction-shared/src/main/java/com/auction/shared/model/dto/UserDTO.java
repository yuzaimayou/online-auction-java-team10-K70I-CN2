package com.auction.shared.model.dto;

public class UserDTO {
    private String id;
    private String username;
    private String email;
    private String role;
    private boolean isVerify;
    private double balance;
    private double frozenBalance;

    public UserDTO(String id, String username, String email,
                   String role, boolean isVerify,
                   double balance, double frozenBalance) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.role         = role;
        this.isVerify     = isVerify;
        this.balance      = balance;
        this.frozenBalance = frozenBalance;
    }

    // Getters
    public String  getId()            { return id; }
    public String  getUsername()      { return username; }
    public String  getEmail()         { return email; }
    public String  getRole()          { return role; }
    public boolean isVerify()         { return isVerify; }
    public double  getBalance()       { return balance; }
    public double  getFrozenBalance() { return frozenBalance; }
}