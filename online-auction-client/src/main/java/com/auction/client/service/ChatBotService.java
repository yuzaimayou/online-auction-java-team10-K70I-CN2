package com.auction.client.service;

import com.auction.client.network.ChatBotApiClient;
import com.auction.shared.message.AIResponseData;

import java.util.concurrent.CompletableFuture;

/**
 * Service xử lý logic nghiệp vụ kết nối dữ liệu Chatbot AI bên phía Client.
 * Đóng vai trò tiền xử lý và làm sạch dữ liệu từ ApiClient trước khi bàn giao cho giao diện.
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
     * * @param message Nội dung tin nhắn người dùng
     * @return CompletableFuture chứa gói dữ liệu đã được chuẩn hóa an toàn
     */
    public CompletableFuture<AIResponseData> sendMsg(String message) {
        return apiClient.sendMessage(message)
                .thenApply(response -> {
                    // Nếu mất kết nối hoặc phản hồi rỗng, tạo một đối tượng fallback an toàn
                    if (response == null) {
                        AIResponseData fallback = new AIResponseData();
                        fallback.setAiResponse("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại!");
                        return fallback;
                    }

                    // Chuẩn hóa chuỗi text tránh lỗi hiển thị khi chuỗi chỉ toàn dấu cách hoặc null
                    if (response.getAiResponse() == null || response.getAiResponse().isBlank()) {
                        response.setAiResponse("");
                    }

                    return response;
                });
    }
}