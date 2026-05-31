package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.util.ToastUtil;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.client.util.UiUtil;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.shared.model.item.Item;
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
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ItemEditController {

    // ── FXML Fields ──
    @FXML
    private TextField txtItemName;
    @FXML
    private TextArea txtItemDesc;
    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private TextField txtInitPrice;
    @FXML
    private TextField txtBidStep;

    @FXML
    private DatePicker startDateP;
    @FXML
    private ComboBox<String> cbStartTime;
    @FXML
    private DatePicker endDateP;
    @FXML
    private ComboBox<String> cbEndTime;

    @FXML
    private VBox dragDropArea;
    @FXML
    private HBox imagesPreviewContainer;
    @FXML
    private VBox smallAddBtn;

    @FXML
    private Label lblMessage;
    @FXML
    private Button btnSave;

    // ── State ──
    private String currentItemId;
    private boolean isSaving = false;

    private List<String> existingImagePaths = new ArrayList<>();
    private final List<File> newSelectedFiles = new ArrayList<>();

    private static final int MAX_IMAGES = 5;
    private final ItemsService itemsService = ItemsService.getInstance();

    @FXML
    public void initialize() {
        UiUtil.populateHalfHourSlots(cbStartTime, cbEndTime);
        UiUtil.autoGrowTextArea(txtItemDesc, 120);
    }

    public void setItemId(String id) {
        this.currentItemId = id;
        itemsService.getItemById(id, "")
                .thenAccept(item -> Platform.runLater(() -> populateForm(item)))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            ToastUtil.showError(
                                    lblMessage.getScene(),
                                    "Không thể tải thông tin sản phẩm."
                            )
                    );
                    return null;
                });
    }

    private void populateForm(Item item) {
        if (item == null) return;

        txtItemName.setText(item.getName() != null ? item.getName() : "");
        txtItemDesc.setText(item.getDescription() != null ? item.getDescription() : "");
        txtInitPrice.setText(String.format("%.0f", item.getStartingPrice()));
        txtBidStep.setText(String.format("%.0f", item.getBidStep()));

        selectCategoryToggle(item.getCategory());

        if (item.getStartTime() != null) {
            startDateP.setValue(item.getStartTime().toLocalDate());
            cbStartTime.setValue(snapToSlot(item.getStartTime().toLocalTime()));
        }
        if (item.getEndTime() != null) {
            endDateP.setValue(item.getEndTime().toLocalDate());
            cbEndTime.setValue(snapToSlot(item.getEndTime().toLocalTime()));
        }

        existingImagePaths = item.getImagesPath() != null ? new ArrayList<>(item.getImagesPath()) : new ArrayList<>();
        newSelectedFiles.clear();

        renderImageStrip();
    }

    private void renderImageStrip() {
        int total = existingImagePaths.size() + newSelectedFiles.size();
        boolean hasImages = total > 0;

        dragDropArea.setVisible(!hasImages);
        dragDropArea.setManaged(!hasImages);
        imagesPreviewContainer.setVisible(hasImages);
        imagesPreviewContainer.setManaged(hasImages);

        if (!hasImages) return;
        imagesPreviewContainer.getChildren().clear();
        for (String imgPath : existingImagePaths) {
            imagesPreviewContainer.getChildren().add(
                    ClientImageUtil.createServerImageCard(imgPath, () -> {
                        existingImagePaths.remove(imgPath);
                        renderImageStrip();
                    })
            );
        }
        for (File file : newSelectedFiles) {
            imagesPreviewContainer.getChildren().add(
                    ClientImageUtil.createImageCard(file, () -> {
                        newSelectedFiles.remove(file);
                        renderImageStrip();
                    })
            );
        }
        if (total < MAX_IMAGES) {
            imagesPreviewContainer.getChildren().add(smallAddBtn);
        }
    }


    //  Event Handlers & API Call
    @FXML
    public void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        List<File> files = fc.showOpenMultipleDialog(btnSave.getScene().getWindow());
        if (files == null || files.isEmpty()) return;

        if (existingImagePaths.size() + newSelectedFiles.size() + files.size() > MAX_IMAGES) {
            ToastUtil.showInfo(lblMessage.getScene(), "Tối đa " + MAX_IMAGES + " ảnh.");
            return;
        }
        newSelectedFiles.addAll(files);
        renderImageStrip();
    }

    private String getSelectedCategory() {
        Toggle toggle = categoryGroup.getSelectedToggle();
        return toggle != null ? toggle.getUserData().toString() : null;
    }
    private void showValidationError(AuctionFormValidator.Result result) {
        String msg = result.getError().message;

        switch (result.getError()) {
            case BID_STEP_EXCEEDS_PRICE,
                 START_TIME_IN_PAST,
                 END_TIME_BEFORE_START ->
                    ToastUtil.showError(lblMessage.getScene(), msg);
            default ->
                    ToastUtil.showInfo(lblMessage.getScene(), msg);
        }
    }

    private void setSaveEnabled(boolean enabled) {
        btnSave.setDisable(!enabled);
        btnSave.setText(enabled ? "Save Changes" : "Đang lưu..."
        );
        smallAddBtn.setDisable(!enabled);
        smallAddBtn.setOpacity(enabled ? 1 : 0.5
        );
    }
    private void resetSubmitState() {
        isSaving = false;
        setSaveEnabled(true);
    }

    @FXML
    public void handleSaveChanges(ActionEvent event) {
        if (isSaving) return;
        String itemName = txtItemName.getText().trim();
        String itemDesc = txtItemDesc.getText().trim();
        String category = getSelectedCategory();
        LocalDate startDate = startDateP.getValue();
        LocalDate endDate = endDateP.getValue();
        String startTime = cbStartTime.getValue();
        String endTime = cbEndTime.getValue();

        List<File> allImages = new ArrayList<>(newSelectedFiles);
        existingImagePaths.forEach(img -> allImages.add(new File(img)));

        AuctionFormValidator.Result result = AuctionFormValidator.validateUpdate(
                        itemName, itemDesc, category,
                        startDate, endDate, startTime, endTime,
                        txtInitPrice.getText().trim(), txtBidStep.getText().trim(), allImages);
        if (!result.isValid()) {
            showValidationError(result);
            return;
        }
        isSaving = true;
        setSaveEnabled(false);

        itemsService.updateItem(
                        itemName, itemDesc, category,
                        startDate, endDate, startTime, endTime,
                        txtInitPrice.getText().trim(), txtBidStep.getText().trim(),
                        existingImagePaths, newSelectedFiles, currentItemId
                )
                .thenAccept(response ->
                        Platform.runLater(() -> {
                            resetSubmitState();
                            if ("success".equals(response.getStatus())) {
                                ToastUtil.showSuccess(lblMessage.getScene(), response.getMessage());
                                PauseTransition pause = new PauseTransition(Duration.seconds(1.2));

                                pause.setOnFinished(e -> navigateToMyAuctions());
                                pause.play();
                            } else {
                                ToastUtil.showError(lblMessage.getScene(), response.getMessage()
                                );
                            }
                        })
                )
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        resetSubmitState();
                        ToastUtil.showError(lblMessage.getScene(),
                                "Lỗi kết nối. Vui lòng thử lại."
                        );
                    });
                    return null;
                });
    }
    @FXML
    public void handleCloseAction(ActionEvent event) {
        Button btn = (Button) event.getSource();
        StackPane overlay = (StackPane) btn.getScene().lookup(".popup-overlay");
        if (overlay != null) {
            ((StackPane) overlay.getParent())
                    .getChildren()
                    .remove(overlay);
        }
    }
    private void navigateToMyAuctions() {
        NavigationUtil.handleSwitchToSetting(
                new ActionEvent(btnSave, btnSave),
                "myAuctions"
        );
    }
    // ──────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void selectCategoryToggle(String categoryName) {
        if (categoryName == null || categoryGroup == null) return;
        for (Toggle t : categoryGroup.getToggles()) {
            if (t instanceof ToggleButton tb && categoryName.equalsIgnoreCase((String) tb.getUserData())) {
                categoryGroup.selectToggle(t);
                break;
            }
        }
    }

    private String snapToSlot(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute() >= 30 ? 30 : 0);
    }
}