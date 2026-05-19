package com.auction.shared.model.account;

import com.auction.shared.model.base.Entity;

public abstract class Person extends Entity {
    protected String username;
    protected String password;
    private String roleOverride;

    public Person(String id, String username, String password) {
        super(id);

        // [ERROR HANDLING] Validate username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Username cannot be null or empty!");
        }

        // [ERROR HANDLING] Validate password
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Password cannot be null or empty!");
        }

        this.username = username;
        this.password = password;
    }

    // --- Getters & Setters ---
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated username cannot be null or empty!");
        }
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Updated password cannot be null or empty!");
        }
        this.password = password;
    }

    protected abstract String getDefaultRole();
    public String getRole() {
        return (roleOverride != null) ? roleOverride : getDefaultRole();
    }

    public void setRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Role cannot be null or empty!");
        }
        this.roleOverride = role;
    }

    @Override
    public String toString() {
        return "Person{id='" + id + "', username='" + username + "', role='" + getRole() + "'}";
    }
}