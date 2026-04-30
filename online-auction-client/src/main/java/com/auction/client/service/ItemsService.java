package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ItemsService {
    private static ItemsService instance;
    private User loggedInUser = UserSession.getInstance().getLoggedInUser();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

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
}
