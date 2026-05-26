package com.auction.server.integration;

import io.github.cdimascio.dotenv.Dotenv;

public class testAI {
    private static final Dotenv dotenv = Dotenv.load();

    public static void main(String[] agrs) {
        String checkKey = dotenv.get("GEMINI_API_KEY");
        if (checkKey == null || checkKey.isEmpty()) {
            System.err.println("LỖI: Chưa tìm thấy biến môi trường GEMINI_API_KEY.");
            System.err.println("Vui lòng cấu hình biến môi trường trước khi chạy!");
            return;
        }
        System.out.println("1. Đang khởi tạo kết nối với Gemini API...");
        GeminiIntegration geminiClient = new GeminiIntegration();
        String testPrompt = "Dựa vào câu nói sau của người dùng, nếu liên quan đến giá tiền hãy chuyển về đơn vị tiền tệ USD, hãy trích xuất các tiêu chí tìm kiếm sản phẩm theo định dạng JSON.\n" +
                "Chỉ trả về JSON, không giải thích gì thêm.\n" +
                "Các trường cần có: \"category\" (Electronics, Art, Vehicle, Fashion), \"keyword\", \"maxPrice\".\n" +
                "Câu nói của người dùng: \"Tôi đang tìm một bức tranh phong cảnh giá dưới 10 triệu vnd để treo phòng khách.\"";

        System.out.println("2. Đang gửi prompt thử nghiệm:");
        System.out.println("   " + testPrompt.replace("\n", "\n   "));
        System.out.println("\n3. Đợi AI suy nghĩ và phản hồi (có thể mất 2-4 giây)...");

        // Gọi API
        long startTime = System.currentTimeMillis();
        String response = geminiClient.callGeminiApi(testPrompt);
        long endTime = System.currentTimeMillis();

        // In kết quả
        System.out.println("\n====== KẾT QUẢ TỪ AI ======");
        if (response != null) {
            System.out.println(response);
            System.out.println("===========================");
            System.out.println("-> Thời gian phản hồi: " + (endTime - startTime) + " ms");
        } else {
            System.out.println("-> Thất bại: Không nhận được phản hồi hợp lệ.");
        }
    }
}
