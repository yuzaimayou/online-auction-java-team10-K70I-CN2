package com.auction.client.ui;

import com.auction.client.service.AutoBidService;
import com.auction.client.util.CountdownTimerUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class BidPanelController {

    private final Label statusMessageLabel;
    private final VBox bidControlsContainer;
    private final StackPane statusOverlay;
    private final Button btnAutoBidToggle;
    private final Button submitBid;
    private final CountdownTimerUtil countdownTimer;
    private final AutoBidService autoBidManager;
    private final ItemStatusService statusService;

    public BidPanelController(
            Label statusMessageLabel,
            VBox bidControlsContainer,
            StackPane statusOverlay,
            Button btnAutoBidToggle,
            Button submitBid,
            CountdownTimerUtil countdownTimer,
            AutoBidService autoBidManager
    ) {
        this.statusMessageLabel  = statusMessageLabel;
        this.bidControlsContainer = bidControlsContainer;
        this.statusOverlay       = statusOverlay;
        this.btnAutoBidToggle    = btnAutoBidToggle;
        this.submitBid           = submitBid;
        this.countdownTimer      = countdownTimer;
        this.autoBidManager      = autoBidManager;
        this.statusService       = new ItemStatusService();
    }

    public void applyAuctionStatusView(Item item, String currentUserId) {
        if (item == null) return;
        if (item.getSellerId().equals(currentUserId)) {
            restrictOwnerInteractions();
            return;
        }
        AuctionStatus status = statusService.resolveStatus(item);
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

    public void startCountdown(Item item, Runnable callback) {
        AuctionStatus status = statusService.resolveStatus(item);
        java.time.LocalDateTime target = switch (status) {
            case UPCOMING -> item.getStartTime();
            case ONGOING  -> item.getEndTime();
            default       -> null;
        };
        countdownTimer.startFor(target, () -> Platform.runLater(callback));
    }

    public void applyBannedStateView(Item item) {
        countdownTimer.stop();
        autoBidManager.deactivate();
        toggleInteractiveControlMode(false);
        statusMessageLabel.getStyleClass().removeAll("status-ended", "status-upcoming");
        statusMessageLabel.getStyleClass().add("status-ended");
        statusMessageLabel.setText("⛔ Auction suspended by Admin");
        item.setStatus(AuctionStatus.BANNED);
    }

    public void restrictOwnerInteractions() {
        bidControlsContainer.setDisable(true);
        btnAutoBidToggle.setDisable(true);
        toggleInteractiveControlMode(false);
        statusMessageLabel.setText("👤 You are the owner of this item");
    }

    private void toggleInteractiveControlMode(boolean enable) {
        bidControlsContainer.setVisible(enable);
        bidControlsContainer.setManaged(enable);
        statusOverlay.setVisible(!enable);
        statusOverlay.setManaged(!enable);
    }
}