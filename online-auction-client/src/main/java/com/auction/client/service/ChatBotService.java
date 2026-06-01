package com.auction.client.service;

import com.auction.shared.message.ResponseMessage;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ChatBotService {
    private static final ChatBotService instance = new ChatBotService();
    private final HttpClient httpClient;
    private final Gson gson = GsonUtil.getInstance();

    private ChatBotService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

    }

    public static ChatBotService getInstance() {
        return instance;
    }

    public CompletableFuture<ResponseMessage> sendMsg(String message) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/api/chatbot"))
                .header("Content-Type", "application/json; utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(message)))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }
}
