package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainClient extends Application {

    // Giữ nguyên hàm loadInterFonts của bạn
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
            if (is != null)
                javafx.scene.text.Font.loadFont(is, 12);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            loadInterFonts();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/authenticator/Login.fxml"));
            Parent root = loader.load();

            // 1. Lấy thông số màn hình của người dùng
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            // 2. Thiết lập kích thước mở đầu nhỏ gọn (ví dụ: 60% màn hình)
            // Thay vì Scene(root, 600, 300) quá nhỏ so với Form
            double initialWidth = screenBounds.getWidth() * 0.8;
            double initialHeight = screenBounds.getHeight() * 0.75;
            Scene scene = new Scene(root, initialWidth, initialHeight);

            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            primaryStage.setScene(scene);

            primaryStage.setResizable(true);

            // 3. QUAN TRỌNG: Hạ thấp MinWidth/MinHeight để nó không tự động bung to
            // Nếu để 1000x700 thì máy nào màn hình bé nó sẽ chiếm gần hết màn hình
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(740);

            // Đảm bảo không bật Maximized
            primaryStage.setMaximized(false);

            // Căn giữa cửa sổ khi hiện ra
            primaryStage.centerOnScreen();

            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}