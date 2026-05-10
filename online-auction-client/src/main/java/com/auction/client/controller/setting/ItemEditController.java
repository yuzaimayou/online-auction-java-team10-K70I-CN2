package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ItemEditController {

    @FXML private ImageView mainImageView;
    @FXML private HBox imageContainer;
    @FXML private Label lblItemTitle;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblAuctionTime;
    @FXML private Label lblAlert;

    @FXML private TextField txtItemName;
    @FXML private ToggleGroup categoryGroup;
    @FXML private TextArea txtItemDesc;

    @FXML private TextField txtInitPrice;
    @FXML private TextField txtBidStep;

    @FXML private DatePicker startDateP;
    @FXML private ComboBox<String> cbStartTime;
    @FXML private DatePicker endDateP;
    @FXML private ComboBox<String> cbEndTime;

    @FXML private Label lblMessage;

    private String currentItemId;
    private Item currentItem;
    private final ItemsService itemsService = ItemsService.getInstance();
    private final Gson gson = GsonUtil.getInstance();

    @FXML
    public void initialize() {
        setupTimeComboBoxes();
        setupAutoGrowTextArea();

        // Listener để cập nhật tiêu đề Header khi người dùng gõ tên mới
        txtItemName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblItemTitle != null) lblItemTitle.setText(newVal);
        });
    }

    /**
     * Quan trọng: Gọi hàm này từ Controller trang danh sách sản phẩm
     */
    public void setItemId(String id) {
        if (id == null || id.isEmpty()) return;
        this.currentItemId = id;

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s", AppConfig.getHttpUrl(), id)))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    ResponseMessage response = gson.fromJson(responseBody, ResponseMessage.class);
                    if ("success".equals(response.getStatus()) && response.getData() != null) {
                        Item item = gson.fromJson(response.getData(), Item.class);
                        // Đẩy việc cập nhật UI lên JavaFX Thread
                        Platform.runLater(() -> setEditData(item));
                    }
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    showMessage("Error loading item data.", Color.RED);
                    return null;
                });
    }

    public void setEditData(Item item) {
        if (item == null) return;
        this.currentItem = item;
        this.currentItemId = item.getId();

        // 1. Header & TextFields
        lblItemTitle.setText(item.getName());
        lblStatusBadge.setText(item.getStatus().toString());
        txtItemName.setText(item.getName());
        txtItemDesc.setText(item.getDescription());
        txtInitPrice.setText(String.format("%.0f", item.getStartingPrice()));
        txtBidStep.setText(String.format("%.0f", item.getBidStep()));

        // 2. Images (Giống ItemPage)
        displayImages(item);

        // 3. Category
        if (item.getCategory() != null) {
            for (Toggle toggle : categoryGroup.getToggles()) {
                if (toggle.getUserData() != null &&
                        toggle.getUserData().toString().equalsIgnoreCase(item.getCategory())) {
                    categoryGroup.selectToggle(toggle);
                    break;
                }
            }
        }

        // 4. Time
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        if (item.getStartTime() != null) {
            startDateP.setValue(item.getStartTime().toLocalDate());
            cbStartTime.setValue(item.getStartTime().toLocalTime().format(timeFormatter));
            lblAuctionTime.setText("Starts at: " + item.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        if (item.getEndTime() != null) {
            endDateP.setValue(item.getEndTime().toLocalDate());
            cbEndTime.setValue(item.getEndTime().toLocalTime().format(timeFormatter));
        }
    }

    private void displayImages(Item item) {
        imageContainer.getChildren().clear();
        List<String> images = item.getImagesPath();

        if (images != null && !images.isEmpty()) {
            ClientImageUtil.displayImage(images.get(0), "images", mainImageView, 320, 200);

            for (String imgPath : images) {
                StackPane thumbPane = new StackPane();
                thumbPane.getStyleClass().add("img-thumbnail");

                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(80);
                thumbView.setFitHeight(60);
                thumbView.setPreserveRatio(false); // Đảm bảo fill đầy khung thumbnail

                ClientImageUtil.displayImage(imgPath, "images", thumbView, 80, 60);

                thumbPane.getChildren().add(thumbView);
                thumbPane.setOnMouseClicked(e -> {
                    mainImageView.setImage(thumbView.getImage());
                    imageContainer.getChildren().forEach(n -> n.getStyleClass().remove("active-thumbnail"));
                    thumbPane.getStyleClass().add("active-thumbnail");
                });

                imageContainer.getChildren().add(thumbPane);
            }
        }
    }

    @FXML
    public void handleSaveChanges(ActionEvent event) {
        try {
            String name = txtItemName.getText().trim();
            String desc = txtItemDesc.getText().trim();
            Toggle selectedCat = categoryGroup.getSelectedToggle();

            if (isAnyNull(name, desc, selectedCat, startDateP.getValue(), cbStartTime.getValue())) {
                showMessage("Please fill all required fields.", Color.RED);
                return;
            }

            LocalDateTime start = LocalDateTime.of(startDateP.getValue(), LocalTime.parse(cbStartTime.getValue()));
            LocalDateTime end = LocalDateTime.of(endDateP.getValue(), LocalTime.parse(cbEndTime.getValue()));

            ItemPayload payload = new ItemPayload(
                    name,
                    selectedCat.getUserData().toString(),
                    desc,
                    null, // Đã sửa: Truyền lại list ảnh của sản phẩm
                    start,
                    end,
                    Double.parseDouble(txtInitPrice.getText()),
                    Double.parseDouble(txtBidStep.getText()),
                    currentItemId
            );

            itemsService.updateItem(gson.toJson(payload), currentItemId)
                    .thenAccept(res -> {
                        if ("success".equals(res.getStatus())) {
                            showMessage("Update Successful!", Color.GREEN);
                            new PauseTransition(Duration.seconds(1)).setOnFinished(e -> handleClose());
                        } else {
                            showMessage(res.getMessage(), Color.RED);
                        }
                    });
        } catch (Exception e) {
            showMessage("Invalid input. Please check prices and dates.", Color.RED);
        }
    }

    // --- Giữ nguyên các hàm helper khác ---
    @FXML public void handleCancel(ActionEvent event) { handleClose(); }
    @FXML public void handleClose() { NavigationUtil.handleSwitchToSetting(lblMessage, "myAuctions"); }

    private void setupTimeComboBoxes() {
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                String t = String.format("%02d:%02d", h, m);
                cbStartTime.getItems().add(t);
                cbEndTime.getItems().add(t);
            }
        }
    }

    private void setupAutoGrowTextArea() {
        txtItemDesc.setWrapText(true);
        txtItemDesc.setPrefHeight(Region.USE_COMPUTED_SIZE);
    }

    private void showMessage(String msg, Color color) {
        Platform.runLater(() -> {
            lblMessage.setTextFill(color);
            lblMessage.setText(msg);
        });
    }

    private boolean isAnyNull(Object... objects) {
        for (Object o : objects) if (o == null || o.toString().isEmpty()) return true;
        return false;
    }
}