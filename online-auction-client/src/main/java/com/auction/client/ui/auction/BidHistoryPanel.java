package com.auction.client.ui.auction;

import com.auction.client.network.BidHistoryApiClient;
import com.auction.client.ui.util.ToastUtil;
import com.auction.shared.model.account.User;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class BidHistoryPanel {

    private final BidHistoryApiClient bidHistoryApiClient;
    private final User user;
    private final VBox historyBidContainer;
    private final ScrollPane historyScrollPane;
    private final Label totalBidsLabel;
    private final XYChart.Series<String, Number> bidPriceSeries;

    public BidHistoryPanel(
            BidHistoryApiClient bidHistoryApiClient,
            User user,
            VBox historyBidContainer,
            ScrollPane historyScrollPane,
            Label totalBidsLabel,
            XYChart.Series<String, Number> bidPriceSeries
    ) {
        this.bidHistoryApiClient = bidHistoryApiClient;
        this.user               = user;
        this.historyBidContainer = historyBidContainer;
        this.historyScrollPane  = historyScrollPane;
        this.totalBidsLabel     = totalBidsLabel;
        this.bidPriceSeries     = bidPriceSeries;
    }

    public void load(String itemId) {
        bidHistoryApiClient.getHistory(itemId)
                .thenAccept(bids -> Platform.runLater(() -> render(bids)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            ToastUtil.showError(historyBidContainer.getScene(),
                                    "Failed to load bid history"));
                    return null;
                });
    }

    private void render(List<BidHistoryItemDTO> bids) {
        BidHistoryUiRenderer.renderChart(bids, bidPriceSeries);
        historyBidContainer.getChildren().clear(); // ← fix memory leak

        if (bids == null || bids.isEmpty()) {
            totalBidsLabel.setText("0 bids");
            toggleScroll(false);
            return;
        }

        toggleScroll(true);
        totalBidsLabel.setText(bids.size() + " bids");

        int total = bids.size();
        for (int i = 0; i < total; i++) {
            historyBidContainer.getChildren().add(
                    BidHistoryUiRenderer.createRow(total - i, bids.get(i), user.getUsername())
            );
        }
    }

    private void toggleScroll(boolean visible) {
        if (historyScrollPane != null) {
            historyScrollPane.setVisible(visible);
            historyScrollPane.setManaged(visible);
        }
    }
}