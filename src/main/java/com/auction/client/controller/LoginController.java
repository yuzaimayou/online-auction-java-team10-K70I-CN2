package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.server.controller.ClientHandler;
import com.auction.server.service.AuthService;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class LoginController {

    // Khai báo các thành phần giao diện có fx:id tương ứng trong file FXML
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;

    // Khởi tạo AuthService để dùng chung
    private AuthService authService = new AuthService();
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

        // Gọi logic kiểm tra từ Server (AuthService)

        NetworkService network=new NetworkService();
        ResponseMessage res=network.sendLoginMessage(username,password);
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
        lblMessage.setTextFill(Color.BLUE);
        lblMessage.setText("Chức năng chuyển sang form Đăng ký sẽ code sau!");
    }
}