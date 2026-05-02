package com.auction.client.controller;

import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.product.Item;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ItemCardHPController {
    private Item currentItem;

    @FXML
    private StackPane imageContainer;
    @FXML
    private ImageView productImage;
    @FXML
    private Label productNameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label endTimeLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label fullEndDateLabel;

    private Timeline timeline;


    @FXML
    public void initialize() {
        if (imageContainer == null) return;

        // 1. Tạo mặt nạ cắt phần thừa (border-radius)
        Rectangle clip = new Rectangle(280, 240);
        clip.setArcWidth(30); // Nếu muốn bo góc nhẹ, bạn có thể tăng lên 10-15
        clip.setArcHeight(30);
        imageContainer.setClip(clip);

        // Tắt tính năng tự giữ tỷ lệ của JavaFX để mình tự tính bằng code
        productImage.setPreserveRatio(false);

        // 2. Lắng nghe ảnh thay đổi
        productImage.imageProperty().addListener((observable, oldImage, newImage) -> {
            if (newImage != null) {
                newImage.widthProperty().addListener((obs, oldW, newW) -> applyObjectFitCover(newImage));
                newImage.heightProperty().addListener((obs, oldH, newH) -> applyObjectFitCover(newImage));
                applyObjectFitCover(newImage);
            }
        });
    }

    private void applyObjectFitCover(Image img) {
        if (img == null) return;

        double w = img.getWidth();
        double h = img.getHeight();

        if (w <= 0 || h <= 0) return;

        double containerW = 280.0;
        double containerH = 240.0;

        // Tìm tỷ lệ phóng to cho chiều ngang và chiều dọc
        double scaleX = containerW / w;
        double scaleY = containerH / h;

        // Cover: Phải lấy tỷ lệ lớn hơn để đảm bảo lấp đầy toàn bộ khung
        double scale = Math.max(scaleX, scaleY);

        // Set lại kích thước cho ảnh sau khi đã nhân tỷ lệ
        productImage.setFitWidth(w * scale);
        productImage.setFitHeight(h * scale);
    }

    public void setData(Item item) {
        this.currentItem = item;

        productNameLabel.setText(truncateText(item.getName(), 40));

        double displayPrice = (item.getHighestCurrentPrice() > 0) ?
                item.getHighestCurrentPrice() : item.getStartingPrice();
        priceLabel.setText(formatPrice(displayPrice));

        statusLabel.setText(getStatus(item.getEndTime()));
        if (item.getEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            fullEndDateLabel.setText("Ends on " + item.getEndTime().format(formatter));
        }

        ClientImageUtil.displayImage(item.getImagePath(), "images", productImage);

        startCountdown();
    }

    private String formatPrice(double price) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return "$ " + formatter.format(price) + " USD";
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) return "N/A";

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) return "Ended";

        long days = ChronoUnit.DAYS.between(now, endTime);
        long hours = ChronoUnit.HOURS.between(now, endTime) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;

        return String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

    private String getStatus(LocalDateTime endTime) {
        if (endTime == null || endTime.isBefore(LocalDateTime.now())) {
            return "ENDED";
        }
        return "LIVE";
    }

    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    String timeLeft = formatTimeLeft(currentItem.getEndTime());
                    endTimeLabel.setText(timeLeft);

                    if (timeLeft.equals("Ended")) {
                        statusLabel.setText("ENDED");
                        timeline.stop();
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return (text.length() <= maxLength) ? text : text.substring(0, maxLength) + "...";
    }

    @FXML
    public void handleSwitchToProductPage(MouseEvent event) {
        try {
            // Dừng timeline trước khi chuyển trang để tránh rò rỉ bộ nhớ
            if (timeline != null) timeline.stop();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ProductPage.fxml"));
            Parent root = loader.load();

            ProductPageController productPageController = loader.getController();
            productPageController.initData(this.currentItem);

            Scene currentScene = productNameLabel.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle(String.format("Online Auction System - %s", currentItem.getName()));

        } catch (IOException e) {
            System.err.println("Lỗi chuyển trang: " + e.getMessage());
        }
    }
}