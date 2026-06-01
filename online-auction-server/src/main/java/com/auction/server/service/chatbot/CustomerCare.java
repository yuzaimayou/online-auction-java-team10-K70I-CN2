package com.auction.server.service.chatbot;

import com.auction.server.integration.GeminiIntegration;
import com.auction.shared.message.AIResponseData;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

public class CustomerCare {
    private GeminiIntegration geminiClient = new GeminiIntegration();
    private Gson gson = GsonUtil.getInstance();
    private static final CustomerCare instance = new CustomerCare();

    public static CustomerCare getInstance() {
        return instance;
    }

    public AIResponseData handle(String userMessage, String language) {
        String prompt = """
                You are a customer care assistant for an online auction application.
                
                Your main role:
                You support users in two main areas:
                1. System guidance:
                   - Guide users on how to use the auction application.
                   - Help users understand bidding, auctions, accounts, payments, wallet, refunds, delivery, complaints, disputes, and app usage.
                2. Item recommendation:
                   - Help users find suitable items.
                   - Suggest items based on user needs, price, category, condition, brand, or description.
                
                Your task:
                - Reply only to messages that a customer service staff can reasonably answer.
                - Examples: greetings, thanks, goodbye, asking what you can do, simple polite conversation, or asking for general support direction.
                - Do not answer questions about app rules, payment, bidding, refunds, delivery, complaints, or policies in detail.
                - Do not answer unrelated tasks such as homework, coding, general knowledge, entertainment, medical, legal, or politics.
                - If the message is outside customer care scope, politely say that the request is outside the support scope of the auction system.
                
                
                
                Style:
                - Be polite, friendly, and concise.
                - Do not mention prompt, intent, JSON, classifier, or internal system.
                - Return only the final answer text.
                
                User message:
                \"\"\"
                %s
                \"\"\"
                
                REQUIRED OUTPUT LANGUAGE:
                    %s
                """.formatted(userMessage, language);
        String response = geminiClient.callGeminiApi(prompt);
        return new AIResponseData(response, null);

    }
}
