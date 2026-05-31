package com.auction.client.ui;

import com.auction.client.util.ToastUtil;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageUploadManager {
    private static final int MAX_IMAGES = 5;

    private final VBox dragDropArea;
    private final HBox imagesPreviewContainer;
    private final VBox smallAddBtn;
    private final Scene scene;
    private final List<File> selectedFiles = new ArrayList<>();

    public ImageUploadManager(VBox dragDropArea, HBox imagesPreviewContainer, VBox smallAddBtn, Scene scene) {
        this.dragDropArea = dragDropArea;
        this.imagesPreviewContainer = imagesPreviewContainer;
        this.smallAddBtn = smallAddBtn;
        this.scene = scene;
    }

    public void chooseImages(Window window) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        List<File> files = chooser.showOpenMultipleDialog(window);
        if (files == null) return;

        if (selectedFiles.size() + files.size() > MAX_IMAGES) {
            ToastUtil.showInfo(scene, "Only upload max " + MAX_IMAGES + " images");
            return;
        }

        selectedFiles.addAll(files);
        refreshImagePreview();
    }

    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    private void refreshImagePreview() {
        boolean hasFiles = !selectedFiles.isEmpty();
        dragDropArea.setVisible(!hasFiles);
        dragDropArea.setManaged(!hasFiles);
        imagesPreviewContainer.setVisible(hasFiles);
        imagesPreviewContainer.setManaged(hasFiles);

        if (hasFiles) {
            imagesPreviewContainer.getChildren().removeIf(n -> n != smallAddBtn);
            int idx = imagesPreviewContainer.getChildren().indexOf(smallAddBtn);
            for (File f : selectedFiles) {
                // Sửa đổi ở đây: Gọi thông qua Factory chuyên biệt của UI
                StackPane card = ImageCardFactory.createLocalImageCard(f, () -> {
                    selectedFiles.remove(f);
                    refreshImagePreview();
                });
                imagesPreviewContainer.getChildren().add(idx++, card);
            }
            smallAddBtn.setVisible(selectedFiles.size() < MAX_IMAGES);
        }
    }
}