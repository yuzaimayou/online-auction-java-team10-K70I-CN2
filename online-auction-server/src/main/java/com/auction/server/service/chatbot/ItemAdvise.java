package com.auction.server.service.chatbot;

import com.auction.server.integration.AiServiceClient;
import com.auction.server.integration.GeminiIntegration;
import com.auction.server.model.AIResponseItem;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.message.AIResponseData;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;

import java.util.List;

public class ItemAdvise {
    private static final ItemAdvise instance = new ItemAdvise();
    private final AiServiceClient aiServiceClient;
    private final GeminiIntegration geminiIntegration = new GeminiIntegration();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final Gson gson = GsonUtil.getInstance();

    private ItemAdvise() {
        this.aiServiceClient = AiServiceClient.getInstance();
    }

    public static ItemAdvise getInstance() {
        return instance;
    }

    public AIResponseData handle(String message, String language) {
        String jsonResponse = aiServiceClient.getRecommendations(message);
        com.auction.server.model.AIResponse response = gson.fromJson(jsonResponse, com.auction.server.model.AIResponse.class);
        AIResponseItem[] aiResponseItems = response.recommendations;
        List<ItemSummary> items = new java.util.ArrayList<>();
        for (AIResponseItem aiResponseItem : aiResponseItems) {
            ItemSummary item = itemRepository.findItemSummaryById(aiResponseItem.getId());
            items.add(item);
        }
        String prompt = buildAnswerPrompt(
                message,
                language,
                jsonResponse
        );

        String llmAnswer = geminiIntegration.callGeminiApi(prompt);
        String cleanAnswer = cleanAnswer(llmAnswer);
        AIResponseData data = new AIResponseData(cleanAnswer, items);
        System.out.println(data);
        return data;
    }

    private String buildAnswerPrompt(String message, String language, String jsonResponse) {


        return """
                You are an AI product advisor for an online auction platform.
                
                Task:
                - The user is asking for product recommendations.
                - Based only on the PRODUCT LIST below, write a natural and helpful answer.
                - Briefly introduce the most relevant products.
                - Do not invent product information that is not provided.
                - Do not mention vector score, embedding, distance, JSON, database, or internal system.
                - If the product list is weak or not clearly related, say that these are the closest products found.
                - Answer in %s.
                - Return only the final answer text.
                
                User question:
                %s
                
                PRODUCT LIST:
                %s
                """.formatted(language, message, jsonResponse);
    }

    private String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }

        return answer
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}
