package com.auction.shared.model.account;

import com.auction.shared.model.base.Entity;

public class User extends Entity {
    protected String username;
    protected String password;
    protected String role;

    public User(String id, String username, String password, String role) {
        super(id); // Validation for ID is handled by the parent class (Entity)

        // [ERROR HANDLING] Validate username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Username cannot be null or empty!");
        }

        // [ERROR HANDLING] Validate password
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Password cannot be null or empty!");
        }

        // [ERROR HANDLING] Validate role
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Role cannot be null or empty!");
        }

        this.username = username;
        this.password = password;
        this.role = role;
    }

    // --- Getters & Setters ---
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        // [ERROR HANDLING] Validate updated username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated username cannot be null or empty!");
        }
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        // [ERROR HANDLING] Validate updated password
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated password cannot be null or empty!");
        }
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        // [ERROR HANDLING] Validate updated role
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated role cannot be null or empty!");
        }
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}