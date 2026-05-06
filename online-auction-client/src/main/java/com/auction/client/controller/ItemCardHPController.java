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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    private Label priceTitleLabel;
    @FXML
    private Label timeTitleLabel;
    @FXML
    private VBox timeContainer;

    private Timeline timeline;


    @FXML
    public void initialize() {
        if (imageContainer == null) return;

        Rectangle clip = new Rectangle(280, 240);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageContainer.setClip(clip);

        productImage.setPreserveRatio(false);

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

        productNameLabel.setText(item.getName());

        ClientImageUtil.displayImage(item.getImagesPath().get(0), "images", productImage);
        startCountdown();
    }

    private String formatPrice(double price) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return "$ " + formatter.format(price) + " USD";
    }

    private String formatTimeLeft(LocalDateTime from, LocalDateTime to) {
        if (to == null || from == null) return "N/A";

        long days = ChronoUnit.DAYS.between(from, to);
        long hours = ChronoUnit.HOURS.between(from, to) % 24;
        long minutes = ChronoUnit.MINUTES.between(from, to) % 60;
        long seconds = ChronoUnit.SECONDS.between(from, to) % 60;

        return String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();


                    // upcoming
                    if (currentItem.getStartTime() != null && now.isBefore(currentItem.getStartTime())) {
                        statusLabel.setText("UPCOMING");;

                        priceTitleLabel.setText("START PRICE");
                        priceLabel.setText(formatPrice(currentItem.getStartingPrice()));
                        endTimeLabel.setText(formatTimeLeft(now, currentItem.getStartTime()));
                        timeTitleLabel.setText("Starts on " + currentItem.getStartTime().format(dateFormatter));
                    }
                    // ongoing
                    else if (currentItem.getEndTime() != null && now.isBefore(currentItem.getEndTime())) {
                        statusLabel.setText("LIVE");

                        priceTitleLabel.setText("CURRENT BID");
                        double displayPrice = (currentItem.getHighestCurrentPrice() > 0) ? currentItem.getHighestCurrentPrice() : currentItem.getStartingPrice();
                        priceLabel.setText(formatPrice(displayPrice));
                        endTimeLabel.setText(formatTimeLeft(now, currentItem.getEndTime()));
                        timeTitleLabel.setText("Ends on " + currentItem.getEndTime().format(dateFormatter));
                    }
                    // ended
                    else {
                        statusLabel.setText("ENDED");

                        priceTitleLabel.setText("FINAL PRICE");
                        double finalPrice = (currentItem.getHighestCurrentPrice() > 0) ? currentItem.getHighestCurrentPrice() : currentItem.getStartingPrice();
                        priceLabel.setText(formatPrice(finalPrice));

                        endTimeLabel.setText("Auction Ended");
                        if (currentItem.getEndTime() != null) {
                            timeTitleLabel.setText("Ended on " + currentItem.getEndTime().format(dateFormatter));
                        }

                        timeline.stop();
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.playFromStart();
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