package com.auction.client.controller.user;

import com.auction.client.service.AuthService;
import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
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
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMessage;

    private AuthService authService = AuthService.getInstance();
    private Gson gson = new Gson();

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
                        User loggedInUser = gson.fromJson(responseMessage.getData(), User.class);
                        UserSession.getInstance().setLoggedInUser(loggedInUser);
                        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(2));

                        if (loggedInUser.isVerify() == false) {
                            javafx.application.Platform.runLater(() -> {
                                lblMessage.setTextFill(Color.RED);
                                lblMessage.setText("Account unverified. Please check your email for the OTP.");
                            });
                            pause.setOnFinished(e -> NavigationUtil.switchToOtpScreen(event, loggedInUser.getEmail()));
                            pause.play();
                            return;
                        }
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.GREEN);
                            lblMessage.setText(responseMessage.getMessage());
                        });
                        pause.setOnFinished(e -> NavigationUtil.handleSwitchToHomePage(lblMessage));
                        pause.play();
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.RED);
                            lblMessage.setText(responseMessage.getMessage());
                        });
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        lblMessage.setTextFill(Color.RED);
                        lblMessage.setText("Failed to connect to server");
                    });
                    return null;
                });
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