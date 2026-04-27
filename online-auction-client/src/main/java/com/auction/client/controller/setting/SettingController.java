package com.auction.client.controller.setting;

import com.auction.client.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingController {
    @FXML
    private VBox dynamicContent;

    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        handleSwitchToAuthenPage(event);

    }

    @FXML
    private void handleProfileInfo(ActionEvent event) {
        dynamicContent.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/setting/ProfilePage.fxml"));
            Parent profilePage = loader.load();
            dynamicContent.getChildren().add(profilePage);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file ProfilePage.fxml! Kiểm tra lại đường dẫn.");
        }


    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        dynamicContent.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/setting/MyAuctionsPage.fxml"));
            Parent myAuctionsPage = loader.load();
            dynamicContent.getChildren().add(myAuctionsPage);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file MyAuctionsPage.fxml! Kiểm tra lại đường dẫn.");
        }

    }

    @FXML
    private void handleHistoryBid(ActionEvent event) {

    }

    @FXML
    public void handleSwitchToAuthenPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/authenticator/AuthPage.fxml")
            );
            Parent root = loader.load();

            Node sourceNode = (Node) event.getSource();
            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Authentication");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file AuthPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}
