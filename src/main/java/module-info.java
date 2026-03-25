module online.auction {
    requires javafx.controls;
    requires javafx.fxml;

    // Cho phép JavaFX truy cập vào package client để load file giao diện .fxml
    opens com.auction.client to javafx.fxml;

    // Xuất package ra để hệ thống có thể chạy được MainClient
    exports com.auction.client;
}