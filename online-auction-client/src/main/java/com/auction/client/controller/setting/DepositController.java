package com.auction.client.controller.setting;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class DepositController {

    @FXML
    private TextField amountField;

    @FXML
    private void handleBackToWallet(ActionEvent event) {
        // Quay lại trang thông tin cá nhân
        if (SettingController.getInstance() != null) {
            SettingController.getInstance().setDynamicContent("/com.auction.client/fxml/setting/ProfilePage.fxml");
        }
    }

    @FXML
    private void handleConfirmDeposit(ActionEvent event) {
        String amount = amountField.getText();
        System.out.println("Đang xử lý nạp tiền: " + amount);
        // Thêm logic xử lý API nạp tiền tại đây
    }
}