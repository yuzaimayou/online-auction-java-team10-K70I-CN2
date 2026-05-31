package com.auction.server.integration;

import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiIntegration {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + API_KEY;
    private final AiServiceClient aiServiceClient = AiServiceClient.getInstance();

    private final HttpClient httpClient;
    private final Gson gson = GsonUtil.getInstance();

    public GeminiIntegration() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String callGeminiApi(String prompt) {
        try {
            // 1. DÙNG GSON ĐỂ TẠO PAYLOAD AN TOÀN
            // Cấu trúc yêu cầu: { "contents": [ { "parts": [ { "text": "prompt" } ] } ] }
            JsonObject textObject = new JsonObject();
            textObject.addProperty("text", prompt);

            JsonArray partsArray = new JsonArray();
            partsArray.add(textObject);

            JsonObject contentsObject = new JsonObject();
            contentsObject.add("parts", partsArray);

            JsonArray contentsArray = new JsonArray();
            contentsArray.add(contentsObject);

            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contentsArray);

            // Chuyển Object thành chuỗi JSON
            String jsonPayload = gson.toJson(requestBody);

            // 2. TẠO VÀ GỬI HTTP REQUEST
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 3. XỬ LÝ KẾT QUẢ TRẢ VỀ
            if (response.statusCode() == 200) {
                return extractTextFromResponse(response.body());
            } else {
                System.err.println("Lỗi gọi Gemini API (Status Code: " + response.statusCode() + ")");
                System.err.println("Chi tiết lỗi: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối mạng hoặc lỗi hệ thống khi gọi LLM:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Hàm phụ trợ: Dùng Gson để bóc tách đúng phần văn bản từ khối JSON khổng lồ của Google
     */
    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

            // Lần mò theo cấu trúc JSON của Gemini: candidates[0].content.parts[0].text
            return responseObj.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

        } catch (Exception e) {
            System.err.println("Lỗi parse JSON phản hồi từ Gemini.");
            return jsonResponse; // Trả về chuỗi thô nếu parse lỗi để dễ debug
        }
    }
}
