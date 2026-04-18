package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.AuthPayload;
import com.google.gson.Gson;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMessage;

    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new Gson();

    @FXML
    protected void handleLogin(ActionEvent event) {

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }
        //create payload
        AuthPayload payload = new AuthPayload(username, password);
        String jsonPayload = gson.toJson(payload);
        String httpUrl = String.format("%s/api/login", AppConfig.getHttpUrl());
        System.out.println("Sending login request to: " + httpUrl);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(String.format("%s/api/login", AppConfig.getHttpUrl())))
                .header("Content-Type", "application/json; utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    ResponseMessage responseMessage = gson.fromJson(responseBody, ResponseMessage.class);

                    if ("success".equals(responseMessage.getStatus())) {
                        User loggedInUser = gson.fromJson(responseMessage.getData(), User.class);
                        UserSession.getInstance().setLoggedInUser(loggedInUser);

                        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(0.5));
                        pause.setOnFinished(e -> handleSwitchToHomePage(loggedInUser));
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

            Parent registerRoot =
                    FXMLLoader.load(getClass().getResource("/com.auction.client/fxml/Register.fxml"));

            Node sourceNode = (Node) event.getSource();

            StackPane dynamicContentArea =
                    (StackPane) sourceNode.getScene().lookup("#dynamicContentArea");

            if (dynamicContentArea != null) {

                dynamicContentArea.getChildren().clear();
                dynamicContentArea.getChildren().add(registerRoot);

            } else {

                System.err.println(
                        "Error: Không tìm thấy StackPane có ID là dynamicContentArea"
                );
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSwitchToHomePage(User loggedInUser) {
        try {
            System.out.println("Da chuyen sang trang chu");
            // Lấy đường dẫn file FXML (Đảm bảo đường dẫn này đúng với thư mục resources)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
            Parent root = loader.load();

            //Lay Controller
            HomePageController homePageController = loader.getController();


            //Lay scene hien tai
            Scene currentScene = lblMessage.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Hệ thống Đấu giá Trực tuyến - Home");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file HomePage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}