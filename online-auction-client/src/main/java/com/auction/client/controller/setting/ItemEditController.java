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
import com.google.gson.JsonElement;
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

    // Header
    @FXML
    private Label lblItemTitle;
    @FXML
    private Label lblItemCategory;
    @FXML
    private Label lblStatusBadge;
    @FXML
    private Label lblAuctionTime;

    // image
    @FXML
    private ImageView mainImageView;
    @FXML
    private HBox imageContainer;

    // ô nhập
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

    // Time
    @FXML
    private DatePicker startDateP;
    @FXML
    private ComboBox<String> cbStartTime;
    @FXML
    private DatePicker endDateP;
    @FXML
    private ComboBox<String> cbEndTime;

    @FXML
    private Label lblMessage;

    private String currentItemId;
    private Item currentItem;
    private final ItemsService itemsService = ItemsService.getInstance();
    private final Gson gson = GsonUtil.getInstance();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTimeComboBoxes();
        setupAutoGrowTextArea();
        setupDynamicListeners();
    }

    private void setupDynamicListeners() {
        txtItemName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblItemTitle != null) lblItemTitle.setText(newVal);
        });

        if (categoryGroup != null) {
            categoryGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && lblItemCategory != null) {
                    lblItemCategory.setText(newVal.getUserData().toString());
                }
            });
        }
    }

    public void setItemId(String id) {
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
                        JsonElement jsonElement = gson.toJsonTree(response.getData());
                        Item item = gson.fromJson(jsonElement, Item.class);

                        this.currentItem = item;
                        Platform.runLater(() -> setEditData(item));
                    }
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void setEditData(Item item) {
        if (item == null) return;

        // Thông tin text
        txtItemName.setText(item.getName());
        lblItemTitle.setText(item.getName());
        txtItemDesc.setText(item.getDescription());
        txtInitPrice.setText(String.format("%.0f", item.getStartingPrice()));
        txtBidStep.setText(String.format("%.0f", item.getBidStep()));
        lblStatusBadge.setText(item.getStatus().toString());

        if (lblItemCategory != null) {
            lblItemCategory.setText(item.getCategory());
        }
        updateCategorySelection(item.getCategory());

        // Thời gian
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        if (item.getStartTime() != null) {
            startDateP.setValue(item.getStartTime().toLocalDate());
            cbStartTime.setValue(item.getStartTime().toLocalTime().format(timeFormatter));
            lblAuctionTime.setText("Auction starts at: " + item.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        if (item.getEndTime() != null) {
            endDateP.setValue(item.getEndTime().toLocalDate());
            cbEndTime.setValue(item.getEndTime().toLocalTime().format(timeFormatter));
        }
        displayImages(item);
    }

    @FXML
    public void handleSaveChanges(ActionEvent event) {
        try {
            String name = txtItemName.getText().trim();
            String desc = txtItemDesc.getText().trim();
            Toggle selectedCat = categoryGroup.getSelectedToggle();

            if (isAnyNull(name, desc, selectedCat, startDateP.getValue(), cbStartTime.getValue(), endDateP.getValue(), cbEndTime.getValue())) {
                showMessage("Vui lòng điền đầy đủ các thông tin bắt buộc.", Color.RED);
                return;
            }

            LocalDateTime start = LocalDateTime.of(startDateP.getValue(), LocalTime.parse(cbStartTime.getValue()));
            LocalDateTime end = LocalDateTime.of(endDateP.getValue(), LocalTime.parse(cbEndTime.getValue()));
            LocalDateTime now = LocalDateTime.now();

            if (!start.isBefore(end)) {
                showMessage("Thời gian kết thúc (End Time) phải sau thời gian bắt đầu.", Color.RED);
                return;
            }
            if (end.isBefore(now)) {
                showMessage("Thời gian kết thúc không thể ở trong quá khứ.", Color.RED);
                return;
            }

            double initPrice = Double.parseDouble(txtInitPrice.getText());
            double bidStep = Double.parseDouble(txtBidStep.getText());

            if (initPrice <= 0 || bidStep <= 0) {
                showMessage("Giá khởi điểm và Bước giá phải lớn hơn 0.", Color.RED);
                return;
            }

            // 4. Đóng gói Payload
            ItemPayload payload = new ItemPayload(
                    name,
                    selectedCat.getUserData().toString(),
                    desc,
                    new java.util.ArrayList<String[]>(),
                    start,
                    end,
                    initPrice,
                    bidStep,
                    currentItemId
            );

            // 5. Khóa nút Save và Hiển thị trạng thái đang xử lý
            Button btnSave = (Button) event.getSource();
            btnSave.setDisable(true);
            showMessage("Đang lưu thay đổi...", Color.BLUE);

            // 6. Gửi request cập nhật lên Server
            itemsService.updateItem(gson.toJson(payload), currentItemId)
                    .thenAccept(res -> {
                        Platform.runLater(() -> {
                            btnSave.setDisable(false); // Mở khóa nút
                            if ("success".equals(res.getStatus())) {
                                showMessage("Cập nhật thành công!", Color.GREEN);
                                // Tự động thoát về MyAuctionsPage sau 1 giây
                                new PauseTransition(Duration.seconds(1)).setOnFinished(e -> handleClose());
                            } else {
                                showMessage("Lỗi từ server: " + res.getMessage(), Color.RED);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            btnSave.setDisable(false);
                            showMessage("Lỗi kết nối đến máy chủ. Vui lòng thử lại.", Color.RED);
                        });
                        return null;
                    });

        } catch (NumberFormatException e) {
            showMessage("Giá tiền và Bước giá phải là số hợp lệ.", Color.RED);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.", Color.RED);
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        handleClose();
    }

    @FXML
    public void handleClose() {
        NavigationUtil.handleSwitchToSetting(lblMessage, "myAuctions");
    }

    // các hàm helper khác
    private void updateCategorySelection(String categoryName) {
        if (categoryName == null || categoryGroup == null) return;

        for (Toggle toggle : categoryGroup.getToggles()) {
            if (toggle instanceof ToggleButton) {
                String userData = (String) toggle.getUserData();
                if (categoryName.equalsIgnoreCase(userData)) {
                    categoryGroup.selectToggle(toggle);
                    break;
                }
            }
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
                thumbView.setPreserveRatio(false);

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