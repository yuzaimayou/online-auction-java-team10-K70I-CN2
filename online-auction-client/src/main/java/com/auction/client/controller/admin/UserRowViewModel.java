package com.auction.client.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UserRowViewModel {
    private final StringProperty id; // Bổ sung trường ID
    private final StringProperty userName;
    private final StringProperty email;
    private final StringProperty status;
    private final StringProperty role;

    public UserRowViewModel(String id, String userName, String email, String status, String role) {
        this.id = new SimpleStringProperty(id);
        this.userName = new SimpleStringProperty(userName);
        this.email = new SimpleStringProperty(email);
        this.status = new SimpleStringProperty(status);
        this.role = new SimpleStringProperty(role);
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }

    public StringProperty userNameProperty() { return userName; }
    public String getUserName() { return userName.get(); }
    public void setUserName(String userName) { this.userName.set(userName); }

    public StringProperty emailProperty() { return email; }
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }

    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }

    public StringProperty roleProperty() { return role; }
    public String getRole() { return role.get(); }
    public void setRole(String role) { this.role.set(role); }

}