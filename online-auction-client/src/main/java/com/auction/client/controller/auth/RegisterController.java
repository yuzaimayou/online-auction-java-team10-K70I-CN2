package com.auction.client.controller.auth;

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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

import static com.auction.client.service.ToastService.showError;

public class RegisterController {
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

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
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();
        String email = txtEmail.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || email.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!email.matches(EMAIL_REGEX)) {
            showError("Invalid email address.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        authService.register(username, password, email)
                .thenAccept(res -> {
                    if ("success".equalsIgnoreCase(res.getStatus())) {
                        Platform.runLater(() -> {
                            showSuccess(res.getMessage());
                            PauseTransition pause = new PauseTransition(Duration.seconds(2));
                            pause.setOnFinished(e -> switchToOtpScreen(event, email));
                            pause.play();
                        });
                    } else {
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
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com.auction.client/fxml/authenticator/Login.fxml"));

            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            stage.getScene().setRoot(loginRoot);

        } catch (IOException e) {
            System.err.println("Không tìm thấy file Login.fxml!");
            e.printStackTrace();
        }
    }

    @FXML
    protected void switchToOtpScreen(ActionEvent event, String registeredEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/authenticator/Verify.fxml"));
            Parent verifyRoot = loader.load();
            VerifyController verifyController = loader.getController();
            verifyController.setEmail(registeredEmail);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(verifyRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
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