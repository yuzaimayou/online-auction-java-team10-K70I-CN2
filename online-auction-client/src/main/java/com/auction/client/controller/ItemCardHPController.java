package com.auction.client.controller;

import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.ItemSummary;
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
    private ItemSummary currentItem;
    private Timeline timeline;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    private StackPane imageContainer;
    @FXML
    private ImageView itemImage;
    @FXML
    private Label itemNameLabel;
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
    public void initialize() {
        if (imageContainer == null) return;

        Rectangle clip = new Rectangle(280, 240);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageContainer.setClip(clip);

        itemImage.setPreserveRatio(false);

        itemImage.imageProperty().addListener((observable, oldImage, newImage) -> {
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

        double scaleX = containerW / w;
        double scaleY = containerH / h;

        double scale = Math.max(scaleX, scaleY);

        itemImage.setFitWidth(w * scale);
        itemImage.setFitHeight(h * scale);
    }

    public void setData(ItemSummary item) {
        this.currentItem = item;

        String name = item.getName();
        if (name != null && name.length() > 50) {
            name = name.substring(0, 47) + "...";
        }

        itemNameLabel.setText(name);

        if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isEmpty()) {
            ClientImageUtil.displayImage(
                    item.getThumbnailUrl(),
                    "images",
                    itemImage,
                    400,
                    400
            );
        }
        updateUI();
        startCountdown();
    }

    private void updateUI() {
        if (currentItem == null) return;
        LocalDateTime now = LocalDateTime.now();

        // Upcoming
        if (currentItem.getStartTime() != null && now.isBefore(currentItem.getStartTime())) {
            statusLabel.setText("UPCOMING");
            statusLabel.setStyle("-fx-background-color: #fff3c4; -fx-text-fill: #eea504;");
            priceTitleLabel.setText("START PRICE");
            priceLabel.setText(formatPrice(currentItem.getCurrentPrice()));
            endTimeLabel.setText(formatTimeLeft(now, currentItem.getStartTime()));
            timeTitleLabel.setText("Starts on " + currentItem.getStartTime().format(dateFormatter));
        }
        // Live
        else if (currentItem.getEndTime() != null && now.isAfter(currentItem.getStartTime()) && now.isBefore(currentItem.getEndTime())) {
            statusLabel.setText("LIVE");
            statusLabel.setStyle("-fx-background-color: #ecfdf5");
            priceTitleLabel.setText("CURRENT BID");
            double displayPrice = currentItem.getCurrentPrice();
            priceLabel.setText(formatPrice(displayPrice));
            endTimeLabel.setText(formatTimeLeft(now, currentItem.getEndTime()));
            timeTitleLabel.setText("Ends on " + currentItem.getEndTime().format(dateFormatter));
        }
        // Ended
        else {
            statusLabel.setText("ENDED");
            statusLabel.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white;");
            priceTitleLabel.setText("FINAL PRICE");
            double finalPrice = currentItem.getCurrentPrice();
            priceLabel.setText(formatPrice(finalPrice));
            endTimeLabel.setText("Auction Ended");
            if (currentItem.getEndTime() != null) {
                timeTitleLabel.setText("Ended on " + currentItem.getEndTime().format(dateFormatter));
            }
            if (timeline != null) timeline.stop();
        }
    }

    private void startCountdown() {
        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateUI()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
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

    @FXML
    public void handleSwitchToItemPage(MouseEvent event) {
        if (timeline != null) timeline.stop();

        NavigationUtil.handleSwitchToItemPage(
                itemNameLabel,
                currentItem.getId(),
                currentItem.getName()
        );
    }
}