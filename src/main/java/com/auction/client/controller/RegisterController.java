package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.model.enums.ActionType;
import com.auction.shared.model.payloads.AuthPayload;
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
import java.util.concurrent.CompletableFuture;

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

    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new Gson();

    @FXML
    public void handleRegister(ActionEvent event) {

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        if (!password.equals(confirm)) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Mật khẩu không khớp");
            return;
        }

        AuthPayload payload = new AuthPayload(username, password);
        String jsonPayload = gson.toJson(payload);
        CompletableFuture.supplyAsync(() -> {
            return network.sendRequest(new RequestMessage(ActionType.REGISTER, jsonPayload));
        }).thenAccept(res -> {
            if (res == null) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText("Không thể kết nối server");
                });

                return;
            }

            if ("SUCCESS".equals(res.getStatus())) {
                System.out.println(res.getMessage());
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.GREEN);
                    lblMessage.setText("Đăng ký thành công");
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(e -> handleSwitchToLogin(event));
                    pause.play();
                });


            } else {
                Platform.runLater(() -> {

                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText(res.getMessage());
                });


            }
        });


    }

    @FXML
    public void handleSwitchToLogin(ActionEvent event) {

        try {

            Parent loginRoot = FXMLLoader.load(
                    getClass().getResource("/com.auction.client/fxml/Login.fxml"));

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
}