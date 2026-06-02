package com.auction.client.controller.auth;

import com.auction.client.service.AuthService;
import com.auction.client.validation.AuthValidation;
import com.auction.client.util.NavigationUtil;
import com.auction.client.service.UserSession;
import com.auction.shared.model.account.User;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class LoginController {

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;
    private final AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
    }

    @FXML
    protected void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // 1. Kiểm tra validation qua class AuthValidation
        String validationError = AuthValidation.validateLogin(username, password);
        if (validationError != null) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText(validationError);
            return;
        }

        // 2. Gọi Service xử lý đăng nhập bất đồng bộ
        authService.login(username.trim(), password)
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        // Phân tích dữ liệu User từ phản hồi của Server
                        User loggedInUser = authService.parseUser(responseMessage.getData());

                        UserSession.getInstance().cleanUserSession();
                        UserSession.getInstance().setLoggedInUser(loggedInUser);

                        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(2));

                        // Trường hợp: Tài khoản chưa kích hoạt OTP
                        if (!loggedInUser.isVerify()) {
                            Platform.runLater(() -> {
                                lblMessage.setTextFill(Color.RED);
                                lblMessage.setText("Account unverified. Redirecting to Verify...");
                            });
                            pause.setOnFinished(e ->
                                    NavigationUtil.switchToOtpScreen(event, loggedInUser.getEmail())
                            );
                            pause.play();
                            return;
                        }

                        // Trường hợp: Đăng nhập thành công hoàn toàn
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.GREEN);
                            lblMessage.setText("Login successful! Welcome " + loggedInUser.getUsername());
                        });
                        pause.setOnFinished(e -> NavigationUtil.handleSwitchToHomePage(lblMessage));
                        pause.play();

                    } else {
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.RED);
                            lblMessage.setText(responseMessage.getMessage());
                        });
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        lblMessage.setTextFill(Color.RED);
                        lblMessage.setText("Failed to connect to server");
                    });
                    return null;
                });
    }

    @FXML
    protected void handleSwitchToRegister(ActionEvent event) {
        NavigationUtil.switchToRegister(event);
    }
}