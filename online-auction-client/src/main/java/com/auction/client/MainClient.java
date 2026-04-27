package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.IOException;

public class MainClient extends Application {
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
            if (is == null) {
                System.out.println("Not found: " + font);
                continue;
            }
            var f = javafx.scene.text.Font.loadFont(is, 12);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load fonts
            loadInterFonts();
            // Base size
            double baseW = 1440;
            double baseH = 1024;
            // Tải file giao diện FXML.
            // Lưu ý: Đường dẫn này trỏ tới thư mục
            // src/main/resources/com/auction/client/Login.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/authenticator/AuthPage.fxml"));
            Parent content = loader.load();
            // Group scale UI
            Group scaleGroup = new Group(content);

            StackPane root = new StackPane(scaleGroup);
            root.setStyle("-fx-background-color: #ffffff;");

            // Khởi tạo Scene (khung cảnh)
            Scene scene = new Scene(root, baseW, baseH);

            // Scale demo(dang loi), dang khoa kich thuoc
            Scale scale = new Scale(1, 1, 0, 0);
            scaleGroup.getTransforms().add(scale);
            Runnable updateScale = () -> {
                double scaleX = scene.getWidth() / baseW;
                double scaleY = scene.getHeight() / baseH;
                double s = Math.min(scaleX, scaleY);
                scale.setX(s);
                scale.setY(s);
            };
            scene.widthProperty().addListener((obs, oldV, newV) -> updateScale.run());
            scene.heightProperty().addListener((obs, oldV, newV) -> updateScale.run());

            updateScale.run();

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