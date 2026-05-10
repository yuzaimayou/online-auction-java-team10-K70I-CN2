package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ItemEditController {

    // thông tin và phân loại sản phẩm
    @FXML
    private TextField txtItemName;
    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private TextArea txtItemDesc;

    // set up giá
    @FXML
    private TextField txtInitPrice;
    @FXML
    private TextField txtBidStep;

    // Time
    @FXML
    private DatePicker startDateP;
    @FXML
    private ComboBox<String> cbStartTime;
    @FXML
    private DatePicker endDateP;
    @FXML
    private ComboBox<String> cbEndTime;

    // Phản hồi
    @FXML private Label lblMessage;

    private String currentItemId;
    private final ItemsService itemsService = ItemsService.getInstance();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @FXML
    public void initialize() {
        // Khởi tạo danh sách giờ cho ComboBox
        cbStartTime.getItems().clear();
        cbEndTime.getItems().clear();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                cbStartTime.getItems().add(time);
                cbEndTime.getItems().add(time);
            }
        }

        // Tự động giãn dòng cho TextArea Description
        if (txtItemDesc != null) {
            txtItemDesc.setWrapText(true);
            txtItemDesc.textProperty().addListener((observable, oldValue, newValue) -> {
                javafx.scene.text.Text helper = new javafx.scene.text.Text();
                helper.setText(newValue);
                helper.setFont(txtItemDesc.getFont());
                helper.setWrappingWidth(txtItemDesc.getWidth() - 40);
                double textHeight = helper.getLayoutBounds().getHeight();
                txtItemDesc.setPrefHeight(Math.max(80, textHeight + 40));
            });
        }
    }

    // Lấy data item
    public void setEditData(Item item) {
        if (item == null) return;

        this.currentItemId = item.getId();

        txtItemName.setText(item.getName() != null ? item.getName() : "");
        txtItemDesc.setText(item.getDescription() != null ? item.getDescription() : "");
        txtInitPrice.setText(String.valueOf(item.getStartingPrice()));
        txtBidStep.setText(String.valueOf(item.getBidStep()));

        if (item.getCategory() != null && categoryGroup != null) {
            for (Toggle toggle : categoryGroup.getToggles()) {
                if (toggle.getUserData() != null && toggle.getUserData().toString().equalsIgnoreCase(item.getCategory())) {
                    categoryGroup.selectToggle(toggle);
                    break;
                }
            }
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        if (item.getStartTime() != null) {
            startDateP.setValue(item.getStartTime().toLocalDate());
            cbStartTime.setValue(item.getStartTime().toLocalTime().format(timeFormatter));
        }
        if (item.getEndTime() != null) {
            endDateP.setValue(item.getEndTime().toLocalDate());
            cbEndTime.setValue(item.getEndTime().toLocalTime().format(timeFormatter));
        }
    }

    // xử lí hành động người dùng
    @FXML
    public void handleSaveChanges(ActionEvent event) {
        String itemName = txtItemName.getText().trim();
        String itemDesc = txtItemDesc.getText().trim();
        Toggle selectedToggle = categoryGroup.getSelectedToggle();

        LocalDate startDate = startDateP.getValue();
        LocalDate endDate = endDateP.getValue();
        String startTime = cbStartTime.getValue();
        String endTime = cbEndTime.getValue();

        Double initPrice = convertNumeric(txtInitPrice.getText().trim());
        Double bidStep = convertNumeric(txtBidStep.getText().trim());

        // Validate cơ bản
        if (isAnyNull(itemName, itemDesc, selectedToggle, startDate, endDate, startTime, endTime, initPrice, bidStep)) {
            showMessage("Please fill in all required fields.", Color.RED);
            return;
        }

        if (initPrice == -2.0 || bidStep == -2.0) {
            showMessage("Price and Bid Step must be a positive number.", Color.RED);
            return;
        }

        // Parse ngày giờ
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime parsedStartTime = LocalTime.parse(startTime, timeFormatter);
        LocalTime parsedEndTime = LocalTime.parse(endTime, timeFormatter);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, parsedStartTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, parsedEndTime);

        if (endDateTime.isBefore(startDateTime) || endDateTime.equals(startDateTime)) {
            showMessage("End time must be after the start time.", Color.RED);
            return;
        }

        String selectedCategory = selectedToggle.getUserData().toString();

        // Tạo Payload
        ItemPayload payload = new ItemPayload(
                itemName, selectedCategory, itemDesc,
                null, // Hiện tại FXML chưa gắn nút add ảnh thật, gửi mảng rỗng
                startDateTime, endDateTime, initPrice, bidStep, currentItemId
        );

        String jsonPayload = gson.toJson(payload);

        // Gọi HTTP API từ ItemsService
        itemsService.updateItem(jsonPayload, this.currentItemId)
                .thenAccept((ResponseMessage responseMessage) -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        showMessage("Item updated successfully!", Color.GREEN);
                        PauseTransition pause = new PauseTransition(Duration.seconds(1));
                        pause.setOnFinished(e -> handleClose());
                        pause.play();
                    } else {
                        showMessage(responseMessage.getMessage(), Color.RED);
                    }
                })
                .exceptionally((Throwable e) -> {
                    e.printStackTrace();
                    showMessage("Failed to connect to server: " + e.getMessage(), Color.RED);
                    return null;
                });
    }

    @FXML
    public void handleClose() {
        NavigationUtil.handleSwitchToSetting(lblMessage, "myAuctions");
    }

    // --- HELPER METHODS ---
    private void showMessage(String message, Color color) {
        Platform.runLater(() -> {
            lblMessage.setTextFill(color);
            lblMessage.setText(message);
        });
    }

    private boolean isAnyNull(Object... items) {
        for (Object item : items) {
            if (item == null || (item instanceof String && ((String) item).isEmpty())) {
                return true;
            }
        }
        return false;
    }

    private Double convertNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(str);
            return (d <= 0) ? -2.0 : d;
        } catch (NumberFormatException e) {
            return -2.0;
        }
    }
}