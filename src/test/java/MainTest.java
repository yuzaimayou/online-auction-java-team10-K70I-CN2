import com.auction.server.service.AuthService;

public class MainTest {
    public static void main(String[] args) {
        AuthService authService = new AuthService();

        System.out.println("--- TEST ĐĂNG NHẬP SAI ---");
        authService.login("admin", "sai_pass"); // Sẽ báo lỗi


        System.out.println("\n--- TEST ĐĂNG KÝ MỚI ---");
        authService.register("nguyenvana", "password123", "Bidder");

        System.out.println("\n--- TEST ĐĂNG NHẬP TÀI KHOẢN VỪA TẠO ---");
        authService.login("nguyenvana", "password123");
    }
}
