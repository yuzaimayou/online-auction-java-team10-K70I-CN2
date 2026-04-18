package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.message.ResponseMessage;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.auction.client.util.AppConfig.getHttpUrl;

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

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(String.format("%s/api/register", getHttpUrl())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    ResponseMessage res = gson.fromJson(responseBody, ResponseMessage.class);
                    if ("success".equalsIgnoreCase(res.getStatus())) {
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.GREEN);
                            lblMessage.setText(res.getMessage());
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