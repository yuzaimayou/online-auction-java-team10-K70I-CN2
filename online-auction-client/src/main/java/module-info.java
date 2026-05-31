module online.auction.client {
    requires transitive online.auction.shared;

    requires java.desktop;
    requires com.google.gson;
    requires javafx.controls;
    requires javafx.fxml;

    requires javafx.base;
    requires javafx.graphics;
    requires java.net.http;

    // Giữ lại nếu thư mục client vẫn chứa file chạy App chính (như Main.java / App.java)
    exports com.auction.client;
    opens com.auction.client to javafx.fxml;

    // ── PHÂN HỆ AUTH ──────────────────────────────────────────────────────
    exports com.auction.client.controller.auth;
    opens com.auction.client.controller.auth to javafx.fxml;

    // ── PHÂN HỆ COMMON ────────────────────────────────────────────────────
    exports com.auction.client.controller.common;
    opens com.auction.client.controller.common to javafx.fxml;

    // ── PHÂN HỆ USER ──────────────────────────────────────────────────────
    exports com.auction.client.controller.user;
    opens com.auction.client.controller.user to javafx.fxml;

    // ── PHÂN HỆ ADMIN ─────────────────────────────────────────────────────
    exports com.auction.client.controller.admin;
    opens com.auction.client.controller.admin to javafx.fxml;

    // ── PHÂN HỆ AUCTION ───────────────────────────────────────────────────
    exports com.auction.client.controller.auction;
    opens com.auction.client.controller.auction to javafx.fxml;
}