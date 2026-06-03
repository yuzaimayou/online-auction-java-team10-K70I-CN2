package com.auction.client.ui.auction;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 *  VIEW COMPONENT - BẢNG ĐIỀU KHIỂN ĐẤU GIÁ
 */
public class BidPanelView {

    private final Label statusMessageLabel;
    private final VBox bidControlsContainer;
    private final StackPane statusOverlay;
    private final Button btnAutoBidToggle;
    private final Button submitBid;

    public BidPanelView(
            Label statusMessageLabel,
            VBox bidControlsContainer,
            StackPane statusOverlay,
            Button btnAutoBidToggle,
            Button submitBid
    ) {
        this.statusMessageLabel  = statusMessageLabel;
        this.bidControlsContainer = bidControlsContainer;
        this.statusOverlay       = statusOverlay;
        this.btnAutoBidToggle    = btnAutoBidToggle;
        this.submitBid           = submitBid;
    }

    public void showOngoingState(boolean isAutoBidActive) {
        toggleInteractiveControlMode(true);
        submitBid.setDisable(isAutoBidActive);
    }

    public void showUpcomingState() {
        toggleInteractiveControlMode(false);
        statusMessageLabel.setText("⏳ This auction hasn't started yet");
        clearStatusStyles();
        statusMessageLabel.getStyleClass().add("status-upcoming");
    }

    public void showEndedState() {
        toggleInteractiveControlMode(false);
        statusMessageLabel.setText("🚫 This auction has ended");
        clearStatusStyles();
        statusMessageLabel.getStyleClass().add("status-ended");
    }

    public void showBannedState() {
        toggleInteractiveControlMode(false);
        clearStatusStyles();
        statusMessageLabel.getStyleClass().add("status-ended");
        statusMessageLabel.setText("⛔ Auction suspended by Admin");
    }

    public void showOwnerRestrictedState() {
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

    private void clearStatusStyles() {
        statusMessageLabel.getStyleClass().removeAll("status-ended", "status-upcoming");
    }
}