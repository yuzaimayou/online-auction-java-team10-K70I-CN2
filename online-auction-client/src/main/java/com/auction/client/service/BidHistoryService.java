package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BidHistoryService {
    private static volatile BidHistoryService instance;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = GsonUtil.getInstance();

    private BidHistoryService() {}

    public static BidHistoryService getInstance() {
        if (instance == null) {
            synchronized (BidHistoryService.class) {
                if (instance == null) {
                    instance = new BidHistoryService();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy lịch sử đấu giá từ Server API
     */
    public CompletableFuture<List<BidHistoryItemDTO>> getHistory(String itemId) {
        String url = String.format("%s/api/history/%s", AppConfig.getHttpUrl(), itemId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(responseBody -> {
                    ResponseMessage response = gson.fromJson(responseBody, ResponseMessage.class);
                    if (!"success".equals(response.getStatus()) || response.getData() == null) {
                        System.err.println("[BidHistoryClientService] Failed: " + response.getMessage());
                        System.out.println("[DEBUG] Fetching history URL: " + url);
                        return Collections.<BidHistoryItemDTO>emptyList();
                    }
                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.<List<BidHistoryItemDTO>>fromJson(
                            jsonElement, new TypeToken<List<BidHistoryItemDTO>>() {}.getType()
                    );
                });
    }

    /**
     * Sinh giao diện động cho một hàng lịch sử đấu giá (Bid Row)
     * Được chuyển từ ClientUiUtil cũ sang đây theo yêu cầu.
     */
    public static HBox createBidRow(int index, BidHistoryItemDTO bid, String currentUsername) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");

        Label lblIndex = new Label(String.valueOf(index));
        lblIndex.getStyleClass().add("history-index");
        lblIndex.setStyle("-fx-text-fill: #000000;");

        VBox info = new VBox(2);
        String name = bid.userName.equals(currentUsername) ? bid.userName + " (You)" : bid.userName;
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label lblTime = new Label(bid.bidTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        info.getChildren().addAll(lblName, lblTime);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblPrice = new Label(String.format("$ %.1f", bid.bidPrice));
        lblPrice.setStyle("-fx-text-fill: #4A835D; -fx-font-weight: bold; -fx-font-size: 16px;");

        row.getChildren().addAll(lblIndex, info, lblPrice);
        return row;
    }
}