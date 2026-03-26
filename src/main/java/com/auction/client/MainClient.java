package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainClient extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Tải file giao diện FXML.
            // Lưu ý: Đường dẫn này trỏ tới thư mục src/main/resources/com/auction/client/Login.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/AuthPage.fxml"));
            Parent root = loader.load();

            // Khởi tạo Scene (khung cảnh)
            Scene scene = new Scene(root, 1440, 1024);

            // Cấu hình Stage (Cửa sổ ứng dụng)
            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            primaryStage.setScene(scene);

            // Tạm thời khóa thay đổi kích thước để form không bị xô lệch
            primaryStage.setResizable(false);

            // Hiển thị cửa sổ
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải file FXML. Hãy kiểm tra lại đường dẫn trong resources!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Lệnh bắt buộc để khởi chạy vòng đời của một ứng dụng JavaFX
        launch(args);
    }
}