package com.auction.client.controller.auction; // ✅ ĐÃ SỬA: Đưa về đúng package nghiệp vụ

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

/**
 * Trách nhiệm:
 * 1. Hiển thị thông tin tổng quan của một phiên đấu giá ngoài Trang chủ.
 * 2. Đếm ngược thời gian thông minh, tối ưu hiệu năng render giao diện JavaFX.
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

    // Phục vụ tính toán logic trạng thái và format chuỗi công việc
    private final ItemStatusRendered itemStatusRendered = new ItemStatusRendered();

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

        // Cập nhật giao diện toàn diện lần đầu tiên (Bao gồm cả màu sắc CSS)
        itemStatusRendered.updateCardUi(
                currentItem, statusLabel, priceTitleLabel, priceLabel, endTimeLabel, timeTitleLabel
        );

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

    // ── Timer Logic ───────────────────────────────────────────────────────────
    private void startCountdown() {
        stopCountdown();

        lastStatus = itemStatusRendered.resolveStatus(currentItem);

        // VÁ LỖI: Nếu phiên đã kết thúc hoặc bị cấm ngay từ đầu, không kích hoạt Timer làm gì
        if (lastStatus == AuctionStatus.ENDED || lastStatus == AuctionStatus.BANNED) {
            return;
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdown()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * TỐI ƯU HIỆU NĂNG: Hàm chạy mỗi giây 1 lần.
     * Chỉ cập nhật chuỗi đếm ngược thời gian, không ép JavaFX phải render lại toàn bộ CSS tĩnh.
     */
    private void tickCountdown() {
        if (currentItem == null) return;

        AuctionStatus currentStatus = itemStatusRendered.resolveStatus(currentItem);

        // Trường hợp 1: Trạng thái không đổi -> Chỉ cập nhật lại phần text đếm ngược thời gian thực
        if (currentStatus == lastStatus) {
            LocalDateTime now = LocalDateTime.now();
            if (currentStatus == AuctionStatus.UPCOMING) {
                endTimeLabel.setText(itemStatusRendered.formatTimeLeft(now, currentItem.getStartTime()));
            } else if (currentStatus == AuctionStatus.ONGOING) {
                endTimeLabel.setText(itemStatusRendered.formatTimeLeft(now, currentItem.getEndTime()));
            }
            return;
        }

        // Trường hợp 2: Có sự chuyển cảnh trạng thái (ví dụ từ UPCOMING -> ONGOING hoặc bị BANNED)
        lastStatus = currentStatus;

        // Vẽ lại toàn bộ UI (Màu sắc, Tiêu đề giá) để khớp với trạng thái mới
        itemStatusRendered.updateCardUi(
                currentItem, statusLabel, priceTitleLabel, priceLabel, endTimeLabel, timeTitleLabel
        );

        // VÁ LỖI: Dừng triệt để bộ đếm nếu trạng thái mới là kết thúc hoặc bị khóa
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

    // ── Event Handlers ────────────────────────────────────────────────────────
    @FXML
    public void handleSwitchToItemPage(MouseEvent event) {
        if (currentItem == null) return;

        stopCountdown(); // Giải phóng tài nguyên trước khi chuyển màn hình

        NavigationUtil.switchToItemPage(
                event,
                currentItem.getId(),
                currentItem.getName()
        );
    }
}