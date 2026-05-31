package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.client.util.ToastUtil;
import com.auction.client.util.UiUtil;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.client.validation.AuctionFormValidator.Result;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Trách nhiệm:
 *   1. Thu thập data từ UI
 *   2. Validate format qua AuctionFormValidator
 *   3. Giao ItemsService xử lý
 *   4. Hiển thị kết quả
 * Không biết gì về: JSON, base64, userId, ItemPayload.
 */
public class AuctionFormController {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int    MAX_IMAGES        = 5;
    private static final double NAV_DELAY_SECONDS = 0.5;
    private static final String STATUS_SUCCESS    = "success";

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Label            lblMessage;
    @FXML private TextField        txtItemName;
    @FXML private ToggleGroup      categoryGroup;
    @FXML private DatePicker       startDateP;
    @FXML private DatePicker       endDateP;
    @FXML private ComboBox<String> cbStartTime;
    @FXML private ComboBox<String> cbEndTime;
    @FXML private TextArea         txtItemDesc;
    @FXML private TextField        txtInitPrice;
    @FXML private TextField        txtBidStep;
    @FXML private VBox             dragDropArea;
    @FXML private HBox             imagesPreviewContainer;
    @FXML private VBox             smallAddBtn;
    @FXML private Button           btnSubmit;

    private final List<File>    selectedFiles = new ArrayList<>();
    // FIX: dùng AtomicBoolean tránh race condition giữa JavaFX thread
    // và CompletableFuture callback thread
    private final AtomicBoolean isSubmitting  = new AtomicBoolean(false);

    // ── Dependency ────────────────────────────────────────────────────────────
    private final ItemsService itemsService;

    public AuctionFormController() {
        this.itemsService = ItemsService.getInstance();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        UiUtil.populateHalfHourSlots(cbStartTime, cbEndTime);
        cbStartTime.setValue("00:00");
        cbEndTime.setValue("00:00");
        UiUtil.autoGrowTextArea(txtItemDesc, 80);
    }
    // ── Event handlers ────────────────────────────────────────────────────────

    @FXML
    public void handleAddItem(ActionEvent event) {
        if (isSubmitting.get()) return;

        // 1. Thu thập data từ UI
        FormData data = collectFormData();

        // 2. Validate
        Result result = AuctionFormValidator.validateCreate(
                data.itemName, data.itemDesc, data.category,
                data.startDate, data.endDate,
                data.startTime, data.endTime,
                data.initPriceStr, data.bidStepStr,
                selectedFiles
        );
        if (!result.isValid()) {
            showValidationError(result);
            return;
        }

        // 3. Submit — service tự lo phần còn lại
        isSubmitting.set(true);
        setSubmitEnabled(false);

        itemsService.createItem(
                        data.itemName, data.itemDesc, data.category,
                        data.startDate, data.endDate,
                        data.startTime, data.endTime,
                        data.initPriceStr, data.bidStepStr,
                        selectedFiles
                )
                .thenAccept(response -> {
                    if (STATUS_SUCCESS.equals(response.getStatus())) {
                        Platform.runLater(() -> {
                            ToastUtil.showSuccess(lblMessage.getScene(), response.getMessage());
                            PauseTransition pause = new PauseTransition(Duration.seconds(NAV_DELAY_SECONDS));
                            pause.setOnFinished(e -> NavigationUtil.handleSwitchToHomePage(lblMessage));
                            pause.play();
                        });
                    } else {
                        isSubmitting.set(false);
                        Platform.runLater(() -> {
                            ToastUtil.showInfo(lblMessage.getScene(), response.getMessage());
                            setSubmitEnabled(true);
                        });
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    isSubmitting.set(false);
                    Platform.runLater(() -> {
                        ToastUtil.showInfo(lblMessage.getScene(),
                                "Failed to connect to server. Please submit again");
                        setSubmitEnabled(true);
                    });
                    return null;
                });
    }

    @FXML
    public void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        List<File> files = chooser.showOpenMultipleDialog(dragDropArea.getScene().getWindow());
        if (files == null) return;

        if (selectedFiles.size() + files.size() > MAX_IMAGES) {
            ToastUtil.showInfo(lblMessage.getScene(), "Only upload max " + MAX_IMAGES + " images");
            return;
        }
        selectedFiles.addAll(files);
        refreshImagePreview();
    }

    @FXML
    public void handleSwitchToHomePage(ActionEvent event) {
        NavigationUtil.handleSwitchToHomePage(lblMessage);
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    /**
     * Thu thập toàn bộ input từ form vào một object duy nhất.
     * Giúp handleAddItem không bị rối với quá nhiều biến local.
     */
    private FormData collectFormData() {
        return new FormData(
                txtItemName.getText().trim(),
                txtItemDesc.getText().trim(),
                getSelectedCategory(),
                startDateP.getValue(),
                endDateP.getValue(),
                cbStartTime.getValue(),
                cbEndTime.getValue(),
                txtInitPrice.getText().trim(),
                txtBidStep.getText().trim()
        );
    }

    /** Simple data holder cho form input — không có logic. */
    private static final class FormData {
        final String itemName;
        final String itemDesc;
        final String    category;
        final LocalDate startDate;
        final LocalDate endDate;
        final String    startTime;
        final String    endTime;
        final String    initPriceStr;
        final String    bidStepStr;

        FormData(String itemName, String itemDesc, String category,
                 LocalDate startDate, LocalDate endDate,
                 String startTime, String endTime,
                 String initPriceStr, String bidStepStr) {
            this.itemName     = itemName;
            this.itemDesc     = itemDesc;
            this.category     = category;
            this.startDate    = startDate;
            this.endDate      = endDate;
            this.startTime    = startTime;
            this.endTime      = endTime;
            this.initPriceStr = initPriceStr;
            this.bidStepStr   = bidStepStr;
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private String getSelectedCategory() {
        Toggle toggle = categoryGroup.getSelectedToggle();
        return toggle != null ? toggle.getUserData().toString() : null;
    }

    private void showValidationError(Result result) {
        String msg = result.getError().message;
        switch (result.getError()) {
            case BID_STEP_EXCEEDS_PRICE,
                 START_TIME_IN_PAST,
                 END_TIME_BEFORE_START -> ToastUtil.showError(lblMessage.getScene(), msg);
            default                    -> ToastUtil.showInfo(lblMessage.getScene(), msg);
        }
    }

    private void setSubmitEnabled(boolean enabled) {
        smallAddBtn.setDisable(!enabled);
        smallAddBtn.setOpacity(enabled ? 1.0 : 0.5);
        btnSubmit.setDisable(!enabled);
        btnSubmit.setText(enabled ? "Add Item" : "Creating...");
    }

    private void refreshImagePreview() {
        boolean hasFiles = !selectedFiles.isEmpty();
        dragDropArea.setVisible(!hasFiles);
        dragDropArea.setManaged(!hasFiles);
        imagesPreviewContainer.setVisible(hasFiles);
        imagesPreviewContainer.setManaged(hasFiles);

        if (hasFiles) {
            imagesPreviewContainer.getChildren().removeIf(n -> n != smallAddBtn);
            int idx = imagesPreviewContainer.getChildren().indexOf(smallAddBtn);
            for (File f : selectedFiles) {

                StackPane card = ClientImageUtil.createImageCard(f, () -> {
                    selectedFiles.remove(f);
                    refreshImagePreview();
                });
                imagesPreviewContainer.getChildren().add(idx++, card);
            }
            smallAddBtn.setVisible(selectedFiles.size() < MAX_IMAGES);
        }
    }
}