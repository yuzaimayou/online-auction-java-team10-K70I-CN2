package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.IOException;

public class MyAuctionsPageController {

    @FXML
    private ToggleButton profileInfoBtn;

    @FXML
    private ToggleButton myAuctionsBtn;

    @FXML
    private ToggleButton historyBidBtn;

    @FXML
    public void initialize() {
        if (myAuctionsBtn != null) {
            myAuctionsBtn.setSelected(true);
        }
    }

    @FXML
    private void handleProfileInfo(ActionEvent event) {
        switchScene("/com.auction.client/fxml/ProfilePage.fxml");
    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        myAuctionsBtn.setSelected(true);
    }

    @FXML
    private void handleHistoryBid(ActionEvent event) {
        switchScene("/com.auction.client/fxml/HistoryBidPage.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) myAuctionsBtn.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void handleSwitchToProductEdit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/ProductEdit.fxml")
            );
            Parent root = loader.load();

            Node sourceNode = (Node) event.getSource();
            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Product Edit");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file ProductEdit.fxml! Kiểm tra lại đường dẫn.");
        }
    }

}