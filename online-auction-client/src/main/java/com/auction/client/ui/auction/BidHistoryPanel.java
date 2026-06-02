package com.auction.client.ui.auction;

import com.auction.client.ui.util.ToastUtil;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 🖼️ VIEW COMPONENT
 * Quản lý trực tiếp các đối tượng giao diện JavaFX và thực hiện các hành vi render đồ họa.
 */
public class BidHistoryPanel {

    private final VBox historyBidContainer;
    private final ScrollPane historyScrollPane;
    private final Label totalBidsLabel;
    private final XYChart.Series<String, Number> bidPriceSeries;

    public BidHistoryPanel(
            VBox historyBidContainer,
            ScrollPane historyScrollPane,
            Label totalBidsLabel,
            XYChart.Series<String, Number> bidPriceSeries
    ) {
        this.historyBidContainer = historyBidContainer;
        this.historyScrollPane  = historyScrollPane;
        this.totalBidsLabel     = totalBidsLabel;
        this.bidPriceSeries     = bidPriceSeries;
    }

    public void render(List<BidHistoryItemDTO> bids, String currentUsername) {
        // Vẽ đồ thị biểu đồ giá thông qua Renderer Helper
        BidHistoryRowFactory.renderChart(bids, bidPriceSeries);

        // Làm sạch container cũ để tránh Memory Leak (Rò rỉ bộ nhớ)
        historyBidContainer.getChildren().clear();

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
                    BidHistoryRowFactory.createRow(total - i, bids.get(i), currentUsername)
            );
        }
    }

    private void toggleScroll(boolean visible) {
        if (historyScrollPane != null) {
            historyScrollPane.setVisible(visible);
            historyScrollPane.setManaged(visible);
        }
    }

    public void showLoadErrorMessage(String message) {
        if (historyBidContainer.getScene() != null) {
            ToastUtil.showError(historyBidContainer.getScene(), message);
        }
    }
}