package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.util.NavigationUtil;
import com.auction.client.util.ToastUtil;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.client.validation.AuctionFormValidator.Result;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Trách nhiệm:
 *   1. Thu thập data từ UI
 *   2. Validate format qua AuctionFormValidator
 *   3. Giao ItemsService xử lý
 *   4. Hiển thị kết quả
 * Không biết gì về: JSON, base64, userId, ItemPayload.
 */
public class AuctionFormController {

    @FXML
    private Label  lblMessage;
    @FXML
    private TextField txtItemName;
    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private DatePicker startDateP;
    @FXML
    private DatePicker endDateP;
    @FXML
    private ComboBox<String> cbStartTime;
    @FXML
    private ComboBox<String> cbEndTime;
    @FXML
    private TextArea txtItemDesc;
    @FXML
    private TextField txtInitPrice;
    @FXML
    private TextField txtBidStep;
    @FXML
    private VBox dragDropArea;
    @FXML
    private HBox imagesPreviewContainer;
    @FXML
    private VBox smallAddBtn;
    @FXML
    private Button btnSubmit;

    private final List<File> selectedFiles = new ArrayList<>();
    private static final int MAX_IMAGES = 5;
    private boolean isSubmitting = false;

    // ── Dependency ────────────────────────────────────────────────────────────
    private final ItemsService itemsService = ItemsService.getInstance();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        populateTimePickers();
        setupDescriptionAutoResize();
    }
    // ── Event handlers ────────────────────────────────────────────────────────

    @FXML
    public void handleAddItem(ActionEvent event) {
        if (isSubmitting) return;

        String itemName = txtItemName.getText().trim();
        String itemDesc = txtItemDesc.getText().trim();
        String category = getSelectedCategory();
        LocalDate startDate = startDateP.getValue();
        LocalDate endDate = endDateP.getValue();
        String startTime = cbStartTime.getValue();
        String endTime = cbEndTime.getValue();
        String initPriceStr = txtInitPrice.getText().trim();
        String bidStepStr = txtBidStep.getText().trim();

        // 2. Validate
        Result result = AuctionFormValidator.validateCreate(
                itemName, itemDesc, category, startDate, endDate, startTime, endTime,
                initPriceStr, bidStepStr, selectedFiles
        );
        if (!result.isValid()) {
            showValidationError(result);
            return;
        }

        // 3. Submit — service tự lo phần còn lại
        isSubmitting = true;
        setSubmitEnabled(false);
        itemsService.createItem(
                        itemName, itemDesc, category,
                        startDate, endDate, startTime, endTime,
                        initPriceStr, bidStepStr, selectedFiles
                )
                .thenAccept(response -> {
                    if ("success".equals(response.getStatus())) {
                        Platform.runLater(() ->
                                ToastUtil.showSuccess(lblMessage.getScene(), response.getMessage()));
                        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                        pause.setOnFinished(e -> NavigationUtil.handleSwitchToHomePage(lblMessage));
                        pause.play();
                    } else {
                        isSubmitting = false;
                        Platform.runLater(() -> {
                            ToastUtil.showInfo(lblMessage.getScene(), response.getMessage());
                            setSubmitEnabled(true);
                        });
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    isSubmitting = false;
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

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void populateTimePickers() {
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                String time = String.format("%02d:%02d", h, m);
                cbStartTime.getItems().add(time);
                cbEndTime.getItems().add(time);
            }
        }
        cbStartTime.setValue("00:00");
        cbEndTime.setValue("00:00");
    }

    private void setupDescriptionAutoResize() {
        txtItemDesc.setWrapText(true);
        txtItemDesc.textProperty().addListener((obs, oldVal, newVal) -> {
            javafx.scene.text.Text helper = new javafx.scene.text.Text(newVal);
            helper.setFont(txtItemDesc.getFont());
            helper.setWrappingWidth(txtItemDesc.getWidth() - 40);
            txtItemDesc.setPrefHeight(Math.max(80, helper.getLayoutBounds().getHeight() + 40));
        });
    }

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
            for (File f : selectedFiles)
                imagesPreviewContainer.getChildren().add(idx++, createImageCard(f));
            smallAddBtn.setVisible(selectedFiles.size() < MAX_IMAGES);
        }
    }

    private StackPane createImageCard(File file) {
        final double W = 150, H = 120;

        ImageView iv = new ImageView(new Image(file.toURI().toString()));
        iv.setPreserveRatio(true);
        double ratio = iv.getImage().getWidth() / iv.getImage().getHeight();
        if (ratio > W / H) iv.setFitHeight(H); else iv.setFitWidth(W);

        VBox box = new VBox(iv);
        box.getStyleClass().add("image-border-container");
        box.setAlignment(Pos.CENTER);
        box.setMinSize(W, H);
        box.setMaxSize(W, H);
        Rectangle clip = new Rectangle(W, H);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        box.setClip(clip);

        Button del = new Button("✕");
        del.getStyleClass().add("delete-photo-btn");
        StackPane.setAlignment(del, Pos.TOP_RIGHT);
        del.setOnAction(e -> { selectedFiles.remove(file); refreshImagePreview(); });

        StackPane card = new StackPane(box, del);
        card.setPickOnBounds(false);
        return card;
    }
}