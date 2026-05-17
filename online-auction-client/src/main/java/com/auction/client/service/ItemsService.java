package com.auction.client.service;

import com.auction.shared.model.item.ItemSummary;
import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.auction.BidTransaction;
import com.auction.shared.model.item.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ItemsService {
    private static volatile ItemsService instance;
    private final HttpClient httpClient;
    private final Gson gson;

    private ItemsService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = GsonUtil.getInstance();
    }

    public static ItemsService getInstance() {
        if (instance == null) {
            synchronized (ItemsService.class) {
                if (instance == null) {
                    instance = new ItemsService();
                }
            }
        }
        return instance;
    }

    // GET ITEM BY ID
    public CompletableFuture<Item> getItemById(
            String itemId,
            String userId
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s?userId=%s",
                                        AppConfig.getHttpUrl(),
                                        itemId, userId)
                        )
                )
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response =
                            gson.fromJson(body, ResponseMessage.class);

                    if (!"success".equals(response.getStatus()) || response.getData() == null) {
                        throw new RuntimeException(
                                response.getMessage()
                        );
                    }

                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.fromJson(
                            jsonElement,
                            Item.class
                    );
                });
    }

    // GET ALL ITEMS FROM SELLER
    public CompletableFuture<ResponseMessage> getAllFromSeller(
            String sellerId
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items?sellerId=%s",
                                        AppConfig.getHttpUrl(),
                                        sellerId)
                        )
                )
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()
                ).thenApply(response ->
                        gson.fromJson(
                                response.body(),
                                ResponseMessage.class
                        )
                );
    }

    // CREATE ITEM
    public CompletableFuture<ResponseMessage> createItem(String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items", AppConfig.getHttpUrl()
                                )
                        )
                )
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response ->
                        gson.fromJson(
                                response.body(),
                                ResponseMessage.class
                        )
                );
    }

    // UPDATE ITEM
    public CompletableFuture<ResponseMessage> updateItem(
            String jsonPayload,
            String itemId
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s", AppConfig.getHttpUrl(), itemId
                                )
                        )
                )
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response ->
                        gson.fromJson(
                                response.body(),
                                ResponseMessage.class)
                );
    }

    // DELETE ITEM
    public CompletableFuture<ResponseMessage> deleteItem(
            String itemId
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s", AppConfig.getHttpUrl(), itemId)))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(response ->
                        gson.fromJson(response.body(), ResponseMessage.class)
                );
    }


// GET BID HISTORY
    public CompletableFuture<List<BidTransaction>> getBidHistory(
            String itemId
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/bids/history/%s", AppConfig.getHttpUrl(), itemId)))
                .GET()
                .build();

        return httpClient.sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response = gson.fromJson(body, ResponseMessage.class);

                    if (!"success".equals(response.getStatus()) || response.getData() == null) {
                        throw new RuntimeException(
                                response.getMessage()
                        );
                    }

                    Type listType = new TypeToken<List<BidTransaction>>() {}.getType();
                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.fromJson(jsonElement, listType
                    );
                });
    }
    public CompletableFuture<List<ItemSummary>> getItems(
            String search,
            String category
    ) {

        StringBuilder url = new StringBuilder(
                String.format("%s/api/items?", AppConfig.getHttpUrl())
        );

        boolean hasParam = false;
        if (search != null && !search.isBlank()) {
            url.append("search=").append(search.trim());
            hasParam = true;
        }

        if (category != null &&
                !category.equalsIgnoreCase("ALL")) {
            if (hasParam) {
                url.append("&");
            }
            url.append("category=").append(category);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET()
                .build();

        return httpClient.sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response =
                            gson.fromJson(body, ResponseMessage.class);

                    if (!"success".equals(response.getStatus())) {
                        throw new RuntimeException(
                                response.getMessage()
                        );
                    }
                    Type listType =
                            new TypeToken<List<ItemSummary>>() {}.getType();
                    JsonElement jsonElement =
                            gson.toJsonTree(response.getData());
                    return gson.fromJson(jsonElement, listType);
                });
    }
}