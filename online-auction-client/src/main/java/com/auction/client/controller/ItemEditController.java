package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.ui.ItemImageEditManager;
import com.auction.client.util.NavigationUtil;
import com.auction.client.util.ToastUtil;
import com.auction.client.util.UiUtil;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.Item;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ItemEditController {

    // ── FXML Fields ───────────────────────────────────────────────────────────
    @FXML
    private TextField txtItemName;
    @FXML
    private TextArea  txtItemDesc;
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
    private Label  lblMessage;
    @FXML
    private Button btnSave;

    // ── State & Managers ──────────────────────────────────────────────────────
    private String currentItemId;
    private boolean isSaving = false;
    private ItemImageEditManager imageManager;

    private final ItemsService itemsService = ItemsService.getInstance();

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        UiUtil.populateHalfHourSlots(cbStartTime, cbEndTime);
        UiUtil.autoGrowTextArea(txtItemDesc, 120);
        Platform.runLater(() -> this.imageManager = new ItemImageEditManager(
                dragDropArea, imagesPreviewContainer, smallAddBtn, btnSave.getScene()
        ));
    }

    public void setItemId(String id) {
        this.currentItemId = id;
        itemsService.getItemById(id, "")
                .thenAccept(item -> Platform.runLater(() -> populateForm(item)))
                .exceptionally(this::handleFetchException);
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

        if (imageManager != null) {
            imageManager.initData(item.getImagesPath());
        }
    }

    // ── Event Handlers ────────────────────────────────────────────────────────
    @FXML
    public void handleChooseImage() {
        if (imageManager != null) {
            imageManager.chooseImages(btnSave.getScene().getWindow());
        }
    }

    @FXML
    public void handleSaveChanges(ActionEvent event) {
        if (isSaving || imageManager == null) return;

        String itemName = txtItemName.getText().trim();
        String itemDesc = txtItemDesc.getText().trim();
        String category = getSelectedCategory();
        LocalDate startDate = startDateP.getValue();
        LocalDate endDate = endDateP.getValue();
        String startTime = cbStartTime.getValue();
        String endTime = cbEndTime.getValue();

        List<File> allImages = imageManager.getAllImagesAsFiles();

        AuctionFormValidator.Result result = AuctionFormValidator.validateUpdate(
                itemName, itemDesc, category,
                startDate, endDate, startTime, endTime,
                txtInitPrice.getText().trim(), txtBidStep.getText().trim(), allImages
        );

        if (!result.isValid()) {
            showValidationError(result);
            return;
        }

        setSaveState(true);

        itemsService.updateItem(
                        itemName, itemDesc, category,
                        startDate, endDate, startTime, endTime,
                        txtInitPrice.getText().trim(), txtBidStep.getText().trim(),
                        imageManager.getExistingImagePaths(),
                        imageManager.getNewSelectedFiles(),
                        currentItemId
                )
                .thenAccept(this::handleSaveResponse)
                .exceptionally(this::handleSaveException);
    }

    @FXML
    public void handleCloseAction(ActionEvent event) {
        Button btn = (Button) event.getSource();
        StackPane overlay = (StackPane) btn.getScene().lookup(".popup-overlay");
        if (overlay != null) {
            ((StackPane) overlay.getParent()).getChildren().remove(overlay);
        }
    }

    // ── Response Handlers (Async Callbacks) ───────────────────────────────────
    private void handleSaveResponse(ResponseMessage response) {
        Platform.runLater(() -> {
            setSaveState(false);
            if ("success".equals(response.getStatus())) {
                ToastUtil.showSuccess(lblMessage.getScene(), response.getMessage());
                PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
                pause.setOnFinished(e -> NavigationUtil.handleSwitchToSetting(
                        new ActionEvent(btnSave, btnSave), "myAuctions"
                ));
                pause.play();
            } else {
                ToastUtil.showError(lblMessage.getScene(), response.getMessage());
            }
        });
    }

    private Void handleSaveException(Throwable ex) {
        ex.printStackTrace();
        Platform.runLater(() -> {
            setSaveState(false);
            ToastUtil.showError(lblMessage.getScene(), "Lỗi kết nối. Vui lòng thử lại.");
        });
        return null;
    }

    private Void handleFetchException(Throwable ex) {
        ex.printStackTrace();
        Platform.runLater(() -> ToastUtil.showError(lblMessage.getScene(), "Không thể tải thông tin sản phẩm."));
        return null;
    }

    // ── Private UI Helpers ────────────────────────────────────────────────────
    private String getSelectedCategory() {
        Toggle toggle = categoryGroup.getSelectedToggle();
        return toggle != null ? toggle.getUserData().toString() : null;
    }

    private void selectCategoryToggle(String categoryName) {
        if (categoryName == null || categoryGroup == null) return;
        for (Toggle t : categoryGroup.getToggles()) {
            if (t instanceof ToggleButton tb && categoryName.equalsIgnoreCase((String) tb.getUserData())) {
                categoryGroup.selectToggle(t);
                break;
            }
        }
    }

    private void showValidationError(AuctionFormValidator.Result result) {
        String msg = result.getError().message;
        switch (result.getError()) {
            case BID_STEP_EXCEEDS_PRICE, START_TIME_IN_PAST, END_TIME_BEFORE_START ->
                    ToastUtil.showError(lblMessage.getScene(), msg);
            default -> ToastUtil.showInfo(lblMessage.getScene(), msg);
        }
    }

    private void setSaveState(boolean saving) {
        this.isSaving = saving;
        btnSave.setDisable(saving);
        btnSave.setText(saving ? "Đang lưu..." : "Save Changes");
        smallAddBtn.setDisable(saving);
        smallAddBtn.setOpacity(saving ? 0.5 : 1.0);
    }

    private String snapToSlot(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute() >= 30 ? 30 : 0);
    }
}