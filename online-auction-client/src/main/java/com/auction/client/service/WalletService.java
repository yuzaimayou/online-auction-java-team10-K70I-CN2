package com.auction.client.service;

import com.auction.client.network.HttpClientProvider;
import com.auction.client.util.AppConfig;
import com.auction.shared.model.account.User;
import com.auction.shared.util.GsonUtil; // Dùng chung Gson chuẩn của dự án
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * TRÁCH NHIỆM DUY NHẤT: Quản lý toàn bộ các thao tác liên quan đến Ví tài chính
 * (Lấy số dư, Nạp tiền) và đồng bộ trạng thái UserSession cục bộ.
 */
public class WalletService {

    private static final WalletService instance = new WalletService();
    public static WalletService getInstance() { return instance; }

    private final HttpClient httpClient = HttpClientProvider.get();
    private final Gson gson = GsonUtil.getInstance();

    private WalletService() {}

    /**
     *  Lấy số dư từ server
     */
    public CompletableFuture<double[]> fetchAndSync() {
        User user = UserSession.getInstance().getLoggedInUser();
        if (user == null) return CompletableFuture.completedFuture(null);

        String url = AppConfig.getHttpUrl() + "/api/wallet/balance?userId=" + user.getId();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        JsonObject response = gson.fromJson(body, JsonObject.class);
                        if (!"success".equals(response.get("status").getAsString())) return null;

                        JsonObject data;
                        JsonElement dataElement = response.get("data");
                        if (dataElement.isJsonObject()) {
                            data = dataElement.getAsJsonObject();
                        } else {
                            data = gson.fromJson(dataElement.getAsString(), JsonObject.class);
                        }

                        double available = data.get("balance").getAsDouble();
                        double frozen    = data.get("frozenBalance").getAsDouble();

                        user.setBalance(available);
                        user.setFrozenBalance(frozen);
                        UserSession.getInstance().setLoggedInUser(user);

                        return new double[]{available, frozen};
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    /**
     * TÍNH NĂNG : Nạp tiền vào ví
     */
    public CompletableFuture<Double> deposit(String userId, double amount) {
        if (amount <= 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Amount must be greater than 0"));
        }

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
                        double newBalance = -1;

                        if (root.has("newBalance")) {
                            newBalance = root.get("newBalance").getAsDouble();
                        } else if (root.has("data") && root.get("data").isJsonObject()) {
                            JsonObject data = root.getAsJsonObject("data");
                            if (data.has("newBalance")) {
                                newBalance = data.get("newBalance").getAsDouble();
                            } else if (data.has("balance")) {
                                newBalance = data.get("balance").getAsDouble();
                            }
                        }

                        if (newBalance != -1) {
                            User user = UserSession.getInstance().getLoggedInUser();
                            if (user != null && userId.equals(user.getId())) {
                                user.setBalance(newBalance);
                                UserSession.getInstance().setLoggedInUser(user);
                            }
                            return newBalance;
                        }

                        throw new RuntimeException("Could not parse new balance from server response");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to process deposit response", e);
                    }
                });
    }
}