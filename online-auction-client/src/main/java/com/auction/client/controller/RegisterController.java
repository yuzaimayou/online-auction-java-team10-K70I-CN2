package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.google.gson.Gson;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;

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
    private Gson gson = new Gson();

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();
        String email = txtEmail.getText().trim();
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || email.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (!email.matches(regex)) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Email không hợp lệ");
            return;
        }

        if (!password.equals(confirm)) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Mật khẩu không khớp");
            return;
        }

        authService.register(username, password, email)
                .thenAccept(res -> {
                    if ("success".equalsIgnoreCase(res.getStatus())) {
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.GREEN);
                            lblMessage.setText(res.getMessage());
                            PauseTransition pause = new PauseTransition(Duration.seconds(2));
                            pause.setOnFinished(e -> switchToOtpScreen(event, email));
                            pause.play();
                        });
                    } else {
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.RED);
                            lblMessage.setText(res.getMessage());
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        lblMessage.setTextFill(Color.RED);
                        lblMessage.setText("Unable to connect to server");
                    });
                    return null;
                });
    }

    @FXML
    public void handleSwitchToLogin(ActionEvent event) {

        try {

            Parent loginRoot = FXMLLoader.load(
                    getClass().getResource("/com.auction.client/fxml/authenticator/Login.fxml"));

            Node sourceNode = (Node) event.getSource();

            StackPane dynamicContentArea =
                    (StackPane) sourceNode.getScene().lookup("#dynamicContentArea");

            if (dynamicContentArea != null) {

                dynamicContentArea.getChildren().clear();
                dynamicContentArea.getChildren().add(loginRoot);

            } else {

                System.err.println("Không tìm thấy StackPane dynamicContentArea");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void switchToOtpScreen(ActionEvent event, String registeredEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/authenticator/OtpVerification.fxml"));
            Node sourceNode = (Node) event.getSource();

            StackPane dynamicContentArea = (StackPane) sourceNode.getScene().lookup("#dynamicContentArea");

            if (dynamicContentArea != null) {

                dynamicContentArea.getChildren().clear();
                dynamicContentArea.getChildren().add(loader.load());

            } else {
                System.err.println("Không tìm thấy StackPane dynamicContentArea");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}