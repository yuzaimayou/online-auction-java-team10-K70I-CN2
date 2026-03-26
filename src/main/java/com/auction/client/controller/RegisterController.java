package com.auction.client.controller;

import com.auction.server.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    // Khai báo các thành phần giao diện có fx:id tương ứng trong file FXML
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblMessage;

    private AuthService authService = new AuthService();

    // Hàm này chạy khi bấm nút "Đăng ký"
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        // Hàm này chạy khi bấm nút "Đăng nhập"
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra mật khẩu nhập lại có khớp
        if (!password.equals(confirm)) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Mật khẩu không khớp!");
            return;
        }

        boolean success = authService.register(username, password, "USER");
        if (success) {
            lblMessage.setTextFill(Color.GREEN);
            lblMessage.setText("Đăng ký thành công!");
        } else {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Tên đăng nhập đã tồn tại!");
        }
    }

    // Chuyển sang giao diện Login
    @FXML
    public void handleSwitchToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com.auction.client/login.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}