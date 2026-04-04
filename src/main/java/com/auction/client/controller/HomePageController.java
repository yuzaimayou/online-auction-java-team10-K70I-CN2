package com.auction.client.controller;

import com.auction.client.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController {
    @FXML
    public Label userName;

    @FXML
    public void initialize() {

        System.out.println("Đã vào trang chủ!");
        userName.setText(UserSession.getInstance().getLoggedInUser().getUsername());
    }


    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/AuctionFormPage.fxml"));
            Parent root = loader.load();

            AuctionFormController auctionFormController = loader.getController();

            Scene currentScene = userName.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Add Product");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file AuctionFormPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }

    @FXML
    public void handleSwitchToProductPage(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ProductPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu Trực tuyến ");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file ProductPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}

