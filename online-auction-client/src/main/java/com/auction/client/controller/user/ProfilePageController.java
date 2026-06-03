package com.auction.client.controller.user;

import com.auction.client.service.WalletService;
import com.auction.client.service.UserSession;
import com.auction.client.ui.util.ToastUtil;
import com.auction.shared.model.account.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePageController {
    @FXML
    private TextField emailField;
    @FXML
    private TextField userNameField;
    @FXML
    private TextField roleUserField;
    @FXML
    private ToggleButton profileInfoBtn;
    // Wallet
    @FXML
    private Label availableBalanceLabel;
    @FXML
    private Label frozenBalanceLabel;
    @FXML
    private Label totalBalanceLabel;
    private Runnable sessionListener;

    @FXML
    public void initialize() {
        if (profileInfoBtn != null) {
            profileInfoBtn.setSelected(true);
        }

        displayUserData();

        refreshWallet();
        sessionListener = () -> {
            displayUserData();
            refreshWallet();
        };
        UserSession.getInstance().addListener(sessionListener);
        WalletService.getInstance().fetchAndSync();
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
                boolean isAdmin = currentUser.getRole() != null
                        && currentUser.getRole().equalsIgnoreCase("admin");
                roleUserField.setText(isAdmin ? "Admin Account" : "User Account");
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


    /**
     * Cập nhật dữ liệu ví lên giao diện.
     */
    public void loadWalletData(double available, double frozen, double total) {
        if (availableBalanceLabel != null)
            availableBalanceLabel.setText(String.format("$%,.2f", available));
        if (frozenBalanceLabel != null)
            frozenBalanceLabel.setText(String.format("$%,.2f", frozen));
        if (totalBalanceLabel != null)
            totalBalanceLabel.setText(String.format("$%,.2f", total));
    }
    private void refreshWallet() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            double available = currentUser.getBalance();
            double frozen = currentUser.getFrozenBalance();
            double total = available + frozen;

            loadWalletData(available, frozen, total);
        } else {
            displayWalletPlaceholder();
        }
    }
    private void displayWalletPlaceholder() {
        if (availableBalanceLabel != null) availableBalanceLabel.setText("Loading...");
        if (frozenBalanceLabel != null) frozenBalanceLabel.setText("Loading...");
        if (totalBalanceLabel != null) totalBalanceLabel.setText("Loading...");
    }


    @FXML
    private void handleDepositAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/setting/DepositPage.fxml"));
            Parent root = loader.load();

            StackPane wrapperPane = new StackPane();
            wrapperPane.getChildren().add(root);

            Stage stage = new Stage();
            stage.setTitle("Nạp tiền vào tài khoản");
            stage.setScene(new javafx.scene.Scene(wrapperPane));

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtil.showError(((Node) event.getSource()).getScene(), "Không thể mở trang nạp tiền!");
        }
    }
}