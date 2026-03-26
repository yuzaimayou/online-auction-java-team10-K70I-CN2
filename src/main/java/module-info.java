module online.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.auction.shared.message to com.google.gson;
    opens com.auction.shared.model to com.google.gson;
    opens com.auction.shared.constant to com.google.gson;

    // Cho phép JavaFX truy cập vào package client để load file giao diện .fxml
    opens com.auction.client to javafx.fxml;

    // Xuất package ra để hệ thống có thể chạy được MainClient
    exports com.auction.client;
    exports com.auction.client.controller;
    exports com.auction.shared.message;
    exports com.auction.shared.model;
    opens com.auction.client.controller to javafx.fxml;
}