package com.auction.client.controller;

import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class NavBarController {
    @FXML
    private Label lblUserName;
    @FXML
    private TextField searchField;
    @FXML
    public void initialize() {
        refreshUserInfo();

        searchField.textProperty().bindBidirectional(SearchStoreController.searchQueryProperty());
    }

    public void refreshUserInfo() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && lblUserName != null) {
            lblUserName.setText(currentUser.getUsername());
        } else {
            lblUserName.setText("Guest");
        }
    }
    public void handleSwitchToHome() {

        searchField.textProperty().unbindBidirectional(SearchStoreController.searchQueryProperty());

        SearchStoreController.reset();
        NavigationUtil.handleSwitchToHomePage(lblUserName);
    }

    public void handleSwitchToSetting() {
        searchField.textProperty().unbindBidirectional(SearchStoreController.searchQueryProperty());

        NavigationUtil.handleSwitchToSetting(lblUserName);
    }
}
