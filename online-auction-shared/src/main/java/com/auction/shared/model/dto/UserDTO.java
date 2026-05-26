package com.auction.shared.model.dto;

public class UserDTO {
    // SỬA: Đổi từ private sang public để Gson có quyền truy cập thẳng
    public String id;
    public String username;
    public String email;
    public String role;
    public boolean isVerify;
    public double balance;
    public double frozenBalance;

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

    // GIỮ NGUYÊN: Các hàm Getters để các file khác trong dự án không bị lỗi đỏ
    public String  getId()            { return id; }
    public String  getUsername()      { return username; }
    public String  getEmail()         { return email; }
    public String  getRole()          { return role; }
    public boolean isVerify()         { return isVerify; }
    public double  getBalance()       { return balance; }
    public double  getFrozenBalance() { return frozenBalance; }
}