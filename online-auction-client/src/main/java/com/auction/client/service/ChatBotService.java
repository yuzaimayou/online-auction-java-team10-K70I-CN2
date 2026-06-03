package com.auction.client.service;

import com.auction.client.network.ChatBotApiClient;
import com.auction.shared.message.AIResponseData;

import java.util.concurrent.CompletableFuture;

/**
 * Service xử lý logic nghiệp vụ kết nối dữ liệu Chatbot AI bên phía Client.
 */
public class ChatBotService {

    private static final ChatBotService instance = new ChatBotService();
    private final ChatBotApiClient apiClient = ChatBotApiClient.getInstance();

    private ChatBotService() {}

    public static ChatBotService getInstance() {
        return instance;
    }

    /**
     * Gửi tin nhắn và tiền xử lý gói tin phản hồi để đảm bảo dữ liệu không bị null khi lên UI.
     */
    public CompletableFuture<AIResponseData> sendMsg(String message) {
        return apiClient.sendMessage(message)
                .thenApply(response -> {
                    if (response == null) {
                        AIResponseData fallback = new AIResponseData();
                        fallback.setAiResponse("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại!");
                        return fallback;
                    }
                    if (response.getAiResponse() == null || response.getAiResponse().isBlank()) {
                        response.setAiResponse("");
                    }

                    return response;
                });
    }
}