package com.auction.client.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static com.auction.client.util.AppConfig.getImageUrl;

public class ClientImageUtil {
    public static void displayImage(String imageName, String source, ImageView imageId) {
        String imageUrl = String.format("%s/%s/%s", getImageUrl(), source, imageName);
        Image fxImage = new Image(imageUrl, true);

        imageId.setImage(fxImage);
        System.out.println("loaded image" + imageName);

    }
}
