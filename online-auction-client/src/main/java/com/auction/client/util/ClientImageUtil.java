package com.auction.client.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ClientImageUtil {
    public static void displayImage(String imageName, String source, ImageView imageId) {
        String imageUrl = String.format("http://localhost:1401/%s/%s", source, imageName);
        Image fxImage = new Image(imageUrl, true);

        imageId.setImage(fxImage);
        System.out.println("loaded image" + imageName);

    }
}
