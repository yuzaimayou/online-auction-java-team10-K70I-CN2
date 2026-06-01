package com.auction.client.controller.auth;

import com.auction.client.service.AuthService;
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

    private AuthService authService = AuthService.getInstance();

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
        authService.login(username, password)
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        User loggedInUser = authService.parseUser(responseMessage.getData());

                        UserSession.getInstance().cleanUserSession();
                        UserSession.getInstance().setLoggedInUser(loggedInUser);
                        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(2));

                        if (!loggedInUser.isVerify()) {
                            Platform.runLater(() -> {
                                lblMessage.setTextFill(Color.RED);
                                lblMessage.setText("Account unverified. Redirecting to Verify...");
                            });
                            pause.setOnFinished(
                                    e -> NavigationUtil.switchToOtpScreen(
                                            event,
                                            loggedInUser.getEmail()
                                    )
                            );
                            pause.play();
                            return;
                        }

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