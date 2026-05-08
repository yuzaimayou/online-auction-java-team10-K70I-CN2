package com.auction.client.controller;

import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class NavBarController {
    private User user = UserSession.getInstance().getLoggedInUser();
    @FXML
    private Label userName;
    @FXML
    private TextField searchField;

    public void initialize() {
        userName.setText(user.getUsername());

        searchField.textProperty().bindBidirectional(SearchStoreController.searchQueryProperty());
    }

    public void handleSwitchToHome() {

        searchField.textProperty().unbindBidirectional(SearchStoreController.searchQueryProperty());

        SearchStoreController.reset();
        NavigationUtil.handleSwitchToHomePage(userName);
    }

    public void handleSwitchToSetting() {
        searchField.textProperty().unbindBidirectional(SearchStoreController.searchQueryProperty());

        NavigationUtil.handleSwitchToSetting(userName);
    }
}
