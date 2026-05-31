package com.auction.client.ui;

import com.auction.client.network.NetworkService;
import com.auction.client.service.AutoBidService;
import com.auction.client.service.AutoBidService.AutoBidDecision;
import com.auction.client.util.ItemStatusService;
import com.auction.client.util.ToastUtil;
import com.auction.client.validation.AutoBidValidationService; // Import validator mới
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

public class AutoBidUiHandler {

    private final AutoBidService autoBidManager;
    private final AutoBidValidationService autoBidValidator; // Khai báo validator mới
    private final NetworkService network;
    private final User user;
    private final ItemStatusService statusService;

    // UI refs
    private final VBox autoBidForm;
    private final VBox autoBidActiveStatus;
    private final TextField maxBidField;
    private final TextField autoBidStepField;
    private final Label userCurrentBidLabel;
    private final Button btnAutoBidToggle;
    private final Button submitBid;
    private final Supplier<Item> itemSupplier;
    private final Supplier<Double> myLastBidSupplier;

    public AutoBidUiHandler(
            AutoBidService autoBidManager,
            AutoBidValidationService autoBidValidator, // Nhận validator từ Controller
            NetworkService network,
            User user,
            ItemStatusService statusService,
            VBox autoBidForm,
            VBox autoBidActiveStatus,
            TextField maxBidField,
            TextField autoBidStepField,
            Label userCurrentBidLabel,
            Button btnAutoBidToggle,
            Button submitBid,
            Supplier<Item> itemSupplier,
            Supplier<Double> myLastBidSupplier
    ) {
        this.autoBidManager      = autoBidManager;
        this.autoBidValidator    = autoBidValidator;
        this.network             = network;
        this.user                = user;
        this.statusService       = statusService;
        this.autoBidForm         = autoBidForm;
        this.autoBidActiveStatus = autoBidActiveStatus;
        this.maxBidField         = maxBidField;
        this.autoBidStepField    = autoBidStepField;
        this.userCurrentBidLabel = userCurrentBidLabel;
        this.btnAutoBidToggle    = btnAutoBidToggle;
        this.submitBid           = submitBid;
        this.itemSupplier        = itemSupplier;
        this.myLastBidSupplier   = myLastBidSupplier;
    }

    public void toggleForm() {
        boolean visible = autoBidForm.isVisible();
        autoBidForm.setVisible(!visible);
        autoBidForm.setManaged(!visible);
    }

    public void start() {
        Item item = itemSupplier.get();
        if (!statusService.isOngoing(item)) {
            ToastUtil.showError(maxBidField.getScene(), "Auction is not active.");
            return;
        }
        try {
            double max  = Double.parseDouble(maxBidField.getText().trim());
            double step = Double.parseDouble(autoBidStepField.getText().trim());

            // Thay đổi 1: Gọi hàm validate từ AutoBidValidationService
            AutoBidValidationService.ValidationResult result = autoBidValidator.validate(item, max, step);
            if (!result.ok()) {
                ToastUtil.showError(maxBidField.getScene(), result.errorMessage());
                return;
            }

            autoBidManager.activate(max, step);
            network.sendAutoBidRegister(item.getId(), user.getId(), max, step);
            updateUi(true);
            ToastUtil.showSuccess(maxBidField.getScene(), "Auto-Bid activated!");

            boolean isLeading = user.getId().equals(autoBidManager.getLastBidderId());
            userCurrentBidLabel.setText(isLeading
                    ? String.format("Your current bid: $ %.0f (Leading)", item.getCurrentPrice())
                    : "Auto-bidding...");
        } catch (NumberFormatException e) {
            ToastUtil.showError(maxBidField.getScene(), "Please enter valid numbers.");
        }
    }

    public void stop() {
        autoBidManager.deactivate();
        updateUi(false);
    }

    public void handleDecision(double serverPrice, String topBidderId) {
        Item item = itemSupplier.get();

        // Thay đổi 2: Gọi hàm decideBid gọn nhẹ (không truyền myLastBid sang Service nữa)
        AutoBidDecision decision = autoBidManager.decideBid(
                topBidderId, serverPrice, user.getId(), statusService.isOngoing(item));

        // Thay đổi 3: Đưa toàn bộ việc render chuỗi text về đúng UI Handler tại đây
        switch (decision.type()) {
            case AUCTION_ENDED -> stop();
            case INACTIVE      -> {}
            case LEADING       -> {
                userCurrentBidLabel.setText(String.format("Your current bid: $ %.0f (Leading)", serverPrice));
            }
            case MAX_REACHED   -> {
                updateUi(false);
                ToastUtil.showInfo(userCurrentBidLabel.getScene(), "Auto-bid stopped: Max limit reached!");
            }
            case OUTBID_AND_REBID -> {
                double myLastBid = myLastBidSupplier.get();
                String statusText = myLastBid > 0
                        ? String.format("Your current bid: $ %.0f (Outbid — auto-bidding...)", myLastBid)
                        : "Auto-bidding...";
                userCurrentBidLabel.setText(statusText);
                network.sendBid(item.getId(), user.getId(), decision.nextBidPrice(), "");
            }
        }
    }

    public void updateUi(boolean active) {
        Item item = itemSupplier.get();
        autoBidForm.setVisible(false);
        autoBidForm.setManaged(false);
        autoBidActiveStatus.setVisible(active);
        autoBidActiveStatus.setManaged(active);
        btnAutoBidToggle.setVisible(!active);
        btnAutoBidToggle.setManaged(!active);
        submitBid.setDisable(active || item.getSellerId().equals(user.getId()));
    }
}