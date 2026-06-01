package com.auction.server.service.chatbot;

import com.auction.server.integration.GeminiIntegration;
import com.auction.server.model.QuestionAnalysis;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class QuestionAnalyzer {
    private GeminiIntegration geminiClient = new GeminiIntegration();
    private Gson gson = GsonUtil.getInstance();
    private static final QuestionAnalyzer instance = new QuestionAnalyzer();

    private QuestionAnalyzer() {
    }

    public static QuestionAnalyzer getInstance() {
        return instance;
    }

    public QuestionAnalysis analyzeUserQuestion(String userMessage) {
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
}
