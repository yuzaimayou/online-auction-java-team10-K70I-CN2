package com.auction.client.controller;

import com.auction.client.service.ItemStatusService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ItemCardHPController {

    private static final double CONTAINER_W = 280.0;
    private static final double CONTAINER_H = 240.0;

    private ItemSummary currentItem;
    private Timeline    timeline;
    private AuctionStatus lastStatus;

    // Khởi tạo Service gộp chung
    private final ItemStatusService statusUiService = new ItemStatusService();

    @FXML private StackPane imageContainer;
    @FXML private ImageView itemImage;
    @FXML private Label itemNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label priceLabel;
    @FXML private Label priceTitleLabel;
    @FXML private Label timeTitleLabel;

    @FXML
    public void initialize() {

        if (imageContainer == null) return;

        itemImage.setSmooth(true);
        itemImage.setCache(true);
        ClientImageUtil.makeResponsiveCover(
                itemImage,
                imageContainer,
                30
        );
    }

    // Public API
    public void setData(ItemSummary item) {
        this.currentItem = item;

        String name = item.getName();
        if (name != null && name.length() > 50) {
            name = name.substring(0, 47) + "...";
        }
        itemNameLabel.setText(name);

        if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isEmpty()) {
            ClientImageUtil.displayImage(item.getThumbnailUrl(), "images", itemImage, CONTAINER_W * 2, CONTAINER_H * 2
            );
        }

        updateUI();
        startCountdown();
    }

    private void updateUI() {
        if (currentItem == null) return;

        // Ủy quyền hoàn toàn việc vẽ UI cho Service chung
        statusUiService.updateCardUi(currentItem, statusLabel, priceTitleLabel, priceLabel, endTimeLabel, timeTitleLabel);

        // Tối ưu giải phóng tài nguyên CPU nếu phòng kết thúc
        if (statusUiService.resolveStatus(currentItem) == AuctionStatus.ENDED && timeline != null) {
            timeline.stop();
        }
    }

    private void startCountdown() {
        if (timeline != null) timeline.stop();
        lastStatus = statusUiService.resolveStatus(currentItem);

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            AuctionStatus newStatus = statusUiService.resolveStatus(currentItem);
            if (newStatus != lastStatus) {
                lastStatus = newStatus;
                if (newStatus == AuctionStatus.ENDED && timeline != null) {
                    timeline.stop();
                }
            }
            updateUI();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    public void handleSwitchToItemPage(
            MouseEvent event
    ) {
        if (timeline != null) {
            timeline.stop();}
        if (currentItem == null) {
            return;
        }
        NavigationUtil.switchToItemPage(event,
                currentItem.getId(),
                currentItem.getName()
        );
    }
}