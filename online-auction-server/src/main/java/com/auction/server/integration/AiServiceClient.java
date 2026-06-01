package com.auction.server.integration;

import com.auction.server.config.AppConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AiServiceClient {
    public static class AiServerUnavailableException extends RuntimeException {
        private final String message;

        public AiServerUnavailableException(String message) {
            super(message);
            this.message = message;
        }

        public AiServerUnavailableException(String message, Throwable cause) {
            super(message, cause);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(AiServiceClient.class.getName());
    private static AiServiceClient instance;

    private static final String AI_SERVER_URL = AppConfig.getAiServerUrl();
    private final HttpClient httpClient;


    private volatile String cachedDocsContext = null;

    private volatile long cachedDocsTime = 0;

    private static final long DOCS_CACHE_DURATION_MS = 12 * 60 * 60 * 1000; // 10 phút

    private final Object docsCacheLock = new Object();

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

    private String fetchDocsFromServer() {
        String requestUrl = AI_SERVER_URL + "/docs/get-docs";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                throw new AiServerUnavailableException("Failed to fetch docs from AI server. Status code: " + response.statusCode());
            }
            JsonObject root = JsonParser
                    .parseString(response.body())
                    .getAsJsonObject();
            String status = root.get("status").getAsString();
            if (!"success".equals(status)) {
                String message = root.get("message").getAsString();
                throw new AiServerUnavailableException("AI server returned error status: " + message);
            }
            return root.get("data").getAsString();
        } catch (ConnectException e) {
            throw new AiServerUnavailableException("Unable to connect to AI server at " + AI_SERVER_URL, e);
        } catch (HttpTimeoutException e) {
            throw new AiServerUnavailableException("Connection to AI server timed out at " + AI_SERVER_URL, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServerUnavailableException("Thread was interrupted while fetching docs from AI server", e);
        } catch (Exception e) {
            throw new AiServerUnavailableException("An error occurred while fetching docs from AI server: " + e.getMessage(), e);
        }
    }

    public String getDocs() {
        long now = System.currentTimeMillis();
        if (cachedDocsContext != null
                && !cachedDocsContext.isBlank()
                && now - cachedDocsTime < DOCS_CACHE_DURATION_MS) {
            return cachedDocsContext;
        }

        synchronized (docsCacheLock) {
            now = System.currentTimeMillis();
            if (cachedDocsContext != null
                    && !cachedDocsContext.isBlank()
                    && now - cachedDocsTime < DOCS_CACHE_DURATION_MS) {
                return cachedDocsContext;
            }

            String docs = fetchDocsFromServer();

            //save cache
            cachedDocsContext = docs;
            cachedDocsTime = System.currentTimeMillis();
            return docs;
        }


    }


    public String embeddingProduct(String itemId, String name, String description, List<Path> imagePaths) {
        String requestUrl = AI_SERVER_URL + "/products/index-product/" + itemId;
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

    public String getRecommendations(String prompt, int topk) {

        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());
            String requestUrl = AI_SERVER_URL + "/products/recommend?prompt=" + encodedPrompt + "&top_k=" + topk;

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

    public String getRecommendations(String prompt) {
        return getRecommendations(prompt, 5); // Mặc định trả về top 5 recommendations
    }

    public static void main(String[] args) {
        AiServiceClient aiClient = new AiServiceClient();
        String jsonResult = aiClient.getRecommendations("barca", 1);
        System.out.println("Kết quả từ AI: \n" + jsonResult);

        // Sau bước này, bạn sẽ dùng thư viện như Gson hoặc Jackson
        // để parse chuỗi jsonResult này thành List các Object trong Java.
    }

    /**
     * Hàm phụ trợ để đóng gói các trường Text (name, description)
     */
    private void addTextPart(List<byte[]> byteArrays, String boundary, String fieldName, String value, String crlf) {
        String part = "--" + boundary + crlf +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"" + crlf + crlf +
                value + crlf;
        byteArrays.add(part.getBytes(StandardCharsets.UTF_8));
    }
}
