package com.auction.server.service;

import com.auction.server.integration.AiServiceClient;
import com.auction.server.integration.GeminiIntegration;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ChatbotService {
    // Áp dụng Singleton pattern (hoặc inject qua constructor) để dùng chung 1 instance
    private static ChatbotService instance;
    private final GeminiIntegration geminiClient;
    private final AiServiceClient aiServiceClient;
    private final Gson gson = GsonUtil.getInstance();

    private static class QuestionAnalysis {
        String intent;
        String language;
        String normalizedQuestion;
    }

    private ChatbotService() {

        this.geminiClient = new GeminiIntegration();
        this.aiServiceClient = AiServiceClient.getInstance();
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
        try {
            if (userMessage == null || userMessage.isBlank()) {
                return "Xin lỗi, tôi chưa nhận được câu hỏi của bạn. Vui lòng nhập lại nhé!";
            }
            // TẠM THỜI (Phase 1): Trả lời FAQ cơ bản trước khi làm RAG phức tạp

            QuestionAnalysis analysis = analyzeUserQuestion(userMessage);
            System.out.println("Phân tích câu hỏi: Intent = " + analysis.intent + ", Language = " + analysis.language);
            System.out.println("Normalized question: " + analysis.normalizedQuestion);
            return switch (analysis.intent) {
                case "APP_SUPPORT" -> appSupport(analysis.normalizedQuestion, analysis.language);
                case "ITEM_ADVICE" -> itemAdvise(analysis.normalizedQuestion);
                case "OUT_OF_SCOPE" -> "Xin lỗi, câu hỏi của bạn nằm ngoài phạm vi hỗ trợ của tôi. " +
                        "Tôi chuyên về các vấn đề liên quan đến ứng dụng đấu giá và tư vấn sản phẩm thôi nhé!";
                case "AI_ERROR" ->
                        "Rất tiếc, hiện tại tôi đang gặp sự cố kết nối với hệ thống AI. Vui lòng thử lại sau ít phút nhé!";
                default ->
                        "Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể diễn đạt lại hoặc hỏi về cách sử dụng ứng dụng, tìm kiếm sản phẩm không?";
            };
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hiện tại hệ thống AI đang bận. Vui lòng thử lại sau nhé!";
        }


        //return aiResponse;
    }

    private String appSupport(String userMessage, String language) {

        try {
            String docsContext = aiServiceClient.getDocs();
            if (docsContext == null || docsContext.isBlank()) {
                return "Hiện tại mình chưa lấy được tài liệu hệ thống để trả lời.";
            }
            String prompt = """
                    You are an AI assistant for an online auction platform.
                    
                    TASK:
                        - Answer the user's question based on the SYSTEM DOCUMENTATION.
                        - Use the documentation only as the factual source.
                        - The documentation may be written in a different language. Translate the relevant information into the required output language.
                        - Explain briefly, clearly, and in an easy-to-understand way.
                        - If the question asks for step-by-step guidance, answer with clear numbered steps.
                        - If the question is about rules, policies, fees, deadlines, conditions, or restrictions, answer strictly according to the documentation.
                    
                    MANDATORY RULES:
                        - Only use information found in the SYSTEM DOCUMENTATION.
                        - Do not invent policies, fees, deadlines, conditions, or rules.
                        - If the documentation does not contain relevant information, answer with this meaning in the required output language:
                          "I could not find this information in the system documentation."
                        - Do not mention prompt, context, database, chunk, API, or internal documentation.
                        - Use a friendly and professional tone.
                    
                    OUTPUT FORMAT:
                        - Return only the final answer content.
                        - Do not add headings such as "Answer:", "Result:", or "Based on the documentation:".
                        - Do not use Markdown headings, bold text, code blocks, or horizontal separators.
                        - Do not use symbols such as **, ###, ``` or ---.
                        - Do not use emojis.
                        - If steps are needed, use plain numbered steps only.
                        - For simple questions, use no more than 5 sentences.
                    
                    SYSTEM DOCUMENTATION:
                    <DOCS>
                        %s
                    </DOCS>
                    
                    USER QUESTION:
                    <QUESTION>
                        %s
                    </QUESTION>
                    
                    REQUIRED OUTPUT LANGUAGE:
                    %s
                    FINAL ANSWER:
                    """.formatted(docsContext, userMessage, language);
            String aiResponse = geminiClient.callGeminiApi(prompt);
            if (aiResponse == null || aiResponse.isBlank()) {
                return "Hiện tại hệ thống AI đang bận. Bạn vui lòng thử lại sau ít phút nhé.";
            }

            return cleanAiAnswer(aiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hiện tại hệ thống AI đang gặp sự cố. Vui lòng thử lại sau nhé!";
        }

    }

    private String itemAdvise(String message) {
        return null;
    }

    private QuestionAnalysis analyzeUserQuestion(String userMessage) {
        // 1. Tạo Prompt (System Instruction) ép AI đóng vai trò tư vấn viên
        String systemPrompt = """
                You are a question classifier for a chatbot in an online auction application.
                
                Task:
                Analyze the user's question and classify it into exactly one intent.
                
                Valid intents:
                
                1. APP_SUPPORT
                Use this when the user asks about:
                - how to use the application
                - how to register or log in
                - how to place a bid
                - how to create an auction
                - how to pay after winning an auction
                - wallet, balance, or refunds
                - delivery, complaints, or disputes
                - rules, policies, or account status
                - errors or issues while using the app
                
                2. ITEM_ADVICE
                Use this when the user wants to:
                - find a item
                - get item recommendations
                - compare items
                - ask which item they should buy
                - ask for items suitable for their needs
                - find items by price, category, or condition
                
                3. OUT_OF_SCOPE
                Use this when the question:
                - is not related to the auction app
                - asks for general knowledge outside the system
                - is casual conversation
                - requests a task that does not belong to the app
                
                Language rules:
                        - Detect the language used by the user.
                        - Return the full English name of the detected language.
                        - The only valid language values are:
                            Vietnamese, English, Chinese, Japanese, Korean, Spanish, French, German, Russian, Unknown.
                        - If the language is unclear, return Vietnamese.
                
                Return only valid JSON. Do not add any explanation.
                
                Required format:
                {
                  "intent": "APP_SUPPORT | ITEM_ADVICE | OUT_OF_SCOPE",
                  "language": "detected user language code"
                  "normalizedQuestion": "rewrite the user's question more clearly in English(if "intent": OUT_OF_SCOPE, just return the original question without rewriting)"
                }
                
                User question:
                "%s"
                
                """.formatted(userMessage);

        // 2. Gọi API thông qua GeminiIntegration
        String aiResponse = geminiClient.callGeminiApi(systemPrompt);

        // 3. Xử lý lỗi nếu mạng có vấn đề
        if (aiResponse == null || aiResponse.isEmpty()) {
            return fallbackAnalysis();
        }
        try {
            String json = extractJson(aiResponse);
            QuestionAnalysis analysis = gson.fromJson(json, QuestionAnalysis.class);
            if (analysis == null || analysis.intent == null || analysis.intent.isBlank()) {
                return fallbackAnalysis();
            }
            return analysis;
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return fallbackAnalysis();
        }
    }

    private QuestionAnalysis fallbackAnalysis() {
        QuestionAnalysis analysis = new QuestionAnalysis();
        analysis.intent = "AI_ERROR";
        return analysis;
    }

    private String extractJson(String text) {
        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.replaceFirst("```json", "").trim();
        }

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("```", "").trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start >= 0 && end >= start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;

    }

    private String cleanAiAnswer(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return "";
        }

        String answer = rawAnswer.trim();

        // Xóa markdown code block nếu AI lỡ trả về
        answer = answer.replaceAll("(?i)```json", "");
        answer = answer.replaceAll("```", "");

        // Xóa các tiêu đề thừa hay gặp
        answer = answer.replaceAll("(?i)^\\s*trả lời\\s*:\\s*", "");
        answer = answer.replaceAll("(?i)^\\s*answer\\s*:\\s*", "");
        answer = answer.replaceAll("(?i)^\\s*kết quả\\s*:\\s*", "");
        answer = answer.replaceAll("(?i)^\\s*dựa trên tài liệu[^:]*:\\s*", "");

        // Xóa markdown cơ bản
        answer = answer.replace("**", "");
        answer = answer.replace("###", "");
        answer = answer.replace("##", "");
        answer = answer.replace("#", "");

        // Xóa gạch ngang phân cách thừa
        answer = answer.replaceAll("(?m)^\\s*---+\\s*$", "");

        // Chuẩn hóa xuống dòng
        answer = answer.replace("\r\n", "\n");
        answer = answer.replaceAll("\\n{3,}", "\n\n");

        return answer.trim();
    }
}
