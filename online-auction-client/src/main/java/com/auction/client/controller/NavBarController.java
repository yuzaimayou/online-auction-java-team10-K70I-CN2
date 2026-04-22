package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class NavBarController {
    private User user = UserSession.getInstance().getLoggedInUser();
    @FXML
    private Label userName;

    public void initialize() {
        userName.setText(user.getUsername());

    }

    public void handleSwitchToHome() {
        try {
            NetworkService.getInstance().leaveRoom();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
            Parent root = loader.load();
            Scene currentScene = userName.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - HomePage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSwitchToProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ProfilePage.fxml"));
            Parent root = loader.load();

            Scene currentScene = userName.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Profile Page");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
