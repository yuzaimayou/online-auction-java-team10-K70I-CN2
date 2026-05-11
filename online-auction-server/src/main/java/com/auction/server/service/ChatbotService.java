package com.auction.server.service;

import com.auction.server.integration.GeminiIntegration;

public class ChatbotService {
    // Áp dụng Singleton pattern (hoặc inject qua constructor) để dùng chung 1 instance
    private static ChatbotService instance;
    private final GeminiIntegration geminiClient;

    private ChatbotService() {
        this.geminiClient = new GeminiIntegration();
    }

    public static synchronized ChatbotService getInstance() {
        if (instance == null) {
            instance = new ChatbotService();
        }
        return instance;
    }

    /**
     * Hàm xử lý chính: Nhận tin nhắn từ người dùng và trả về câu trả lời
     */
    public String processUserMessage(String userMessage) {
        // TẠM THỜI (Phase 1): Trả lời FAQ cơ bản trước khi làm RAG phức tạp

        // 1. Tạo Prompt (System Instruction) ép AI đóng vai trò tư vấn viên
        String systemPrompt = "Bạn là trợ lý ảo của một sàn đấu giá trực tuyến. " +
                "Hãy trả lời ngắn gọn, thân thiện và chuyên nghiệp. " +
                "Câu hỏi của người dùng: \"" + userMessage + "\"";

        // 2. Gọi API thông qua GeminiIntegration
        String aiResponse = geminiClient.callGeminiApi(systemPrompt);

        // 3. Xử lý lỗi nếu mạng có vấn đề
        if (aiResponse == null || aiResponse.isEmpty()) {
            return "Xin lỗi, hiện tại hệ thống AI đang bận. Vui lòng thử lại sau nhé!";
        }

        return aiResponse;
    }
}
