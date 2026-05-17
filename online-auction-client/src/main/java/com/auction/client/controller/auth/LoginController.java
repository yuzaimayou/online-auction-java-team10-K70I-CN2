package com.auction.client.controller.auth;

import com.auction.client.service.AuthService;
import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    private static final String FXML_REGISTER = "/com.auction.client/fxml/authenticator/Register.fxml";
    private static final String FXML_VERIFY = "/com.auction.client/fxml/authenticator/Verify.fxml";

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;

    private AuthService authService = AuthService.getInstance();
    private Gson gson = GsonUtil.getInstance();

    @FXML
    public void initialize() {
    }

    @FXML
    protected void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Username and password are required!");
            return;
        }

        System.out.println("Sending login request for: " + username);

        authService.login(username, password)
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        // --- ĐOẠN CODE ĐÃ ĐƯỢC SỬA LẠI ĐỂ FIX LỖI ROLE ---
                        // 1. Đọc Data thành JsonObject để kiểm tra role trước
                        JsonElement jsonElement = gson.toJsonTree(responseMessage.getData());
                        JsonObject userData = jsonElement.getAsJsonObject();
                        System.out.println("Received auth data: " + userData.toString());
                        String role = "";

                        // Kiểm tra xem JSON có trường "role" không
                        if (userData.has("role") && !userData.get("role").isJsonNull()) {
                            role = userData.get("role").getAsString();
                        }

                        // 2. Khởi tạo User hoặc Admin tùy thuộc vào Role
                        User loggedInUser;
                        if ("Admin".equalsIgnoreCase(role)) {
                            loggedInUser = gson.fromJson(jsonElement, Admin.class);
                            System.out.println("Đã đăng nhập với quyền ADMIN!");
                        } else {
                            loggedInUser = gson.fromJson(jsonElement, User.class);
                            System.out.println("Đã đăng nhập với quyền USER!");
                        }

                        UserSession.getInstance().cleanUserSession();
                        UserSession.getInstance().setLoggedInUser(loggedInUser);
                        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(2));

                        if (!loggedInUser.isVerify()) {
                            Platform.runLater(() -> {
                                lblMessage.setTextFill(Color.RED);
                                lblMessage.setText("Account unverified. Redirecting to Verify...");
                            });
                            pause.setOnFinished(e -> redirectToVerify(event, loggedInUser.getEmail()));
                            pause.play();
                            return;
                        }

                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.GREEN);
                            lblMessage.setText("Login successful! Welcome " + loggedInUser.getUsername());
                        });

                        // Chuyển về trang chủ
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

    private void redirectToVerify(ActionEvent event, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/authenticator/Verify.fxml"));
            Parent verifyRoot = loader.load();
            VerifyController verifyController = loader.getController();
            verifyController.setEmail(email);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(verifyRoot);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    protected void handleSwitchToRegister(ActionEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/com.auction.client/fxml/authenticator/Register.fxml"));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(registerRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}