package com.auction.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Util hiển thị Alert dùng chung toàn client.
 * Mỗi controller không nên tự dựng Alert riêng.
 */
public class AlertUtil {

    private AlertUtil() {}

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait()
                .filter(r -> r == ButtonType.YES)
                .isPresent();
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}