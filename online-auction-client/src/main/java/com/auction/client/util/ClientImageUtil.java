package com.auction.client.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientImageUtil {

    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    public static void displayImage(String imageName,
                                    String source,
                                    ImageView imageView,
                                    double reqWidth,
                                    double reqHeight) {
        String imageUrl  = String.format("%s/%s/%s", AppConfig.getStaticUrl(), source, imageName);
        String cacheKey  = imageUrl + "_" + reqWidth + "x" + reqHeight;

        Image fxImage = imageCache.computeIfAbsent(cacheKey, key -> {
            try {
                System.out.println("Loading new image: " + imageUrl);
                return new Image(imageUrl, reqWidth, reqHeight, true, true);
            } catch (Exception e) {
                System.err.println("Failed to load image: " + imageUrl);
                e.printStackTrace();
                return null;
            }
        });

        imageView.setImage(fxImage);
    }
    public static void applyObjectFitCoverToImageView(ImageView imageView,
                                                      Image img,
                                                      double targetW,
                                                      double targetH) {
        if (img == null || imageView == null) return;

        if (img.getProgress() < 1.0) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    Platform.runLater(() ->
                            applyObjectFitCoverToImageView(imageView, img, targetW, targetH));
                }
            });
            return;
        }

        Platform.runLater(() -> {
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            if (imgW == 0 || imgH == 0) return;

            double targetRatio = targetW / targetH;
            double sourceRatio = imgW   / imgH;
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