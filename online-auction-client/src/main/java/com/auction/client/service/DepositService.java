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
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private static DepositService instance;

    public static synchronized DepositService getInstance() {
        if (instance == null) {
            instance = new DepositService();
        }
        return instance;
    }

    /**
     * Gửi yêu cầu nạp tiền lên Server qua API
     */
    public CompletableFuture<Double> deposit(String userId, double amount) {
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
                    if (response.statusCode() == 200) {
                        JsonObject jsonResp = gson.fromJson(response.body(), JsonObject.class);
                        if (jsonResp != null && jsonResp.has("newBalance")) {
                            return jsonResp.get("newBalance").getAsDouble();
                        }
                        throw new RuntimeException("Phản hồi từ server thiếu thông tin số dư");
                    } else {
                        throw new RuntimeException("Server báo lỗi: " + response.body());
                    }
                });
    }
}