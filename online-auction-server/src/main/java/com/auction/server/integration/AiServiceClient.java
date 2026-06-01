package com.auction.server.integration;

import com.auction.server.config.AppConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AiServiceClient {
    private static final Logger LOGGER = Logger.getLogger(AiServiceClient.class.getName());
    private static AiServiceClient instance;

    private static final String AI_SERVER_URL = AppConfig.getAiServerUrl();
    private final HttpClient httpClient;

    public AiServiceClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

    }

    public static AiServiceClient getInstance() {
        if (instance == null) {
            instance = new AiServiceClient();
        }
        return instance;
    }

    public String embeddingProduct(String itemId, String name, String description, List<Path> imagePaths) {
        String requestUrl = AI_SERVER_URL + "/index-product/" + itemId;
        String boundary = "---Boundary" + UUID.randomUUID().toString();
        String crlf = "\r\n";

        try {
            List<byte[]> byteArrays = new ArrayList<>();

            addTextPart(byteArrays, boundary, "name", name, crlf);
            addTextPart(byteArrays, boundary, "description", description, crlf);

            for (Path path : imagePaths) {
                String fileName = path.getFileName().toString();
                String fileHeader = "--" + boundary + crlf +
                        "Content-Disposition: form-data; name=\"files\"; filename=\"" + fileName + "\"" + crlf +
                        "Content-Type: application/octet-stream" + crlf + crlf;

                byteArrays.add(fileHeader.getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add(crlf.getBytes(StandardCharsets.UTF_8));
            }

            String endBoundary = "--" + boundary + "--" + crlf;
            byteArrays.add(endBoundary.getBytes(StandardCharsets.UTF_8));

            int totalLength = byteArrays.stream().mapToInt(b -> b.length).sum();
            byte[] multipartBody = new byte[totalLength];
            int destPos = 0;
            for (byte[] b : byteArrays) {
                System.arraycopy(b, 0, multipartBody, destPos, b.length);
                destPos += b.length;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(requestUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                LOGGER.warning("AI server index request failed: " + response.statusCode());
                LOGGER.warning("AI server response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to index product with AI service", e);
            return null;
        }
    }

    public String getRecommendations(String prompt) {
        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());
            String requestUrl = AI_SERVER_URL + "/recommend?prompt=" + encodedPrompt;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                LOGGER.warning("AI server recommendation request failed: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get AI recommendations", e);
            return null;
        }
    }

    private void addTextPart(List<byte[]> byteArrays, String boundary, String fieldName, String value, String crlf) {
        String part = "--" + boundary + crlf +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"" + crlf + crlf +
                value + crlf;
        byteArrays.add(part.getBytes(StandardCharsets.UTF_8));
    }
}
