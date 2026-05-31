package com.auction.client.controller.setting;

import com.auction.client.service.WalletService;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class ProfilePageController {
    // Auth info
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

            // FIX: Thay vì check hardcode username "admin", ta dùng thuộc tính Role từ Object User
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

    // ── Wallet ─────────────────────────────────────────────────────────────────

    /**
     * Cập nhật dữ liệu ví lên giao diện.
     * Hàm này dùng chung cho cả việc load nội bộ lẫn nạp dữ liệu từ WalletService bên ngoài.
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
    /** Hiển thị trạng thái loading khi chờ WalletService cập nhật. */
    private void displayWalletPlaceholder() {
        if (availableBalanceLabel != null) availableBalanceLabel.setText("Loading...");
        if (frozenBalanceLabel    != null) frozenBalanceLabel.setText("Loading...");
        if (totalBalanceLabel     != null) totalBalanceLabel.setText("Loading...");
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    @FXML
    private void handleProfileInfo(ActionEvent event) {
        if (profileInfoBtn != null) {
            profileInfoBtn.setSelected(true);
        }
    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/MyAuctionsPage.fxml");
        }
    }

    @FXML
    private void handleDepositAction(ActionEvent event) {
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/DepositPage.fxml");
        }
    }
    public void dispose() {
        if (sessionListener != null) {
            UserSession.getInstance().removeListener(sessionListener);
            sessionListener = null;
        }
    }
}