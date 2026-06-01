package com.auction.server.service.chatbot;

import com.auction.server.integration.GeminiIntegration;
import com.auction.shared.message.AIResponseData;

public class OutOfScope {
    private static final OutOfScope instance = new OutOfScope();
    private GeminiIntegration geminiIntegration = new GeminiIntegration();

    public static OutOfScope getInstance() {
        return instance;
    }

    public AIResponseData handle(String message, String language) {
        String prompt = """
                You are an out-of-scope response generator for an online auction chatbot.
                
                The user's message has already been classified as OUT_OF_SCOPE.
                
                Your task:
                - Do not answer the user's actual question.
                - Politely tell the user that this request is outside the support scope of the auction system.
                - Briefly remind the user that you can only help with:
                  1. guidance about using the auction system;
                  2. item search and item recommendations.
                
                Style:
                - Be polite, friendly, and concise.
                - Return only the final response text.
                - Do not mention intent, classifier, prompt, JSON, database, or internal system.
                
                User message:
                \"\"\"
                %s
                \"\"\"
                REQUIRED OUTPUT LANGUAGE:
                %s
                """.formatted(message, language);
        String response = geminiIntegration.callGeminiApi(prompt);
        return new AIResponseData(response, null);

    }
}
