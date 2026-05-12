package com.auction.server.service;

import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;

public class SearchItemTest {

    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU TEST TÌM KIẾM SẢN PHẨM ===");

        try {
            // 1. Khởi tạo kết nối Database (Bắt buộc để Repository có thể query)
            // Lấy theo cách bạn khởi tạo trong MainServer.java
            DatabaseManager.init();
            DatabaseInit.init();

            // Lấy instance của service
            ItemService itemService = ItemService.getInstance();

            // Kịch bản 1: Tìm kiếm cơ bản 1 từ khóa
            System.out.println("\n[Test 1] Tìm kiếm với từ khóa 'ps':");
            runTest(itemService, "search=ps&page=0");

            // Kịch bản 2: Tìm kiếm nhiều từ khóa
            System.out.println("\n[Test 2] Tìm kiếm nhiều từ khóa 'áo psg':");
            runTest(itemService, "search=áo psg&page=0");

            // Kịch bản 3: Test phân trang (Xem thêm - Lấy trang số 1, tức là bỏ qua 10 cái đầu)
            System.out.println("\n[Test 3] Xem thêm trang 2 của từ khóa 'ps':");
            runTest(itemService, "search=ps&page=1");

            // Kịch bản 4: Tìm kiếm không ra kết quả
            System.out.println("\n[Test 4] Tìm kiếm từ khóa không tồn tại 'xyz123':");
            runTest(itemService, "search=xyz123&page=0");

            // Kịch bản 5: Lấy toàn bộ danh sách (Không truyền search)
            System.out.println("\n[Test 5] Lấy toàn bộ danh sách (Không truyền từ khóa):");
            runTest(itemService, "");

        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng trong quá trình test: ");
            e.printStackTrace();
        }
    }

    /**
     * Hàm hỗ trợ chạy test và in kết quả ra màn hình cho đẹp
     */
    private static void runTest(ItemService itemService, String query) {
        try {
            // Gọi hàm getItems bạn vừa cập nhật
            var results = itemService.getItems(query);

            if (results == null || results.isEmpty()) {
                System.out.println("   -> [Trống] Không tìm thấy sản phẩm nào!");
            } else {
                System.out.println("   -> [Thành công] Tìm thấy " + results.size() + " sản phẩm:");

                // In ra thông tin từng sản phẩm
                for (int i = 0; i < results.size(); i++) {
                    var item = results.get(i);
                    // Lưu ý: Sửa .getItemName() thành phương thức get tên tương ứng trong class của bạn
                    System.out.println("      " + (i + 1) + ". " + item.toString());
                }
            }
        } catch (Exception e) {
            System.out.println("   -> [Lỗi] Quá trình lấy dữ liệu thất bại: " + e.getMessage());
        }
        System.out.println("--------------------------------------------------");
    }
}