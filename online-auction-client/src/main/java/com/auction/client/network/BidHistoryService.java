package com.auction.client.network;

import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BidHistoryService {

    private static volatile BidHistoryService instance;
    private final HttpClient httpClient = HttpClientProvider.get();
    private final Gson gson = GsonUtil.getInstance();

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

    public CompletableFuture<List<BidHistoryItemDTO>> getHistory(String itemId) {
        String url = String.format("%s/api/history/%s", AppConfig.getHttpUrl(), itemId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseResponse);
    }

    private List<BidHistoryItemDTO> parseResponse(String responseBody) {
        ResponseMessage response = gson.fromJson(responseBody, ResponseMessage.class);

        if (!"success".equals(response.getStatus()) || response.getData() == null) {
            return Collections.emptyList();
        }

        JsonElement jsonElement = gson.toJsonTree(response.getData());

        return gson.fromJson(
                jsonElement,
                new TypeToken<List<BidHistoryItemDTO>>() {}.getType()
        );
    }
}