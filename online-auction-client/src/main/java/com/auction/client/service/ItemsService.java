package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ItemsService {
    private static ItemsService instance;
    private User loggedInUser = UserSession.getInstance().getLoggedInUser();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new GsonUtil().getInstance();

    public static ItemsService getInstance() {
        if (instance == null) {
            instance = new ItemsService();
        }
        return instance;
    }

    public CompletableFuture<ResponseMessage> getAllFromSeller() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/products?sellerId=%s", AppConfig.getHttpUrl(), loggedInUser.getId())))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));

    }

    public CompletableFuture<ResponseMessage> createItem(String jsonPayload) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/product?action=create", AppConfig.getHttpUrl())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }

    public CompletableFuture<ResponseMessage> updateItem(String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/product?action=update", AppConfig.getHttpUrl())))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload)) // Thường dùng PUT để update dữ liệu
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }

    public CompletableFuture<ResponseMessage> deleteItem(String id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/product?action=delete&itemId=%s", AppConfig.getHttpUrl(), id)))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }
}
