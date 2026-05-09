package com.auction.client.controller.setting;

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
            String amount = clickedButton.getText();
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
        String amount = amountField.getText();
        System.out.println("Đang xử lý nạp tiền: " + amount);
    }
}