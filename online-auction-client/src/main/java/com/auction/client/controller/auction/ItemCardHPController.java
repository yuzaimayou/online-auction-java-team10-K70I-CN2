package com.auction.client.controller.auction;

import com.auction.client.ui.item.ItemStatusRendered; // Đã sửa import tương ứng
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalDateTime;

public class ItemCardHPController {

    private static final double CONTAINER_W   = 280.0;
    private static final double CONTAINER_H   = 240.0;
    private static final double CORNER_RADIUS = 30.0;

    private ItemSummary   currentItem;
    private Timeline      timeline;
    private AuctionStatus lastStatus;

    // Phục vụ tính toán logic trạng thái
    private final ItemStatusRendered itemStatusRendered = new ItemStatusRendered();

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

        itemImage.setSmooth(true);
        itemImage.setCache(true);
        ClientImageUtil.makeResponsiveCover(itemImage, imageContainer, CORNER_RADIUS);
        itemNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    }

    public void setData(ItemSummary item) {
        this.currentItem = item;
        if (currentItem == null) return;

        itemNameLabel.setText(item.getName());
        loadThumbnail(item.getThumbnailUrl());

        itemStatusRendered.updateCardUi(
                currentItem, statusLabel, priceTitleLabel, priceLabel, endTimeLabel, timeTitleLabel
        );

        startCountdown();
    }

    private void loadThumbnail(String thumbnailUrl) {
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            ClientImageUtil.displayImage(
                    thumbnailUrl,
                    "images",
                    itemImage,
                    CONTAINER_W * 2,
                    CONTAINER_H * 2
            );
        }
    }

    private void startCountdown() {
        stopCountdown();

        lastStatus = itemStatusRendered.resolveStatus(currentItem);

        if (lastStatus == AuctionStatus.ENDED || lastStatus == AuctionStatus.BANNED) {
            return;
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdown()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void tickCountdown() {
        if (currentItem == null) return;

        AuctionStatus currentStatus = itemStatusRendered.resolveStatus(currentItem);

        if (currentStatus == lastStatus) {
            LocalDateTime now = LocalDateTime.now();
            if (currentStatus == AuctionStatus.UPCOMING) {
                endTimeLabel.setText(itemStatusRendered.formatTimeLeft(now, currentItem.getStartTime()));
            } else if (currentStatus == AuctionStatus.ONGOING) {
                endTimeLabel.setText(itemStatusRendered.formatTimeLeft(now, currentItem.getEndTime()));
            }
            return;
        }
        lastStatus = currentStatus;

        itemStatusRendered.updateCardUi(
                currentItem, statusLabel, priceTitleLabel, priceLabel, endTimeLabel, timeTitleLabel
        );

        if (currentStatus == AuctionStatus.ENDED || currentStatus == AuctionStatus.BANNED) {
            stopCountdown();
        }
    }

    public void stopCountdown() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    @FXML
    public void handleSwitchToItemPage(MouseEvent event) {
        if (currentItem == null) return;
        stopCountdown();

        NavigationUtil.switchToItemPage(
                event,
                currentItem.getId(),
                currentItem.getName()
        );
    }
}