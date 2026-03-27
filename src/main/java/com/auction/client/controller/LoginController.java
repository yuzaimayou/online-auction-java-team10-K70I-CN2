package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.constant.ActionType;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.User;
import com.google.gson.Gson;
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

public class LoginController {

    // Khai báo các thành phần giao diện có fx:id tương ứng trong file FXML
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;


    private NetworkService network= NetworkService.getInstance();
    private Gson gson=new Gson();
    // Hàm này chạy khi bấm nút "Đăng nhập"
    @FXML
    protected void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // Kiểm tra xem người dùng có nhập rỗng không
        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        // Gui message den server
        String payload=username+","+password;
        ResponseMessage res= network.sendRequest(new RequestMessage(ActionType.LOGIN,payload));
        if (res==null){
            System.out.println("Unable to connect to the server");
        }
        System.out.println(res.getStatus());
        if ("SUCCESS".equals(res.getStatus())){
            User loggedInUser=gson.fromJson(res.getData(),User.class);
            lblMessage.setTextFill(Color.GREEN);
            lblMessage.setText("Đăng nhập thành công! Vai trò: " + loggedInUser.getRole());
        }else{
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    // Hàm này chạy khi bấm nút "Chưa có tài khoản? Đăng ký"
    @FXML
    protected void handleSwitchToRegister(ActionEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/com.auction.client/fxml/Register.fxml"));
            Node sourceNode=(Node) event.getSource();
            StackPane dynamicContentArea=(StackPane) sourceNode.getScene().lookup("#dynamicContentArea");
            if (dynamicContentArea!=null){
                dynamicContentArea.getChildren().clear();

                dynamicContentArea.getChildren().add(registerRoot);
            }else{
                System.err.println("Error: Không tìm thấy StackPane có ID là dynamicContentArea");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}