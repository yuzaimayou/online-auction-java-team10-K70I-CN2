package com.auction.client.controller.setting;

import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePageController {
    // user info
    @FXML
    private TextField emailField;
    @FXML
    private TextField userNameField;
    @FXML
    private TextField roleUserField;

    // Sidebar
    @FXML
    private ToggleButton profileInfoBtn;
    @FXML
    private ToggleButton myAuctionsBtn;
    @FXML
    private ToggleButton historyBidBtn;

    //w allet
    @FXML
    private Label availableBalanceLabel;
    @FXML
    private Label frozenBalanceLabel;
    @FXML
    private Label totalBalanceLabel;

    @FXML
    public void initialize() {
        if (profileInfoBtn != null) {
            profileInfoBtn.setSelected(true);
        }
        displayUserData();
        displayWalletData();
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
                if ("admin".equalsIgnoreCase(currentUser.getUsername())) {
                    roleUserField.setText("Admin Account");
                } else {
                    roleUserField.setText("User Account");
                }
            }
            lockFields(emailField, userNameField, roleUserField);
        }
    }

    // hiển thị số dư ví
    private void displayWalletData() {
        // Giả lập dữ liệu ví
        double available = 700.00;
        availableBalanceLabel.setText(String.format("$%,.2f", available));
        frozenBalanceLabel.setText("$300.00");
        totalBalanceLabel.setText("$1,000.00");
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
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/MyAuctionsPage.fxml");
        }
    }
    @FXML
    private void handleHistoryBid(ActionEvent event) {
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/HistoryBidPage.fxml");
        }
    }
    @FXML
    private void handleDepositAction(ActionEvent event) {
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/DepositPage.fxml");
        }
    }
}