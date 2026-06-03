package com.auction.client.controller.common;

import com.auction.client.ui.util.ToastUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.client.service.UserSession;
import com.auction.shared.model.account.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

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
    @FXML
    public void handleOpenDeposit(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/setting/DepositPage.fxml"));
            Parent root = loader.load();

            StackPane wrapperPane = new StackPane();
            wrapperPane.getChildren().add(root);

            Stage stage = new Stage();
            stage.setTitle("Nạp tiền vào tài khoản");
            stage.setScene(new javafx.scene.Scene(wrapperPane));

            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtil.showError(((Node) event.getSource()).getScene(), "Không thể mở trang nạp tiền!");
        }
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
