package com.auction.client.ui.auction;

import com.auction.client.ui.util.ToastUtil;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * VIEW COMPONENT
 * Chịu trách nhiệm duy nhất là quản lý, thay đổi trạng thái hiển thị
 * và thu thập dữ liệu thô từ các thành phần giao diện JavaFX đồ họa.
 */
public class AutoBidPaneWrapper {

    private final VBox autoBidForm;
    private final VBox autoBidActiveStatus;
    private final TextField maxBidField;
    private final TextField autoBidStepField;
    private final Label userCurrentBidLabel;
    private final Button btnAutoBidToggle;
    private final Button submitBid;

    public AutoBidPaneWrapper(VBox autoBidForm, VBox autoBidActiveStatus, TextField maxBidField,
                              TextField autoBidStepField, Label userCurrentBidLabel,
                              Button btnAutoBidToggle, Button submitBid) {
        this.autoBidForm = autoBidForm;
        this.autoBidActiveStatus = autoBidActiveStatus;
        this.maxBidField = maxBidField;
        this.autoBidStepField = autoBidStepField;
        this.userCurrentBidLabel = userCurrentBidLabel;
        this.btnAutoBidToggle = btnAutoBidToggle;
        this.submitBid = submitBid;
    }

    public String getMaxBidInput() {
        return maxBidField.getText().trim();
    }

    public String getAutoBidStepInput() {
        return autoBidStepField.getText().trim();
    }

    public void toggleFormVisibility() {
        boolean isVisible = autoBidForm.isVisible();
        autoBidForm.setVisible(!isVisible);
        autoBidForm.setManaged(!isVisible);
    }

    public void renderActiveState(boolean active, boolean isSeller) {
        autoBidForm.setVisible(false);
        autoBidForm.setManaged(false);
        autoBidActiveStatus.setVisible(active);
        autoBidActiveStatus.setManaged(active);
        btnAutoBidToggle.setVisible(!active);
        btnAutoBidToggle.setManaged(!active);
        submitBid.setDisable(active || isSeller);
    }

    public void updateStatusText(String text) {
        userCurrentBidLabel.setText(text);
    }

    public void showErrorMessage(String message) {
        ToastUtil.showError(maxBidField.getScene(), message);
    }

    public void showSuccessMessage(String message) {
        ToastUtil.showSuccess(maxBidField.getScene(), message);
    }

    public void showInfoMessage(String message) {
        ToastUtil.showInfo(userCurrentBidLabel.getScene(), message);
    }
}