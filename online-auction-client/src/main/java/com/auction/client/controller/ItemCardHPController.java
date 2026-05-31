package com.auction.client.controller;

import com.auction.client.util.ItemStatusService;
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

/**
 * Trách nhiệm:
 * 1. Hiển thị thông tin tổng quan của một phiên đấu giá.
 * 2. Đếm ngược thời gian và tự động cập nhật UI khi chuyển trạng thái.
 */
public class ItemCardHPController {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double CONTAINER_W   = 280.0;
    private static final double CONTAINER_H   = 240.0;
    private static final double CORNER_RADIUS = 30.0;

    // ── State & Dependencies ──────────────────────────────────────────────────
    private ItemSummary   currentItem;
    private Timeline      timeline;
    private AuctionStatus lastStatus;

    // (Lưu ý: Nếu ItemStatusService không chứa state, em nên dùng Singleton hoặc biến static để tối ưu bộ nhớ)
    private final ItemStatusService statusUiService = new ItemStatusService();

    // ── FXML Fields ───────────────────────────────────────────────────────────
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (imageContainer == null) return;

        itemImage.setSmooth(true);
        itemImage.setCache(true);
        ClientImageUtil.makeResponsiveCover(itemImage, imageContainer, CORNER_RADIUS);

        itemNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void setData(ItemSummary item) {
        this.currentItem = item;
        if (currentItem == null) return;

        itemNameLabel.setText(item.getName());
        loadThumbnail(item.getThumbnailUrl());

        updateUI();
        startCountdown();
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
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

    private void updateUI() {
        statusUiService.updateCardUi(
                currentItem, statusLabel, priceTitleLabel, priceLabel, endTimeLabel, timeTitleLabel
        );
    }

    // ── Timer Logic ───────────────────────────────────────────────────────────
    private void startCountdown() {
        stopCountdown();

        lastStatus = statusUiService.resolveStatus(currentItem);
        if (lastStatus == AuctionStatus.ENDED) {
            return;
        }
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> checkAndTransitionStatus()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkAndTransitionStatus() {
        AuctionStatus newStatus = statusUiService.resolveStatus(currentItem);

        updateUI();

        if (newStatus != lastStatus) {
            lastStatus = newStatus;
            if (newStatus == AuctionStatus.ENDED) {
                stopCountdown();
            }
        }
    }

    private void stopCountdown() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    // ── Event Handlers ────────────────────────────────────────────────────────
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