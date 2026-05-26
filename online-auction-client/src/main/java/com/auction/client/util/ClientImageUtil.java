package com.auction.client.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientImageUtil {

    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    /**
     * Tải và hiển thị hình ảnh từ server tĩnh kèm cache dữ liệu
     */
    public static void displayImage(String imageName,
                                    String source,
                                    ImageView imageView,
                                    double reqWidth,
                                    double reqHeight) {
        String imageUrl = String.format("%s/%s/%s", AppConfig.getStaticUrl(), source, imageName);

        String cacheKey = imageUrl + "_" + reqWidth + "x" + reqHeight;

        Image fxImage = imageCache.computeIfAbsent(cacheKey, key -> {
            try {
                System.out.println("Loading image: " + imageUrl);
                return new Image(imageUrl, reqWidth, reqHeight, true, true);
            } catch (Exception e) {
                System.err.println("Failed to load image: " + imageUrl);
                e.printStackTrace();
                return null;
            }
        });

        imageView.setImage(fxImage);
    }

    /**
     * Cấu hình ảnh hiển thị dạng Cover co giãn linh hoạt theo Container chính và bo góc tròn
     */
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

    /**
     * Áp dụng chế độ Object-Fit: Cover căn đều khung nhìn ImageView cho ảnh Thumbnail
     */
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

    /**
     * Hàm dựng bộ sưu tập ảnh Thu nhỏ (Thumbnail Gallery) cho trang chi tiết sản phẩm
     */
    public static void setupThumbnailGallery(HBox thumbnailContainer,
                                             ImageView itemImage,
                                             List<String> images,
                                             double thumbWidth,
                                             double thumbHeight,
                                             double imageWidth,
                                             double imageHeight) {
        thumbnailContainer.getChildren().clear();
        if (images == null || images.isEmpty()) return;

        // Đặt mặc định hiển thị ảnh đầu tiên lên khung lớn
        displayImage(images.get(0), "images", itemImage, imageWidth * 2, imageHeight * 2);

        boolean isFirst = true;
        for (String imgPath : images) {
            if (imgPath == null || imgPath.isBlank()) continue;

            StackPane thumbPane = new StackPane();
            thumbPane.getStyleClass().add("thumbnail-container");
            thumbPane.setMinWidth(thumbWidth);
            thumbPane.setMaxWidth(thumbWidth);
            thumbPane.setMinHeight(thumbHeight);
            thumbPane.setMaxHeight(thumbHeight);

            if (isFirst) {
                thumbPane.getStyleClass().add("active-thumb");
                isFirst = false;
            }

            ImageView thumbView = new ImageView();
            thumbView.setFitWidth(thumbWidth);
            thumbView.setFitHeight(thumbHeight);
            thumbView.setPreserveRatio(false);

            thumbView.imageProperty().addListener((obs, oldImg, newImg) -> {
                if (newImg != null) {
                    applyObjectFitCoverToImageView(thumbView, newImg, thumbWidth, thumbHeight);
                }
            });

            displayImage(imgPath, "images", thumbView, imageWidth, imageHeight);
            thumbPane.getChildren().add(thumbView);

            // Sự kiện click để đổi ảnh lớn chính
            thumbPane.setOnMouseClicked(e -> {
                Image clicked = thumbView.getImage();
                if (clicked != null) {
                    itemImage.setImage(clicked);
                } else {
                    displayImage(imgPath, "images", itemImage, thumbWidth, thumbHeight);
                }
                thumbnailContainer.getChildren().forEach(n -> n.getStyleClass().remove("active-thumb"));
                thumbPane.getStyleClass().add("active-thumb");
            });

            thumbnailContainer.getChildren().add(thumbPane);
        }
    }

    public static void clearCache() {
        imageCache.clear();
    }
}