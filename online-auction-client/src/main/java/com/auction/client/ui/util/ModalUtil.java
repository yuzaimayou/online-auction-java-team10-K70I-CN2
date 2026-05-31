package com.auction.client.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.function.Consumer;

public class ModalUtil {

    private ModalUtil() {}

    public static void showEditModal(
            StackPane sceneRoot,
            String fxmlPath,
            Consumer<FXMLLoader> initController,
            Runnable onClose
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ModalUtil.class.getResource(fxmlPath));
            Parent form = loader.load();
            if (initController != null) initController.accept(loader);

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("popup-overlay");
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            overlay.prefWidthProperty().bind(sceneRoot.widthProperty());
            overlay.prefHeightProperty().bind(sceneRoot.heightProperty());
            overlay.setOnMouseClicked(e -> {
                sceneRoot.getChildren().remove(overlay);
                if (onClose != null) onClose.run();
            });
            form.setOnMouseClicked(javafx.event.Event::consume);
            overlay.getChildren().add(form);
            sceneRoot.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}