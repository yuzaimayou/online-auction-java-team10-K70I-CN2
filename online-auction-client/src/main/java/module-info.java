module online.auction.client {
    requires transitive online.auction.shared;

    requires java.desktop;
    requires com.google.gson;
    requires javafx.controls;
    requires javafx.fxml;

    requires javafx.base;
    requires javafx.graphics;
    requires java.net.http;

    exports com.auction.client;
    exports com.auction.client.controller;

    opens com.auction.client to javafx.fxml;
    opens com.auction.client.controller to javafx.fxml;
    exports com.auction.client.controller.setting;
    opens com.auction.client.controller.setting to javafx.fxml;
    exports com.auction.client.controller.user;
    opens com.auction.client.controller.user to javafx.fxml;
}