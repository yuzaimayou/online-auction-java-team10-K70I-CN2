package com.auction.server.service.chatbot;

import com.auction.server.integration.AiServiceClient;
import com.auction.server.integration.GeminiIntegration;
import com.auction.server.model.QuestionAnalysis;
import com.auction.shared.message.AIResponseData;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

public class ChatbotService {
    // Áp dụng Singleton pattern (hoặc inject qua constructor) để dùng chung 1 instance
    private static ChatbotService instance;
    private final GeminiIntegration geminiClient;
    private final AiServiceClient aiServiceClient;
    private final QuestionAnalyzer questionAnalyzer;
    private final AppSupport appSupport;
    private final ItemAdvise itemAdvise;
    private final Gson gson = GsonUtil.getInstance();


    private ChatbotService() {
        this.geminiClient = new GeminiIntegration();
        this.aiServiceClient = AiServiceClient.getInstance();
        this.questionAnalyzer = QuestionAnalyzer.getInstance();
        this.appSupport = AppSupport.getInstance();
        this.itemAdvise = ItemAdvise.getInstance();
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
    public AIResponseData handlerMessage(String userMessage) {
        AIResponseData data = new AIResponseData();
        try {
            if (userMessage == null || userMessage.isBlank()) {
                data.setAiResponse("Xin lỗi, tôi chưa nhận được câu hỏi của bạn. Vui lòng nhập lại nhé!");
            }
            QuestionAnalysis question = questionAnalyzer.analyzeUserQuestion(userMessage);
            System.out.println("Phân tích câu hỏi: Intent = " + question.intent + ", Language = " + question.language);
            System.out.println("Normalized question: " + question.normalizedQuestion);

            switch (question.intent) {
                case "APP_SUPPORT":
                    return appSupport.handle(question.normalizedQuestion, question.language);
                case "ITEM_ADVICE":
                    return itemAdvise.handle(question.normalizedQuestion, question.language);
                case "OUT_OF_SCOPE":
                    data.setAiResponse("Xin lỗi, câu hỏi của bạn nằm ngoài phạm vi hỗ trợ của tôi. " +
                            "Tôi chuyên về các vấn đề liên quan đến ứng dụng đấu giá và tư vấn sản phẩm thôi nhé!");
                    break;

                case "AI_ERROR":
                    data.setAiResponse("Rất tiếc, hiện tại tôi đang gặp sự cố kết nối với hệ thống AI. Vui lòng thử lại sau ít phút nhé!");
                    break;
                default:
                    data.setAiResponse("Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể diễn đạt lại hoặc hỏi về cách sử dụng ứng dụng, tìm kiếm sản phẩm không?");
                    break;

            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            data.setAiResponse("Xin lỗi, hiện tại hệ thống AI đang bận. Vui lòng thử lại sau nhé!");
            return data;
        }

    }
}
