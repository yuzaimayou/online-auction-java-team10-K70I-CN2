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
    // auth info
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
        displayWalletPlaceholder();
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
                /*  setText(emailField, currentUser.getEmail());
        setText(userNameField, currentUser.getUsername());

        // FIX [refactor]: dùng role thay vì so sánh username để phân biệt admin
        if (roleUserField != null) {
            boolean isAdmin = currentUser.getRole() != null
                    && currentUser.getRole().equalsIgnoreCase("admin");
            roleUserField.setText(isAdmin ? "Admin Account" : "User Account"); */
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

    // ── Wallet ─────────────────────────────────────────────────────────────────
    /**
     * FIX [dead code]: thay hardcode bằng method public để
     * inject dữ liệu thật từ WalletService khi sẵn sàng.
     * Gọi method này từ bên ngoài sau khi load xong wallet:
     *   controller.loadWalletData(available, frozen, total);
     */
    public void loadWalletData(double available, double frozen, double total) {
        // FIX [error handling]: null-check trước khi set text
        if (availableBalanceLabel != null)
            availableBalanceLabel.setText(String.format("$%,.2f", available));
        if (frozenBalanceLabel != null)
            frozenBalanceLabel.setText(String.format("$%,.2f", frozen));
        if (totalBalanceLabel != null)
            totalBalanceLabel.setText(String.format("$%,.2f", total));
    }

    /** Hiển thị trạng thái loading khi chờ WalletService. */
    private void displayWalletPlaceholder() {
        if (availableBalanceLabel != null) availableBalanceLabel.setText("Loading...");
        if (frozenBalanceLabel    != null) frozenBalanceLabel.setText("Loading...");
        if (totalBalanceLabel     != null) totalBalanceLabel.setText("Loading...");
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