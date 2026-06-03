package com.auction.client.network;

import com.auction.client.util.AppConfig;
import com.auction.shared.message.AIResponseData;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ChatBotApiClient {
    private static final String CHATBOT_URL = String.format("%s/api/chatbot", AppConfig.getHttpUrl());
    private static final Gson gson = GsonUtil.getInstance();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private static final ChatBotApiClient instance = new ChatBotApiClient();

    public static ChatBotApiClient getInstance() {
        return instance;
    }

    /**
     * Gửi message lên server, trả về CompletableFuture<AIResponseData>
     */
    public CompletableFuture<AIResponseData> sendMessage(String userMessage) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHATBOT_URL))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(userMessage, StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Server error: HTTP " + response.statusCode());
                    }
                    ResponseMessage responseMessage = gson.fromJson(
                            response.body(), ResponseMessage.class
                    );
                    String dataJson = gson.toJson(responseMessage.getData());
                    return gson.fromJson(dataJson, AIResponseData.class);
                });
    }
}