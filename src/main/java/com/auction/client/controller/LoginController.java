package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.constant.ActionType;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.AuthPayload;
import com.auction.shared.model.account.User;
import com.google.gson.Gson;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
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

        AuthPayload payload=new AuthPayload(username,password);
        String jsonPayload=gson.toJson(payload);

        ResponseMessage res =
                network.sendRequest(new RequestMessage(ActionType.LOGIN, jsonPayload));

        if (res == null) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Không thể kết nối server");
            return;
        }

        if ("SUCCESS".equals(res.getStatus())) {

            User loggedInUser = gson.fromJson(res.getData(), User.class);
            System.out.println("Đăng nhập thành công");
            handleSwitchToHomePage(event,loggedInUser);

        } else {

            lblMessage.setTextFill(Color.RED);
            lblMessage.setText(res.getMessage());

        }
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
    public void handleSwitchToHomePage(ActionEvent event,User loggedInUser) {
            try {
                String username=loggedInUser.getUsername();
                String role=loggedInUser.getRole();
                // Lấy đường dẫn file FXML (Đảm bảo đường dẫn này đúng với thư mục resources)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
                Parent root = loader.load();

                //Lay Controller
                HomePageController homePageController=loader.getController();
                homePageController.initData(username,role);

                //Lay scene hien tai
                Node sourceNode = (Node) event.getSource();
                Scene scene = sourceNode.getScene();

                //Lay root cua scene
                StackPane mainRoot=(StackPane) scene.getRoot();

                Group scaleGroup= (Group) mainRoot.getChildren().get(0);
                scaleGroup.getChildren().clear();
                scaleGroup.getChildren().add(root);

                Stage stage=(Stage) scene.getWindow();
                stage.setTitle("Hệ thống Đấu giá Trực tuyến - Home");
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Không tìm thấy file HomePage.fxml! Kiểm tra lại đường dẫn.");
            }
        }
}