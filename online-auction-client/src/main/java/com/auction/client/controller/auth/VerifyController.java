package com.auction.client.controller.auth;

import com.auction.client.service.AuthService;
import com.auction.client.util.NavigationUtil;
import com.auction.client.service.UserSession;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public class VerifyController {
    private String email;
    private AuthService authService = AuthService.getInstance();

    @FXML
    private Label lblMessage;
    @FXML
    private TextField txtCode1;
    @FXML
    private TextField txtCode2;
    @FXML
    private TextField txtCode3;
    @FXML
    private TextField txtCode4;
    @FXML
    private TextField txtCode5;
    @FXML
    private TextField txtCode6;


    @FXML
    public void initialize() {
        addAutoJump(txtCode1, txtCode2, null);
        addAutoJump(txtCode2, txtCode3, txtCode1);
        addAutoJump(txtCode3, txtCode4, txtCode2);
        addAutoJump(txtCode4, txtCode5, txtCode3);
        addAutoJump(txtCode5, txtCode6, txtCode4);
        addAutoJump(txtCode6, null, txtCode5);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private void addAutoJump(TextField current, TextField next, TextField previous) {
        current.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                current.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            if (newVal.length() > 1) {
                current.setText(newVal.substring(0, 1));
                return;
            }
            if (newVal.length() == 1 && next != null) {
                next.requestFocus();
            }
        });

        current.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.BACK_SPACE && current.getText().isEmpty() && previous != null) {
                previous.requestFocus();
            }
        });
    }

    @FXML
    public void handleVerify(ActionEvent event) {
        String otp = txtCode1.getText() + txtCode2.getText() + txtCode3.getText() + txtCode4.getText() + txtCode5.getText() + txtCode6.getText();
        authService.verify(email, otp)
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(0.5));
                        if (UserSession.getInstance().getLoggedInUser() != null) {
                            pause.setOnFinished(e -> NavigationUtil.handleSwitchToHomePage(lblMessage));
                        } else {
                            pause.setOnFinished(e -> handleSwitchToLogin(event));
                        }
                        pause.play();
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            // Hiển thị lỗi nếu có
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
    public void handleResend(ActionEvent event) {
        authService.sendOtp(email)
                .thenAccept(responseMessage -> {
                    javafx.application.Platform.runLater(() -> {
                        if ("success".equals(responseMessage.getStatus())) {
                            lblMessage.setTextFill(Color.GREEN);
                            lblMessage.setText("OTP has been resent to your email");
                            txtCode1.clear();
                            txtCode2.clear();
                            txtCode3.clear();
                            txtCode4.clear();
                            txtCode5.clear();
                            txtCode6.clear();
                        } else {
                            lblMessage.setTextFill(Color.RED);
                            lblMessage.setText(responseMessage.getMessage());
                        }
                    });
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
    public void handleSwitchToLogin(
            ActionEvent event
    ) {NavigationUtil.switchToLogin(event);
    }
}