package com.auction.client.service;

import com.auction.client.util.CountdownTimerUtil;
import com.auction.shared.constant.ItemStatusConstants;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ItemStatusService {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // =========================================================================
    // 1. COMMON LOGIC (Dùng chung cho cả Item và ItemSummary)
    // =========================================================================

    public AuctionStatus computeStatus(LocalDateTime start, LocalDateTime end) {
        return AuctionStatus.compute(start, end);
    }

    public String formatPrice(double price) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return "$ " + formatter.format(price) + " USD";
    }

    public String formatTimeLeft(LocalDateTime from, LocalDateTime to) {
        if (to == null || from == null) return "N/A";
        if (from.isAfter(to)) return "00d 00h 00m 00s";

        long days    = ChronoUnit.DAYS.between(from, to);
        long hours   = ChronoUnit.HOURS.between(from, to)   % 24;
        long minutes = ChronoUnit.MINUTES.between(from, to) % 60;
        long seconds = ChronoUnit.SECONDS.between(from, to) % 60;

        return String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

    // =========================================================================
    // 2. LOGIC CHO THẺ CARD (STATELESS - Truyền UI Node vào tham số)
    // =========================================================================

    public AuctionStatus resolveStatus(ItemSummary item) {
        if (item == null) return AuctionStatus.ENDED;
        return computeStatus(item.getStartTime(), item.getEndTime());
    }

    public void updateCardUi(ItemSummary item, Label statusLabel, Label priceTitleLabel,
                             Label priceLabel, Label endTimeLabel, Label timeTitleLabel) {
        if (item == null) return;

        AuctionStatus status = resolveStatus(item);
        LocalDateTime now = LocalDateTime.now();

        switch (status) {
            case UPCOMING -> {
                statusLabel.setText("UPCOMING");
                statusLabel.setStyle("-fx-background-color: #fff3c4; -fx-text-fill: #eea504;");
                priceTitleLabel.setText("START PRICE");
                priceLabel.setText(formatPrice(item.getCurrentPrice()));
                endTimeLabel.setText(formatTimeLeft(now, item.getStartTime()));
                timeTitleLabel.setText("Starts on " + item.getStartTime().format(dateFormatter));
            }
            case ONGOING -> {
                statusLabel.setText("LIVE");
                statusLabel.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #10b981;");
                priceTitleLabel.setText("CURRENT BID");
                priceLabel.setText(formatPrice(item.getCurrentPrice()));
                endTimeLabel.setText(formatTimeLeft(now, item.getEndTime()));
                timeTitleLabel.setText("Ends on " + item.getEndTime().format(dateFormatter));
            }
            default -> { // ENDED hoặc BANNED
                statusLabel.setText("ENDED");
                statusLabel.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white;");
                priceTitleLabel.setText("FINAL PRICE");
                priceLabel.setText(formatPrice(item.getCurrentPrice()));
                endTimeLabel.setText("Auction Ended");
                if (item.getEndTime() != null) {
                    timeTitleLabel.setText("Ended on " + item.getEndTime().format(dateFormatter));
                }
            }
        }
    }

    // =========================================================================
    // 3. LOGIC CHO TRANG CHI TIẾT (STATEFUL - Lưu trữ UI Node)
    // =========================================================================

    private Label statusMessageLabel;
    private VBox bidControlsContainer;
    private StackPane statusOverlay;
    private Button btnAutoBidToggle;
    private Button submitBid;
    private CountdownTimerUtil countdownTimer;
    private AutoBidService autoBidManager;

    public void attachUiControls(Label statusMessageLabel, VBox bidControlsContainer, StackPane statusOverlay,
                                 Button btnAutoBidToggle, Button submitBid, CountdownTimerUtil countdownTimer,
                                 AutoBidService autoBidManager) {
        this.statusMessageLabel = statusMessageLabel;
        this.bidControlsContainer = bidControlsContainer;
        this.statusOverlay = statusOverlay;
        this.btnAutoBidToggle = btnAutoBidToggle;
        this.submitBid = submitBid;
        this.countdownTimer = countdownTimer;
        this.autoBidManager = autoBidManager;
    }

    public AuctionStatus resolveStatus(Item item) {
        if (item == null) return AuctionStatus.ENDED;
        return computeStatus(item.getStartTime(), item.getEndTime());
    }

    public boolean isOngoing(Item item) {
        return resolveStatus(item) == AuctionStatus.ONGOING;
    }

    public void applyAuctionStatusView(Item item, String currentUserId) {
        if (item == null) return;

        if (item.getSellerId().equals(currentUserId)) {
            restrictOwnerInteractions();
            return;
        }

        AuctionStatus status = resolveStatus(item);
        item.setStatus(status.getDisplayName());
        statusMessageLabel.getStyleClass().removeAll("status-ended", "status-upcoming");

        switch (status) {
            case ONGOING -> {
                toggleInteractiveControlMode(true);
                submitBid.setDisable(autoBidManager.isActive());
            }
            case UPCOMING -> {
                toggleInteractiveControlMode(false);
                statusMessageLabel.setText("⏳ This auction hasn't started yet");
                statusMessageLabel.getStyleClass().add("status-upcoming");
            }
            case ENDED -> {
                toggleInteractiveControlMode(false);
                statusMessageLabel.setText("🚫 This auction has ended");
                statusMessageLabel.getStyleClass().add("status-ended");
            }
            case BANNED -> applyBannedStateView(item);
        }
    }

    public void applyBannedStateView(Item item) {
        if (countdownTimer != null) countdownTimer.stop();
        toggleInteractiveControlMode(false);
        autoBidManager.deactivate();

        statusMessageLabel.getStyleClass().removeAll("status-ended", "status-upcoming");
        statusMessageLabel.getStyleClass().add("status-ended");
        statusMessageLabel.setText("⛔ Auction Suspended by Admin");

        if (item != null) item.setStatus(ItemStatusConstants.BANNED);
    }

    public void restrictOwnerInteractions() {
        bidControlsContainer.setDisable(true);
        btnAutoBidToggle.setDisable(true);
        toggleInteractiveControlMode(false);
        statusMessageLabel.setText("👤 You are the owner of this item");
    }

    public void startCountdown(Item item, Runnable onStatusTransition) {
        AuctionStatus status = resolveStatus(item);
        LocalDateTime targetTime = (status == AuctionStatus.UPCOMING) ? item.getStartTime()
                : (status == AuctionStatus.ONGOING)  ? item.getEndTime() : null;

        countdownTimer.startFor(targetTime, () -> {
            AuctionStatus newStatus = resolveStatus(item);
            item.setStatus(newStatus.getDisplayName());
            Platform.runLater(onStatusTransition);
        });
    }

    private void toggleInteractiveControlMode(boolean isInteractive) {
        bidControlsContainer.setVisible(isInteractive);
        bidControlsContainer.setManaged(isInteractive);
        statusOverlay.setVisible(!isInteractive);
        statusOverlay.setManaged(!isInteractive);
    }
}