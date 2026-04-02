package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.constant.ActionType;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.AuthPayload;
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
import javafx.util.Duration;

import java.io.IOException;

public class HomePageController {
    @FXML
    public void initialize() {
        System.out.println("Đã vào trang chủ!");
    }
    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/AuctionFormPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu Trực tuyến - Tạo phiên đấu giá");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file AuctionFormPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}

