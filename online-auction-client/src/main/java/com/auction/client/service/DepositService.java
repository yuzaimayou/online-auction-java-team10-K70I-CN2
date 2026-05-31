package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DepositService {
    private final HttpClient httpClient = HttpClientProvider.get();
    private final Gson gson = new Gson();
    private static DepositService instance;

    public static synchronized DepositService getInstance() {
        if (instance == null) {
            instance = new DepositService();
        }
        return instance;
    }
    public CompletableFuture<Double> deposit(String userId, double amount, double currentBalance) {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("userId", userId);
        jsonReq.addProperty("amount", amount);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getApiUrl() + "/api/wallet/deposit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonReq.toString(), StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Server error: " + response.body());
                    }
                    try {
                        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
                        // Thử parse newBalance từ root trước (legacy), nếu không có thì lấy từ data
                        if (root.has("newBalance")) {
                            return root.get("newBalance").getAsDouble();
                        }
                        if (root.has("data") && root.get("data").isJsonObject()) {
                            JsonObject data = root.getAsJsonObject("data");
                            if (data.has("newBalance")) return data.get("newBalance").getAsDouble();
                            if (data.has("balance"))    return data.get("balance").getAsDouble();
                        }
                        return currentBalance + amount; // fallback
                    } catch (Exception e) {
                        e.printStackTrace();
                        return currentBalance + amount;
                    }
                });
    }
}