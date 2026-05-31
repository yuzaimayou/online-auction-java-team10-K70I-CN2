package com.auction.client.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientImageUtil {

    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    public static void displayImage(String imageName, String source, ImageView imageView, double reqWidth, double reqHeight) {
        String imageUrl = String.format("%s/%s/%s", AppConfig.getStaticUrl(), source, imageName);
        Image fxImage = imageCache.computeIfAbsent(imageUrl, key -> {
            try {
                System.out.println("Loading image: " + imageUrl);
                return new Image(imageUrl, true); // Load bất đồng bộ để tránh đơ UI
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });

        imageView.setSmooth(true);
        imageView.setImage(fxImage);
    }

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

        Runnable updateViewport = () -> {
            Image img = imageView.getImage();
            if (img == null) return;
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            double containerW = container.getWidth();
            double containerH = container.getHeight();
            if (imgW == 0 || imgH == 0 || containerW == 0 || containerH == 0) return;

            double targetRatio = containerW / containerH;
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
        };

        container.widthProperty().addListener((obs, old, val) -> updateViewport.run());
        container.heightProperty().addListener((obs, old, val) -> updateViewport.run());
        imageView.imageProperty().addListener((obs, old, val) -> {
            if (val != null) {
                if (val.getProgress() < 1.0) {
                    val.progressProperty().addListener((o, ov, nv) -> {
                        if (nv.doubleValue() >= 1.0) Platform.runLater(updateViewport);
                    });
                } else {
                    Platform.runLater(updateViewport);
                }
            }
        });
    }

    public static void applyObjectFitCoverToImageView(ImageView imageView, Image img, double targetW, double targetH) {
        if (img == null || imageView == null) return;

        if (img.getProgress() < 1.0) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    Platform.runLater(() -> applyObjectFitCoverToImageView(imageView, img, targetW, targetH));
                }
            });
            return;
        }

        Platform.runLater(() -> {
            imageView.setSmooth(true);
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            if (imgW == 0 || imgH == 0) return;

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
        });
    }

    public static void clearCache() {
        imageCache.clear();
    }
}