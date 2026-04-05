package com.auction.shared.util;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class ImageUtil {
    public static String convertBase64ToImg(String base64Image, String extension) {
        if (base64Image == null || base64Image.isEmpty()) {
            System.out.println("base64Image is null");
            return null;
        }
        if (extension == null || extension.isEmpty()) {
            System.out.println("extension is null");
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + File.separator + "dataBase" + File.separator + "images" + File.separator;

            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            //create filename
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            Path imagePath = Paths.get(uploadDir + uniqueFileName);

            Files.write(imagePath, decodedBytes);
            return imagePath.toString();
        } catch (IOException e) {
            System.out.println("Error when decoding and saving images");
            e.printStackTrace();
            return null;
        }
    }

    public static String[] convertImgToBase64(File selectedImageFile, Label lblMessage) {
        String base64Image = "";
        String imageExtension = "";
        String[] file = {"asdasd", "sdsd"};
        try {
            String fileName = selectedImageFile.getName();
            imageExtension = fileName.substring(fileName.lastIndexOf("."));
            byte[] fileContent = Files.readAllBytes(selectedImageFile.toPath());
            base64Image = Base64.getEncoder().encodeToString(fileContent);
            return new String[]{base64Image, imageExtension};
        } catch (IOException e) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Error reading image file!");
            e.printStackTrace();
            return null;
        }
    }
}
