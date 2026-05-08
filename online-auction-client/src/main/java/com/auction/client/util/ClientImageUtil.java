package com.auction.client.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientImageUtil {
    //Create in-memory cache to store loaded images, key is image URL, value is Image object
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    public static void displayImage(String imageName, String source, ImageView imageId, double reqWidth, double reqHeight) {
        String imageUrl = String.format("%s/%s/%s", AppConfig.getStaticUrl(), source, imageName);

        Image fxImage = imageCache.computeIfAbsent(imageUrl, url -> {
            try {
                System.out.println("Loading new image: " + url);
                return new Image(url, reqWidth, reqHeight, true, true);
            } catch (Exception e) {
                System.err.println("Failed to load image: " + url);
                e.printStackTrace();
                return null;
            }
        });

        imageId.setImage(fxImage);

    }

    public static void clearCache() {
        imageCache.clear();
    }
}
