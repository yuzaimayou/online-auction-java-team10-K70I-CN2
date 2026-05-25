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

        searchField.textProperty().bindBidirectional(SearchStoreController.searchQueryProperty());
    }

    public void refreshUserInfo() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && lblUserName != null) {
            lblUserName.setText(currentUser.getUsername());
        }
        if (lblBalance != null) {
            double availableBalance = currentUser.getBalance();
            lblBalance.setText(String.format("$%,.2f", availableBalance));
        } else {
            if (lblUserName != null) lblUserName.setText("Guest");
            if (lblBalance != null) lblBalance.setText("$0.00");
        }
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
