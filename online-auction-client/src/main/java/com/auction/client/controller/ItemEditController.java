package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.service.ToastService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ItemEditController {

    // ── FXML Fields ──
    @FXML private TextField    txtItemName;
    @FXML private TextArea     txtItemDesc;
    @FXML private ToggleGroup  categoryGroup;
    @FXML private TextField    txtInitPrice;
    @FXML private TextField    txtBidStep;

    @FXML private DatePicker       startDateP;
    @FXML private ComboBox<String> cbStartTime;
    @FXML private DatePicker       endDateP;
    @FXML private ComboBox<String> cbEndTime;

    @FXML private VBox  dragDropArea;
    @FXML private HBox  imagesPreviewContainer;
    @FXML private VBox  smallAddBtn;

    @FXML private Label  lblMessage;
    @FXML private Button btnSave;

    // ── State ──
    private String  currentItemId;
    private boolean isSaving = false;

    private List<String> existingImagePaths = new ArrayList<>();
    private final List<File> newSelectedFiles = new ArrayList<>();

    private static final int MAX_IMAGES = 5;
    private final ItemsService itemsService = ItemsService.getInstance();
    private final Gson         gson         = GsonUtil.getInstance();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private VBox navBar; // Khai báo fx:id của NavBar include nếu có

    @FXML
    public void initialize() {
        // Chạy ẩn đi nếu phát hiện form đang được mở trong một Stage Modal riêng biệt
        Platform.runLater(() -> {
            if (txtItemName.getScene() != null && txtItemName.getScene().getWindow() instanceof Stage stage) {
                // Nếu là cửa sổ Modal popup, ẩn thanh NavBar đi cho gọn gàng
                if (navBar != null) {
                    navBar.setVisible(false);
                    navBar.setManaged(false);
                }
            }
        });

        setupTimeComboBoxes();
        setupAutoGrowTextArea();
    }

    public void setItemId(String id) {
        this.currentItemId = id;
        itemsService.getItemById(id, "")
                .thenAccept(item -> Platform.runLater(() -> populateForm(item)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showMessage("Không thể tải thông tin sản phẩm.", Color.RED));
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

    // ──────────────────────────────────────────────────────────────────────────
    //  Image Rendering (Áp dụng logic của AuctionFormController)
    // ──────────────────────────────────────────────────────────────────────────

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
            imagesPreviewContainer.getChildren().add(buildServerThumb(imgPath));
        }

        for (File file : newSelectedFiles) {
            imagesPreviewContainer.getChildren().add(buildLocalThumb(file));
        }

        if (total < MAX_IMAGES) {
            imagesPreviewContainer.getChildren().add(smallAddBtn);
        }
    }

    private StackPane buildServerThumb(String imgPath) {
        ImageView iv = new ImageView();
        // ClientImageUtil set bất đồng bộ, logic resize sẽ chạy khi image load xong
        ClientImageUtil.displayImage(imgPath, "images", iv, 80, 60);
        return createThumbCard(iv, () -> { existingImagePaths.remove(imgPath); renderImageStrip(); });
    }

    private StackPane buildLocalThumb(File file) {
        ImageView iv = new ImageView(new Image(file.toURI().toString()));
        return createThumbCard(iv, () -> { newSelectedFiles.remove(file); renderImageStrip(); });
    }

    // FIX: Clone chuẩn logic "Object-fit Cover bằng Container Clip" từ AuctionFormController
    private StackPane createThumbCard(ImageView iv, Runnable onDelete) {
        double fixedWidth = 80;
        double fixedHeight = 60;

        // Container cố định kích thước
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("image-border-container");
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPrefSize(fixedWidth, fixedHeight);
        imageContainer.setMinSize(fixedWidth, fixedHeight);
        imageContainer.setMaxSize(fixedWidth, fixedHeight);

        // Bật preserveRatio để chống méo ảnh (Quan trọng)
        iv.setPreserveRatio(true);

        Runnable applyFitCover = () -> {
            Image img = iv.getImage();
            if (img == null) return;

            double imgW = img.getWidth();
            double imgH = img.getHeight();
            if (imgW == 0 || imgH == 0) return;

            double imgRatio = imgW / imgH;
            double containerRatio = fixedWidth / fixedHeight;

            // Thuật toán: Tràn viền theo chiều nào thiếu
            if (imgRatio > containerRatio) {
                iv.setFitHeight(fixedHeight);
                iv.setFitWidth(0); // Reset width để tỉ lệ tự kéo
            } else {
                iv.setFitWidth(fixedWidth);
                iv.setFitHeight(0); // Reset height để tỉ lệ tự kéo
            }
        };

        // Bắt sự kiện khi tải ảnh hoàn tất để tính toán lại
        iv.imageProperty().addListener((obs, oldImg, newImg) -> {
            if (newImg != null) {
                if (newImg.getProgress() < 1.0) {
                    newImg.progressProperty().addListener((o, ov, nv) -> {
                        if (nv.doubleValue() >= 1.0) Platform.runLater(applyFitCover);
                    });
                } else {
                    Platform.runLater(applyFitCover);
                }
            }
        });

        // Áp dụng nếu ảnh (local) đã load sẵn lập tức
        if (iv.getImage() != null && iv.getImage().getProgress() >= 1.0) {
            applyFitCover.run();
        }

        // Cắt bo góc và phần thừa trên chính Container
        Rectangle clip = new Rectangle(fixedWidth, fixedHeight);
        clip.setArcWidth(12); // Tùy chỉnh độ cong góc
        clip.setArcHeight(12);
        imageContainer.setClip(clip);

        imageContainer.getChildren().add(iv);

        // Nút xóa ảnh
        Button btnDel = new Button("✕");
        btnDel.getStyleClass().add("delete-photo-btn");
        StackPane.setAlignment(btnDel, Pos.TOP_RIGHT);
        btnDel.setOnAction(e -> onDelete.run());

        return new StackPane(imageContainer, btnDel);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Event Handlers & API Call
    // ──────────────────────────────────────────────────────────────────────────

    @FXML
    public void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        List<File> files = fc.showOpenMultipleDialog(btnSave.getScene().getWindow());
        if (files == null || files.isEmpty()) return;

        if (existingImagePaths.size() + newSelectedFiles.size() + files.size() > MAX_IMAGES) {
            ToastService.showInfo(lblMessage.getScene(), "Tối đa " + MAX_IMAGES + " ảnh.");
            return;
        }
        newSelectedFiles.addAll(files);
        renderImageStrip();
    }

    @FXML
    public void handleSaveChanges(ActionEvent event) {
        if (isSaving) return;

        String itemName = txtItemName.getText().trim();
        String itemDesc = txtItemDesc.getText().trim();
        Toggle selToggle = categoryGroup.getSelectedToggle();
        LocalDate startDate = startDateP.getValue();
        LocalDate endDate = endDateP.getValue();
        String startTime = cbStartTime.getValue();
        String endTime = cbEndTime.getValue();
        Double initPrice = parsePositive(txtInitPrice.getText());
        Double bidStep = parsePositive(txtBidStep.getText());

        if (isAnyNull(itemName, itemDesc, selToggle, startDate, endDate, startTime, endTime)) {
            ToastService.showInfo(lblMessage.getScene(), "Vui lòng điền đầy đủ thông tin.");
            return;
        }
        if (initPrice == null || initPrice <= 0) {
            ToastService.showInfo(lblMessage.getScene(), "Giá khởi điểm phải là số dương.");
            return;
        }
        if (bidStep == null || bidStep <= 0) {
            ToastService.showInfo(lblMessage.getScene(), "Bước giá phải là số dương.");
            return;
        }
        if (bidStep > initPrice) {
            ToastService.showError(lblMessage.getScene(), "Bước giá không được lớn hơn giá khởi điểm!");
            return;
        }

        LocalDateTime startDT = LocalDateTime.of(startDate, LocalTime.parse(startTime, TIME_FMT));
        LocalDateTime endDT = LocalDateTime.of(endDate, LocalTime.parse(endTime, TIME_FMT));

        if (!endDT.isAfter(startDT)) {
            ToastService.showError(lblMessage.getScene(), "Thời gian kết thúc phải sau thời gian bắt đầu.");
            return;
        }
        if (existingImagePaths.isEmpty() && newSelectedFiles.isEmpty()) {
            ToastService.showInfo(lblMessage.getScene(), "Vui lòng chọn ít nhất một ảnh.");
            return;
        }

        List<String[]> imagesConverted = new ArrayList<>();
        try {
            for (String oldPath : existingImagePaths) {
                imagesConverted.add(new String[]{oldPath, null});
            }
            for (File f : newSelectedFiles) {
                String[] b64 = com.auction.shared.util.ImageUtil.convertImgToBase64(f);
                if (b64 != null) imagesConverted.add(b64);
            }
        } catch (Exception e) {
            ToastService.showInfo(lblMessage.getScene(), "Lỗi xử lý ảnh: " + e.getMessage());
            return;
        }

        ItemPayload payload = new ItemPayload(
                itemName, selToggle.getUserData().toString(), itemDesc,
                imagesConverted, startDT, endDT,
                initPrice, bidStep, currentItemId
        );

        isSaving = true;
        btnSave.setDisable(true);
        btnSave.setText("Đang lưu...");

        itemsService.updateItem(gson.toJson(payload), currentItemId)
                .thenAccept(res -> Platform.runLater(() -> {
                    btnSave.setDisable(false);
                    btnSave.setText("Save Changes");
                    isSaving = false;

                    if ("success".equals(res.getStatus())) {
                        ToastService.showSuccess(lblMessage.getScene(), "Cập nhật thành công!");
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.2));
                        delay.setOnFinished(e -> navigateToMyAuctions());
                        delay.play();
                    } else {
                        ToastService.showError(lblMessage.getScene(), "Lỗi server: " + res.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnSave.setDisable(false);
                        btnSave.setText("Save Changes");
                        isSaving = false;
                        ToastService.showError(lblMessage.getScene(), "Lỗi kết nối. Vui lòng thử lại.");
                    });
                    return null;
                });
    }

    @FXML public void handleCancel(ActionEvent event) { navigateToMyAuctions(); }
    @FXML
    public void handleCloseAction(ActionEvent event){
        Button btn=(Button)event.getSource();
        StackPane overlay =
                (StackPane)
                        btn.getScene()
                                .lookup(".popup-overlay");
        if(overlay!=null){
            ((StackPane)overlay.getParent())
                    .getChildren()
                    .remove(overlay);
        }
    }

    private void navigateToMyAuctions() {
        NavigationUtil.handleSwitchToSetting(lblMessage, "myAuctions");
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void setupTimeComboBoxes() {
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                cbStartTime.getItems().add(String.format("%02d:%02d", h, m));
                cbEndTime.getItems().add(String.format("%02d:%02d", h, m));
            }
        }
    }

    private void setupAutoGrowTextArea() {
        txtItemDesc.setWrapText(true);
        txtItemDesc.textProperty().addListener((obs, oldVal, newVal) -> {
            javafx.scene.text.Text helper = new javafx.scene.text.Text(newVal);
            helper.setFont(txtItemDesc.getFont());
            helper.setWrappingWidth(txtItemDesc.getWidth() - 40);
            txtItemDesc.setPrefHeight(Math.max(120, helper.getLayoutBounds().getHeight() + 40));
        });
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

    private String snapToSlot(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute() >= 30 ? 30 : 0);
    }

    private boolean isAnyNull(Object... items) {
        for (Object o : items) {
            if (o == null || (o instanceof String s && s.isBlank())) return true;
        }
        return false;
    }

    private Double parsePositive(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showMessage(String msg, Color color) {
        lblMessage.setTextFill(color);
        lblMessage.setText(msg);
    }
}