package com.auction.server.integration;

import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;
import com.auction.server.service.chatbot.ChatbotService;
import com.auction.shared.message.AIResponseData;
import io.github.cdimascio.dotenv.Dotenv;

public class testAI {
    private static final Dotenv dotenv = Dotenv.load();
    private static final ChatbotService chatBot = ChatbotService.getInstance();

    public static void main(String[] agrs) {
        DatabaseManager.init();
        DatabaseInit.init();
        String checkKey = dotenv.get("GEMINI_API_KEY");
        if (checkKey == null || checkKey.isEmpty()) {
            System.err.println("LỖI: Chưa tìm thấy biến môi trường GEMINI_API_KEY.");
            System.err.println("Vui lòng cấu hình biến môi trường trước khi chạy!");
            return;
        }
        System.out.println("1. Đang khởi tạo kết nối với Gemini API...");

        String testPrompt = " tìm kiếm cho tôi áo đấu của yamal";

        System.out.println("2. Đang gửi prompt thử nghiệm:");
        System.out.println("   " + testPrompt.replace("\n", "\n   "));
        System.out.println("\n3. Đợi AI suy nghĩ và phản hồi (có thể mất 2-4 giây)...");

        // Gọi API
        long startTime = System.currentTimeMillis();
        //String response = geminiClient.callGeminiApi(testPrompt);
        AIResponseData response = chatBot.handlerMessage(testPrompt);
        long endTime = System.currentTimeMillis();

        // In kết quả
        System.out.println("\n====== KẾT QUẢ TỪ AI ======");
        if (response != null) {
            System.out.println(response);
            System.out.println("===========================");
            System.out.println("-> Thời gian phản hồi: " + (endTime - startTime) + " ms");
        } else {
            System.out.println("-> Thất bại: Không nhận được phản hồi hợp lệ.");
        }
    }
}
