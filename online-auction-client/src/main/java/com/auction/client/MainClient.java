package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainClient extends Application {

    // Hàm load fonts Inter
    private static void loadInterFonts() {
        String[] fontPaths = {
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-Italic.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-Light.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-LightItalic.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-Medium.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-MediumItalic.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-Regular.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-SemiBold.ttf",
                "/com.auction.client/assets/fonts/Inter/Inter_18pt-SemiBoldItalic.ttf"
        };
        for (String font : fontPaths) {
            var is = MainClient.class.getResourceAsStream(font);
            if (is != null) javafx.scene.text.Font.loadFont(is, 12);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load fonts
            loadInterFonts();

            // TẢI TRỰC TIẾP LOGIN.FXML (Bây giờ đã bao gồm cả ảnh bên trái)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/authenticator/Login.fxml"));
            Parent root = loader.load();

            // Khởi tạo Scene với kích thước mặc định nhưng cho phép thay đổi
            Scene scene = new Scene(root, 1440, 900);

            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            primaryStage.setScene(scene);

            // BẬT RESIZABLE ĐỂ GIAO DIỆN LINH HOẠT
            primaryStage.setResizable(true);

            // Đặt kích thước tối thiểu để không bị vỡ layout
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải file FXML!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}