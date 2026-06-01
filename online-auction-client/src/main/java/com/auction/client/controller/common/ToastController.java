package com.auction.client.controller.common;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ToastController {

    @FXML
    private Label messageLabel;

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}