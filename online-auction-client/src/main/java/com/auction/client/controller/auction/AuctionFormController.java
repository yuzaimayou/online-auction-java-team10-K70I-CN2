package com.auction.client.controller.auction;

import com.auction.client.service.ItemsService;
import com.auction.client.ui.image.NewItemImageManager;
import com.auction.client.util.NavigationUtil;
import com.auction.client.ui.util.ToastUtil;
import com.auction.client.ui.util.FormUtil;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.client.validation.AuctionFormValidator.Result;
import com.auction.shared.message.ResponseMessage;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class AuctionFormController {
    private static final double NAV_DELAY_SECONDS = 0.5;
    private static final String STATUS_SUCCESS = "success";

    @FXML
    private Label lblMessage;
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
    private Button  btnSubmit;

    private final AtomicBoolean isSubmitting  = new AtomicBoolean(false);
    private final ItemsService itemsService;
    private NewItemImageManager newItemImageManager;

    public AuctionFormController() {
        this.itemsService = ItemsService.getInstance();
    }

    @FXML
    public void initialize() {
        FormUtil.populateHalfHourSlots(cbStartTime, cbEndTime);
        cbStartTime.setValue("00:00");
        cbEndTime.setValue("00:00");
        FormUtil.autoGrowTextArea(txtItemDesc, 80);

        Platform.runLater(() -> {
            newItemImageManager = new NewItemImageManager(
                    dragDropArea, imagesPreviewContainer, smallAddBtn, lblMessage.getScene()
            );
        });
    }


    @FXML
    public void handleAddItem(ActionEvent event) {
        if (isSubmitting.get()) return;

        FormData data = collectFormData();

        // Kiểm tra tính hợp lệ của toàn bộ dữ liệu
        Result result = AuctionFormValidator.validateCreate(
                data.itemName, data.itemDesc, data.category,
                data.startDate, data.endDate,
                data.startTime, data.endTime,
                data.initPriceStr, data.bidStepStr,
                newItemImageManager.getSelectedFiles()
        );
        if (!result.isValid()) {
            showValidationError(result);
            return;
        }
        setLoadingState(true);

        itemsService.createItem(data, newItemImageManager.getSelectedFiles())
                .thenAccept(this::processSubmitResponse)
                .exceptionally(this::processSubmitException);
    }

    @FXML
    public void handleChooseImage() {
        newItemImageManager.chooseImages(dragDropArea.getScene().getWindow());
    }

    @FXML
    public void handleSwitchToHomePage(ActionEvent event) {
        NavigationUtil.handleSwitchToHomePage(lblMessage);
    }


    /**
     * Tiếp nhận thông báo thành công và hiển thị
     */
    private void processSubmitResponse(ResponseMessage response) {
        Platform.runLater(() -> {
            if (STATUS_SUCCESS.equals(response.getStatus())) {
                ToastUtil.showSuccess(lblMessage.getScene(), response.getMessage());

                PauseTransition pause = new PauseTransition(Duration.seconds(NAV_DELAY_SECONDS));
                pause.setOnFinished(e -> NavigationUtil.handleSwitchToHomePage(lblMessage));
                pause.play();
            } else {
                ToastUtil.showInfo(lblMessage.getScene(), response.getMessage());
                setLoadingState(false);
            }
        });
    }

    /**
     * Tiếp nhận thông báo lỗi và hiển thị
     */
    private Void processSubmitException(Throwable error) {
        error.printStackTrace();
        Platform.runLater(() -> {
            ToastUtil.showInfo(lblMessage.getScene(), "Failed to connect to server. Please try again.");
            setLoadingState(false);
        });
        return null;
    }

    /**
     * Chuyển giao dữ liệu đóng gói
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

    public static final class FormData {
        public final String itemName;
        public final String itemDesc;
        public final String category;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final String startTime;
        public final String endTime;
        public final String initPriceStr;
        public final String bidStepStr;

        FormData(String itemName, String itemDesc, String category,
                 LocalDate startDate, LocalDate endDate,
                 String startTime, String endTime,
                 String initPriceStr, String bidStepStr) {
            this.itemName = itemName;
            this.itemDesc = itemDesc;
            this.category = category;
            this.startDate = startDate;
            this.endDate = endDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.initPriceStr = initPriceStr;
            this.bidStepStr = bidStepStr;
        }
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

    /**
     * Cập nhật trạng thái khóa/mở các nút chức năng giao diện trong suốt tiến trình tạo sản
     */
    private void setLoadingState(boolean isLoading) {
        isSubmitting.set(isLoading);

        smallAddBtn.setDisable(isLoading);
        smallAddBtn.setOpacity(isLoading ? 0.5 : 1.0);

        btnSubmit.setDisable(isLoading);
        btnSubmit.setText(isLoading ? "Creating..." : "Add Item");
    }
}