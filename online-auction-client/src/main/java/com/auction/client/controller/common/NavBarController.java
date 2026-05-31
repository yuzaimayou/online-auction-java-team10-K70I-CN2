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
    private Runnable walletListener;

    @FXML
    public void initialize() {
        refreshUserInfo();
        walletListener = this::refreshUserInfo;
        UserSession.getInstance().addListener(walletListener);
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
    public void dispose() {
        if (walletListener != null) {
            UserSession.getInstance().removeListener(walletListener);
            walletListener = null;
        }
        if (searchField != null) {
            searchField.textProperty().unbindBidirectional(
                    SearchStoreController.searchQueryProperty());
        }
    }

    public void handleSwitchToHome() {
        dispose();
        SearchStoreController.reset();
        NavigationUtil.handleSwitchToHomePage(lblUserName);
    }

    public void handleSwitchToSetting() {
        dispose();
        NavigationUtil.handleSwitchToSetting(new ActionEvent(lblUserName, null));
    }
}
