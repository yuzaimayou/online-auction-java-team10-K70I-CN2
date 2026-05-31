package com.auction.client.ui;

import com.auction.client.util.ToastUtil;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Trách nhiệm: Quản lý trạng thái và render giao diện đồng thời cho cả
 * ảnh cũ trên Server (String) và ảnh mới thêm cục bộ (File).
 */
public class ItemImageEditManager {
    private static final int MAX_IMAGES = 5;

    private final VBox dragDropArea;
    private final HBox imagesPreviewContainer;
    private final VBox smallAddBtn;
    private final Scene scene;

    private final List<String> existingImagePaths = new ArrayList<>();
    private final List<File> newSelectedFiles = new ArrayList<>();

    public ItemImageEditManager(VBox dragDropArea, HBox imagesPreviewContainer, VBox smallAddBtn, Scene scene) {
        this.dragDropArea = dragDropArea;
        this.imagesPreviewContainer = imagesPreviewContainer;
        this.smallAddBtn = smallAddBtn;
        this.scene = scene;
    }

    public void initData(List<String> serverPaths) {
        this.existingImagePaths.clear();
        this.newSelectedFiles.clear();
        if (serverPaths != null) {
            this.existingImagePaths.addAll(serverPaths);
        }
        refreshImageStrip();
    }

    public void chooseImages(Window window) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        List<File> files = fc.showOpenMultipleDialog(window);
        if (files == null || files.isEmpty()) return;

        if (existingImagePaths.size() + newSelectedFiles.size() + files.size() > MAX_IMAGES) {
            ToastUtil.showInfo(scene, "Tối đa " + MAX_IMAGES + " ảnh.");
            return;
        }

        newSelectedFiles.addAll(files);
        refreshImageStrip();
    }

    public List<String> getExistingImagePaths() {
        return new ArrayList<>(existingImagePaths);
    }

    public List<File> getNewSelectedFiles() {
        return new ArrayList<>(newSelectedFiles);
    }

    public List<File> getAllImagesAsFiles() {
        List<File> allImages = new ArrayList<>(newSelectedFiles);
        existingImagePaths.forEach(path -> allImages.add(new File(path)));
        return allImages;
    }

    public void refreshImageStrip() {
        int total = existingImagePaths.size() + newSelectedFiles.size();
        boolean hasImages = total > 0;

        dragDropArea.setVisible(!hasImages);
        dragDropArea.setManaged(!hasImages);
        imagesPreviewContainer.setVisible(hasImages);
        imagesPreviewContainer.setManaged(hasImages);

        if (!hasImages) return;
        imagesPreviewContainer.getChildren().clear();

        // 1. Render ảnh cũ từ server -> Đổi sang gọi ImageCardFactory
        for (String imgPath : existingImagePaths) {
            imagesPreviewContainer.getChildren().add(
                    ImageCardFactory.createServerImageCard(imgPath, () -> {
                        existingImagePaths.remove(imgPath);
                        refreshImageStrip();
                    })
            );
        }
        for (File file : newSelectedFiles) {
            imagesPreviewContainer.getChildren().add(
                    ImageCardFactory.createLocalImageCard(file, () -> {
                        newSelectedFiles.remove(file);
                        refreshImageStrip();
                    })
            );
        }

        if (total < MAX_IMAGES) {
            imagesPreviewContainer.getChildren().add(smallAddBtn);
        }
    }
}