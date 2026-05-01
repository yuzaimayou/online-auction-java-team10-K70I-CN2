package com.auction.client.controller;

import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class NavBarController {
    private User user = UserSession.getInstance().getLoggedInUser();
    @FXML
    private Label userName;

    public void initialize() {
        userName.setText(user.getUsername());

    }

    public void handleSwitchToHome() {
        NavigationUtil.handleSwitchToHomePage(userName);
    }

    public void handleSwitchToSetting() {
        NavigationUtil.handleSwitchToSetting(userName);
    }
}
