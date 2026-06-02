package com.auction.client.ui.auction;

import com.auction.client.util.DateTimeUtil;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BidHistoryRowFactory {

    public static HBox createRow(int index, BidHistoryItemDTO bid, String currentUsername) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");

        Label lblIndex = new Label(String.valueOf(index));
        lblIndex.getStyleClass().add("history-index");

        VBox info = new VBox(2);

        String name = bid.userName.equals(currentUsername)
                ? bid.userName + " (You)"
                : bid.userName;

        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold;");

        Label lblTime = new Label(DateTimeUtil.formatForBidHistory(bid.bidTime));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        info.getChildren().addAll(lblName, lblTime);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblPrice = new Label(String.format("$ %.1f", bid.bidPrice));
        lblPrice.setStyle("-fx-text-fill: #4A835D; -fx-font-weight: bold; -fx-font-size: 16px;");

        row.getChildren().addAll(lblIndex, info, lblPrice);
        return row;
    }

    public static void renderChart(
            List<BidHistoryItemDTO> bids,
            XYChart.Series<String, Number> series
    ) {
        series.getData().clear();

        if (bids == null || bids.isEmpty()) return;

        List<BidHistoryItemDTO> copy = new ArrayList<>(bids);
        Collections.reverse(copy);

        for (BidHistoryItemDTO bid : copy) {
            if (bid.bidTime == null) continue;

            String time = DateTimeUtil.formatForChart(bid.bidTime);
            series.getData().add(
                    new XYChart.Data<>(time, bid.bidPrice)
            );
        }
    }
}