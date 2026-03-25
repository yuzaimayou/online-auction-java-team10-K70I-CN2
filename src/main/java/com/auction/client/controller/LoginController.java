package com.auction.client.controller;

import com.auction.server.service.AuthService;
import com.auction.shared.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class LoginController {

    // Khai báo các thành phần giao diện có fx:id tương ứng trong file FXML
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;

    // Khởi tạo AuthService để dùng chung
    private AuthService authService = new AuthService();

    // Hàm này chạy khi bấm nút "Đăng nhập"
    @FXML
    protected void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // Kiểm tra xem người dùng có nhập rỗng không
        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        // Gọi logic kiểm tra từ Server (AuthService)
        User loggedInUser = authService.login(username, password);

        if (loggedInUser != null) {
            lblMessage.setTextFill(Color.GREEN);
            lblMessage.setText("Đăng nhập thành công! Vai trò: " + loggedInUser.getRole());
            // TODO: Chuyển sang màn hình chính của ứng dụng ở đây (sprint sau)
        } else {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    // Hàm này chạy khi bấm nút "Chưa có tài khoản? Đăng ký"
    @FXML
    protected void handleSwitchToRegister(ActionEvent event) {
        lblMessage.setTextFill(Color.BLUE);
        lblMessage.setText("Chức năng chuyển sang form Đăng ký sẽ code sau!");
    }
}