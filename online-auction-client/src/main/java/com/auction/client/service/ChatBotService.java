package com.auction.client.service;

import com.auction.client.network.ChatBotApiClient;
import com.auction.shared.message.AIResponseData;

import java.util.concurrent.CompletableFuture;

public class ChatBotService {

    private static final ChatBotService instance = new ChatBotService();
    private final ChatBotApiClient apiClient = ChatBotApiClient.getInstance();

    private ChatBotService() {}

    public static ChatBotService getInstance() {
        return instance;
    }

    public CompletableFuture<AIResponseData> sendMsg(String message) {
        return apiClient.sendMessage(message);
    }
}