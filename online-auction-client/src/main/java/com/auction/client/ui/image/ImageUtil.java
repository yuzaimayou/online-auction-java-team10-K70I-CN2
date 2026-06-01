package com.auction.client.ui.image;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

public class ImageUtil {

    private ImageUtil() {}

    public static void makeResponsiveCover(ImageView imageView, Region container, double arcRadius) {
        imageView.setManaged(false);
        imageView.setLayoutX(0);
        imageView.setLayoutY(0);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(arcRadius);
        clip.setArcHeight(arcRadius);
        clip.widthProperty().bind(container.widthProperty());
        clip.heightProperty().bind(container.heightProperty());
        container.setClip(clip);

        imageView.fitWidthProperty().bind(container.widthProperty());
        imageView.fitHeightProperty().bind(container.heightProperty());

        Runnable updateViewport = () -> updateCoverViewport(
                imageView, container.getWidth(), container.getHeight()
        );

        container.widthProperty().addListener((obs, old, val) -> updateViewport.run());
        container.heightProperty().addListener((obs, old, val) -> updateViewport.run());
        imageView.imageProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (val.getProgress() < 1.0) {
                val.progressProperty().addListener((o, ov, nv) -> {
                    if (nv.doubleValue() >= 1.0) Platform.runLater(updateViewport);
                });
            } else {
                Platform.runLater(updateViewport);
            }
        });
    }

    public static void applyObjectFitCover(ImageView imageView, Image img,
                                           double targetW, double targetH) {
        if (img == null || imageView == null) return;

        if (img.getProgress() < 1.0) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0)
                    Platform.runLater(() -> applyObjectFitCover(imageView, img, targetW, targetH));
            });
            return;
        }

        Platform.runLater(() -> {
            imageView.setSmooth(true);
            applyViewport(imageView, img.getWidth(), img.getHeight(), targetW, targetH);
        });
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private static void updateCoverViewport(ImageView imageView,
                                            double containerW, double containerH) {
        Image img = imageView.getImage();
        if (img == null) return;
        applyViewport(imageView, img.getWidth(), img.getHeight(), containerW, containerH);
    }

    private static void applyViewport(ImageView imageView,
                                      double imgW, double imgH,
                                      double targetW, double targetH) {
        if (imgW == 0 || imgH == 0 || targetW == 0 || targetH == 0) return;

        double targetRatio = targetW / targetH;
        double sourceRatio = imgW / imgH;
        double viewW, viewH, viewX, viewY;

        if (sourceRatio > targetRatio) {
            viewH = imgH;
            viewW = imgH * targetRatio;
            viewX = (imgW - viewW) / 2;
            viewY = 0;
        } else {
            viewW = imgW;
            viewH = imgW / targetRatio;
            viewX = 0;
            viewY = (imgH - viewH) / 2;
        }
        imageView.setViewport(new Rectangle2D(viewX, viewY, viewW, viewH));
    }
}