package com.auction.client.controller.setting;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePageController {

    @FXML
    private ToggleButton profileInfoBtn;

    @FXML
    private ToggleButton myAuctionsBtn;

    @FXML
    private ToggleButton historyBidBtn;

    @FXML
    public void initialize() {
        if (profileInfoBtn != null) {
            profileInfoBtn.setSelected(true);
        }
    }

    @FXML
    private void handleProfileInfo(ActionEvent event) {
        profileInfoBtn.setSelected(true);
    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        switchScene("/com.auction.client/fxml/setting/MyAuctionsPage.fxml");
    }

    @FXML
    private void handleHistoryBid(ActionEvent event) {
        switchScene("/com.auction.client/fxml/HistoryBidPage.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) profileInfoBtn.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}