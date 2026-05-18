package com.auction.client.controller.setting;

import com.auction.client.service.DepositService;
import com.auction.client.util.UserSession;
import com.auction.client.service.ToastService;
import com.auction.shared.model.account.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class DepositController {
    @FXML
    private TextField amountField;
    @FXML
    private ToggleGroup amountGroup;
    private boolean isUpdatingFromButton = false;

    @FXML
    public void initialize() {
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingFromButton) return;
            if (amountGroup.getSelectedToggle() != null) {
                ToggleButton selectedBtn = (ToggleButton) amountGroup.getSelectedToggle();
                String cleanBtnText = selectedBtn.getText().replace("$", "").replace(",", "");
                if (!newValue.equals(cleanBtnText)) {
                    amountGroup.selectToggle(null);
                }
            }
        });
    }

    @FXML
    private void handleAmountSelection(ActionEvent event) {
        ToggleButton clickedButton = (ToggleButton) event.getSource();
        isUpdatingFromButton = true;
        if (amountGroup.getSelectedToggle() == null) {
            amountField.clear();
        } else {
            String amount = clickedButton.getText().replace("$", "").replace(",", "").trim();
            amountField.setText(amount);
        }
        isUpdatingFromButton = false;
    }

    @FXML
    private void handleBackToWallet(ActionEvent event) {
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/ProfilePage.fxml");
        }
    }

    @FXML
    private void handleConfirmDeposit(ActionEvent event) {
        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            ToastService.showError(amountField.getScene(), "Vui lòng nhập số tiền!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            ToastService.showError(amountField.getScene(), "Số tiền không hợp lệ!");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) return;

        DepositService.getInstance().deposit(currentUser.getId(), amount)
                .thenAccept(newBalance -> {
                    Platform.runLater(() -> {
                        currentUser.setBalance(newBalance);

                        ToastService.showSuccess(amountField.getScene(), "Nạp tiền thành công!");
                        handleBackToWallet(null);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ToastService.showError(amountField.getScene(), ex.getCause().getMessage());
                    });
                    return null;
                });
    }
}