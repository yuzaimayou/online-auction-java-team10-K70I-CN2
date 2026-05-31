package com.auction.client.ui;

import com.auction.client.util.ClientImageUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.List;

public class ImageCardFactory {

    private static final double CARD_WIDTH  = 150.0;
    private static final double CARD_HEIGHT = 120.0;
    private static final double CARD_ARC    = 20.0;

    /**
     * Dựng Card hiển thị ảnh Local vừa chọn từ máy người dùng
     */
    public static StackPane createLocalImageCard(File file, Runnable onRemove) {
        // Khởi tạo Image với cấu hình load ngầm để tránh đứng giao diện
        ImageView iv = new ImageView(new Image(file.toURI().toString(), true));
        return buildBaseThumbCard(iv, onRemove, CARD_WIDTH, CARD_HEIGHT);
    }

    /**
     * Dựng Card hiển thị ảnh từ URL tĩnh của Server phục vụ chỉnh sửa sản phẩm
     */
    public static StackPane createServerImageCard(String imgPath, Runnable onRemove) {
        ImageView iv = new ImageView();
        ClientImageUtil.displayImage(imgPath, "images", iv, CARD_WIDTH * 2, CARD_HEIGHT * 2);
        return buildBaseThumbCard(iv, onRemove, CARD_WIDTH, CARD_HEIGHT);
    }

    /**
     * Dựng Gallery Thumbnail xem trước chi tiết sản phẩm
     */
    public static void setupThumbnailGallery(HBox thumbnailContainer, ImageView itemImage, List<String> images,
                                             double thumbW, double thumbH, double targetW, double targetH) {
        thumbnailContainer.getChildren().clear();
        if (images == null || images.isEmpty()) return;

        ClientImageUtil.displayImage(images.get(0), "images", itemImage, targetW * 2, targetH * 2);

        boolean isFirst = true;
        for (String imgPath : images) {
            if (imgPath == null || imgPath.isBlank()) continue;

            StackPane thumbPane = new StackPane();
            thumbPane.getStyleClass().add("thumbnail-container");
            thumbPane.setPrefSize(thumbW, thumbH);

            if (isFirst) {
                thumbPane.getStyleClass().add("active-thumb");
                isFirst = false;
            }

            ImageView thumbView = new ImageView();
            thumbView.setFitWidth(thumbW);
            thumbView.setFitHeight(thumbH);
            thumbView.imageProperty().addListener((obs, oldImg, newImg) -> {
                if (newImg != null) {
                    ClientImageUtil.applyObjectFitCoverToImageView(thumbView, newImg, thumbW, thumbH);
                }
            });

            ClientImageUtil.displayImage(imgPath, "images", thumbView, thumbW * 3, thumbH * 3);
            thumbPane.getChildren().add(thumbView);

            thumbPane.setOnMouseClicked(e -> {
                ClientImageUtil.displayImage(imgPath, "images", itemImage, targetW * 2, targetH * 2);
                thumbnailContainer.getChildren().forEach(n -> n.getStyleClass().remove("active-thumb"));
                thumbPane.getStyleClass().add("active-thumb");
            });

            thumbnailContainer.getChildren().add(thumbPane);
        }
    }

    /**
     * Hàm dùng chung tối ưu hóa Responsive Cover, bo góc và nút xóa
     */
    private static StackPane buildBaseThumbCard(ImageView iv, Runnable onRemove, double width, double height) {
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("image-border-container");
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPrefSize(width, height);
        imageContainer.setMinSize(width, height);
        imageContainer.setMaxSize(width, height);

        // Áp dụng thuật toán co giãn cover động cực mượt đã tối ưu ở Util
        ClientImageUtil.makeResponsiveCover(iv, imageContainer, CARD_ARC);
        imageContainer.getChildren().add(iv);

        Button btnDel = new Button("✕");
        btnDel.getStyleClass().add("delete-photo-btn");
        StackPane.setAlignment(btnDel, Pos.TOP_RIGHT);
        btnDel.setOnAction(e -> onRemove.run());

        StackPane finalCard = new StackPane(imageContainer, btnDel);
        finalCard.setPickOnBounds(false);
        return finalCard;
    }
}