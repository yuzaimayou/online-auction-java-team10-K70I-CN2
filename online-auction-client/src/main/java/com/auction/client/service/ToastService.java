package com.auction.client.service;

import com.auction.client.controller.ToastController;
import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ToastService {
    public static void showSuccess(Scene scene, String message) {
        show(scene, message, "toast-success");
    }
    public static void showError(Scene scene, String message) {
        show(scene, message, "toast-error");
    }
    public static void showInfo(Scene scene, String message) {
        show(scene, message, "toast-info");
    }
    private static void show(Scene scene, String message, String styleClass) {
        try {
            FXMLLoader loader = new FXMLLoader(ToastService.class.getResource("/com.auction.client/fxml/Toast.fxml"));
            Parent toastNode = loader.load();

            ToastController controller = loader.getController();
            controller.setMessage(message);

            toastNode.getStyleClass().add(styleClass);
            String css = ToastService.class.getResource("/com.auction.client/css/Toast.css").toExternalForm();
            toastNode.getStylesheets().add(css);

            Parent root = scene.getRoot();

            if (!(root instanceof StackPane)) {
                System.err.println("Lỗi: Root của Scene phải là StackPane để hiển thị Toast.");
                return;
            }
            StackPane container = (StackPane) root;

            container.getChildren().add(toastNode);
            StackPane.setAlignment(toastNode, Pos.TOP_CENTER);

            toastNode.setManaged(false);

            toastNode.applyCss();
            if (toastNode instanceof Region region) {
                region.autosize();
            }

            toastNode.layoutXProperty().bind(container.widthProperty().subtract(toastNode.boundsInParentProperty().get().getWidth()).divide(2));
            toastNode.setOpacity(0);
            double startY = -50;
            double targetY = 30;
            toastNode.setTranslateY(startY);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastNode);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toastNode);
            slideIn.setFromY(startY);
            slideIn.setToY(targetY);

            ParallelTransition showAnim = new ParallelTransition(fadeIn, slideIn);

            PauseTransition wait = new PauseTransition(Duration.seconds(1.5));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastNode);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toastNode);
            slideOut.setFromY(targetY);
            slideOut.setToY(startY);

            ParallelTransition hideAnim = new ParallelTransition(fadeOut, slideOut);
            hideAnim.setOnFinished(e -> container.getChildren().remove(toastNode));

            new SequentialTransition(showAnim, wait, hideAnim).play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}