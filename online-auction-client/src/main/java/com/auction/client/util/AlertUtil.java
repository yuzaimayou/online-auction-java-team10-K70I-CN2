package com.auction.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

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
        configure(alert);

        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert.showAndWait()
                .filter(r -> r == ButtonType.YES)
                .isPresent();
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        configure(alert);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private static void configure(Alert alert) {

        alert.initModality(Modality.APPLICATION_MODAL);
        Window window = Window.getWindows()
                .stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);

        if (window != null) {
            alert.initOwner(window);
        }
        alert.setResizable(false);

        // 4. size hợp lý
        alert.getDialogPane().setPrefWidth(360);
        alert.getDialogPane().setMaxWidth(420);
        alert.getDialogPane().setPrefHeight(180);
    }
}