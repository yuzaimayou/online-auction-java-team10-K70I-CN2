package com.auction.client.controller.setting;

import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePageController {
    @FXML private TextField emailField;
    @FXML private TextField userNameField; // Tạm thời dùng hiển thị Username vào đây
    @FXML private TextField roleUserField;

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

        displayUserData();
    }

    private void displayUserData() {
        User currentUser = UserSession.getInstance().getLoggedInUser();

        if (currentUser != null) {
            if (emailField != null) {
                emailField.setText(currentUser.getEmail());
            }
            if (userNameField != null) {
                userNameField.setText(currentUser.getUsername());
            }
            if (roleUserField != null) {
                roleUserField.setText("User Account");
            }

            lockFields(emailField, userNameField, roleUserField);
        }
    }

    private void lockFields(TextField... fields) {
        for (TextField field : fields) {
            if (field != null) {
                field.setEditable(false);
                field.setFocusTraversable(false);
                field.setMouseTransparent(true);
            }
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

            if (profileInfoBtn != null && profileInfoBtn.getScene() != null) {
                Stage stage = (Stage) profileInfoBtn.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}