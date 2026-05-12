package com.auction.client.model; // Hoặc đổi lại theo đúng thư mục của bạn

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UserModel {
    private final StringProperty userName;
    private final StringProperty email;
    private final StringProperty status;
    private final StringProperty role;
    private final StringProperty joinDate;

    public UserModel(String userName, String email, String status, String role, String joinDate) {
        this.userName = new SimpleStringProperty(userName);
        this.email = new SimpleStringProperty(email);
        this.status = new SimpleStringProperty(status);
        this.role = new SimpleStringProperty(role);
        this.joinDate = new SimpleStringProperty(joinDate);
    }

    // Các phương thức Property (Để TableView liên kết dữ liệu)
    public StringProperty userNameProperty() { return userName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty statusProperty() { return status; }
    public StringProperty roleProperty() { return role; }
    public StringProperty joinDateProperty() { return joinDate; }

    public String getUserName() { return userName.get(); }
    public String getStatus() { return status.get(); }
}