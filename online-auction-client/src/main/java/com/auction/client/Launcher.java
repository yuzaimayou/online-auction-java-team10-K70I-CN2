package com.auction.client;

/**
 * Entry point dùng khi chạy fat JAR (java -jar).
 *
 * JavaFX yêu cầu class khởi động KHÔNG được extends Application trực tiếp
 * khi chạy từ classpath (fat JAR). Class này đóng vai trò trung gian,
 * gọi MainClient.main() để JavaFX tự phát hiện và khởi động đúng cách.
 */
public class Launcher {
    public static void main(String[] args) {
        MainClient.main(args);
    }
}
