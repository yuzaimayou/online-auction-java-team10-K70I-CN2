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
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeminiIntegration {
    private static final Logger LOGGER = Logger.getLogger(GeminiIntegration.class.getName());
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final HttpClient httpClient;
    private final Gson gson = GsonUtil.getInstance();

    public GeminiIntegration() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String callGeminiApi(String prompt) {
        try {
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

            String jsonPayload = gson.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromResponse(response.body());
            } else {
                LOGGER.warning("Gemini API call failed with status code: " + response.statusCode());
                LOGGER.warning("Gemini API error details: " + response.body());
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Network or system error while calling LLM", e);
            return null;
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

            return responseObj.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse Gemini JSON response", e);
            return jsonResponse;
        }
    }
}
