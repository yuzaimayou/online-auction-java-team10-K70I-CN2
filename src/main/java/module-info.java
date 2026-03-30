module online.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;
    requires java.desktop;

    // Cho phép JavaFX truy cập vào package client để load file giao diện .fxml
    opens com.auction.client to javafx.fxml;

    // Xuất package ra để hệ thống có thể chạy được MainClient
    exports com.auction.client;
    exports com.auction.client.controller;
    opens com.auction.client.controller to javafx.fxml;

    opens com.auction.shared.model to com.google.gson;
    opens com.auction.shared.message to com.google.gson;
    opens com.auction.shared.constant to com.google.gson;
    opens com.auction.shared.model.base to com.google.gson;
    opens com.auction.shared.model.account to com.google.gson;
    opens com.auction.shared.model.auction to com.google.gson;
    opens com.auction.shared.model.enums to com.google.gson;
}