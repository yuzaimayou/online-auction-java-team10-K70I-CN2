package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WalletService {

    private static final WalletService instance = new WalletService();
    public static WalletService getInstance() { return instance; }

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private WalletService() {}

    /**
     * Fetch balance từ server → cập nhật UserSession → trả về [available, frozen].
     * Trả về null nếu lỗi hoặc chưa login.
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
                // Thay toàn bộ phần thenApply trong fetchAndSync():
                .thenApply(body -> {
                    try {
                        // Parse response ngoài cùng
                        JsonObject response = gson.fromJson(body, JsonObject.class);
                        if (!"success".equals(response.get("status").getAsString())) return null;

                        // data có thể là JsonObject hoặc String bị double-encode
                        JsonObject data;
                        JsonElement dataElement = response.get("data");
                        if (dataElement.isJsonObject()) {
                            data = dataElement.getAsJsonObject();
                        } else {
                            // double-encoded: data là string JSON → parse lần 2
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
}