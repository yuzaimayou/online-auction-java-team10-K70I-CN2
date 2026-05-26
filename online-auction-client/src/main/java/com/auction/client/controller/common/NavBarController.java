package com.auction.client.controller.common;

import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class NavBarController {
    @FXML
    private Label lblUserName;
    @FXML
    private Label lblBalance;
    @FXML
    private TextField searchField;

    @FXML
    public void initialize() {
        refreshUserInfo();
        UserSession.getInstance().addListener(this::refreshUserInfo);
        searchField.textProperty().bindBidirectional(SearchStoreController.searchQueryProperty());
    }

    public void refreshUserInfo() {
        User currentUser = UserSession.getInstance().getLoggedInUser();

        if (currentUser == null) {
            lblUserName.setText("Guest");
            lblBalance.setText("$0.00");
            return;
        }

        lblUserName.setText(currentUser.getUsername());
        lblBalance.setText(String.format("$%,.2f", currentUser.getBalance()));
    }

    public void handleSwitchToHome() {
        searchField.textProperty().unbindBidirectional(SearchStoreController.searchQueryProperty());
        SearchStoreController.reset();
        NavigationUtil.handleSwitchToHomePage(lblUserName);
    }

    public void handleSwitchToSetting() {
        searchField.textProperty().unbindBidirectional(SearchStoreController.searchQueryProperty());
        NavigationUtil.handleSwitchToSetting(new ActionEvent(lblUserName, null));
    }
}
