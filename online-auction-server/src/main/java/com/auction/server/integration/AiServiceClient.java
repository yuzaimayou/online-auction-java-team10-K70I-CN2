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

public class AiServiceClient {
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

        // Tạo một chuỗi ngẫu nhiên làm vách ngăn (boundary) giữa các phần dữ liệu
        String boundary = "---Boundary" + UUID.randomUUID().toString();
        String crlf = "\r\n";

        try {
            List<byte[]> byteArrays = new ArrayList<>();

            // 1. Đóng gói phần Text (Tên và Mô tả)
            addTextPart(byteArrays, boundary, "name", name, crlf);
            addTextPart(byteArrays, boundary, "description", description, crlf);

            // 2. Đóng gói phần File (Các ảnh sản phẩm)
            for (Path path : imagePaths) {
                String fileName = path.getFileName().toString();
                String fileHeader = "--" + boundary + crlf +
                        "Content-Disposition: form-data; name=\"files\"; filename=\"" + fileName + "\"" + crlf +
                        "Content-Type: application/octet-stream" + crlf + crlf;

                byteArrays.add(fileHeader.getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path)); // Đọc ảnh dưới dạng byte
                byteArrays.add(crlf.getBytes(StandardCharsets.UTF_8));
            }

            // 3. Đóng gói kết thúc (End boundary)
            String endBoundary = "--" + boundary + "--" + crlf;
            byteArrays.add(endBoundary.getBytes(StandardCharsets.UTF_8));

            // 4. Gộp tất cả byte arrays thành một mảng duy nhất để gửi đi
            int totalLength = byteArrays.stream().mapToInt(b -> b.length).sum();
            byte[] multipartBody = new byte[totalLength];
            int destPos = 0;
            for (byte[] b : byteArrays) {
                System.arraycopy(b, 0, multipartBody, destPos, b.length);
                destPos += b.length;
            }

            // 5. Khởi tạo và gửi Request
            HttpRequest request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(requestUrl))
                    // BẮT BUỘC: Phải báo cho Python biết boundary mình đang dùng là gì
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body(); // Thành công
            } else {
                System.err.println("Lỗi từ AI Server khi index: " + response.statusCode());
                System.err.println("Chi tiết: " + response.body());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getRecommendations(String prompt) {
        try {
            // 1. Mã hóa từ khóa (để xử lý dấu cách, tiếng Việt...)
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());
            String requestUrl = AI_SERVER_URL + "/recommend?prompt=" + encodedPrompt;

            // 2. Tạo Request (Giống như cấu hình GET trên Postman)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // 3. Gửi Request và nhận Response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Trả về chuỗi JSON chứa danh sách recommendations
                return response.body();
            } else {
                System.err.println("Lỗi từ AI Server: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        AiServiceClient aiClient = new AiServiceClient();
        String jsonResult = aiClient.getRecommendations("áo MU");
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
