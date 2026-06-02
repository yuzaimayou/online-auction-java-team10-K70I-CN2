package com.auction.client.controller.auth;

import com.auction.client.service.AuthService;
import com.auction.client.validation.AuthValidation;
import com.auction.client.util.NavigationUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.util.Duration;


public class RegisterController {

    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private PasswordField txtConfirmPassword;
    @FXML
    private Label lblMessage;

    private AuthService authService = AuthService.getInstance();


    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        // 1. Kiểm tra validation
        String validationError = AuthValidation.validateRegister(username, email, password, confirm);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        // 2. Gọi Service xử lý đăng ký tài khoản
        authService.register(username.trim(), password, email.trim())
                .thenAccept(res -> {
                    if ("success".equalsIgnoreCase(res.getStatus())) {
                        Platform.runLater(() -> {
                            showSuccess(res.getMessage());
                            PauseTransition pause = new PauseTransition(Duration.seconds(2));
                            pause.setOnFinished(e -> NavigationUtil.switchToOtpScreen(event, email.trim()));
                            pause.play();
                        });
                    } else {
                        // Trường hợp server báo lỗi (Trùng tài khoản, trùng email...)
                        Platform.runLater(() -> showError(res.getMessage()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Unable to connect to server"));
                    return null;
                });
    }

    @FXML
    protected void handleSwitchToLogin(ActionEvent event) {
        NavigationUtil.switchToLogin(event);
    }

    private void showError(String message) {
        lblMessage.setTextFill(Color.RED);
        lblMessage.setText(message);
    }

    private void showSuccess(String message) {
        lblMessage.setTextFill(Color.GREEN);
        lblMessage.setText(message);
    }
}