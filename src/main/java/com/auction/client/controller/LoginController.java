package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.model.account.User;
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
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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

        AuthPayload payload = new AuthPayload(username, password);
        String jsonPayload = gson.toJson(payload);
        CompletableFuture.supplyAsync(() -> {
            return network.sendRequest(new RequestMessage(ActionType.LOGIN, jsonPayload));
        }).thenAccept(res -> {
            if (res == null) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText("Không thể kết nối server");
                });
                return;
            }

            if ("SUCCESS".equals(res.getStatus())) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.GREEN);
                    lblMessage.setText("Đăng nhập thành công");
                });

                User loggedInUser = gson.fromJson(res.getData(), User.class);
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(e -> handleSwitchToHomePage(loggedInUser));
                pause.play();

            } else {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText(res.getMessage());
                });
            }
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
            String username = loggedInUser.getUsername();
            String role = loggedInUser.getRole();

            // Lấy đường dẫn file FXML (Đảm bảo đường dẫn này đúng với thư mục resources)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
            Parent root = loader.load();

            //Lay Controller
            HomePageController homePageController = loader.getController();
            homePageController.initData(username, role);

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