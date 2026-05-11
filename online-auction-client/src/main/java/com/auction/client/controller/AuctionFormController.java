package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.ImageUtil;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuctionFormController {
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
    private Button btnChooseImage;
    @FXML
    private VBox dragDropArea;
    @FXML
    private HBox imagesPreviewContainer;
    @FXML
    private VBox smallAddBtn;
    @FXML
    private Button btnSubmit;


    private static final int MAX_IMAGES = 5;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ItemsService itemsService = ItemsService.getInstance();
    private final List<File> selectedFiles = new ArrayList<>();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private boolean isSubmitting = false;


    @FXML
    public void initialize() {
        setupTimeComboBoxes();
        setupAutoResizingTextArea();
        updateUI();
    }
    private void setupTimeComboBoxes() {
        cbStartTime.getItems().clear();
        cbEndTime.getItems().clear();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                cbStartTime.getItems().add(time);
                cbEndTime.getItems().add(time);
            }
        }
        cbStartTime.setValue("08:00");
        cbEndTime.setValue("17:00");
    }
    private void setupAutoResizingTextArea() {
        txtItemDesc.setWrapText(true);
        // Thay vì tính toán thủ công phức tạp, ta sử dụng thuộc tính tự co giãn cơ bản
        txtItemDesc.setPrefRowCount(5);
    }
    @FXML
    public void handleAddItem(ActionEvent event) {
        if (isSubmitting) return;

        try {
            // 1. Thu thập và Validate dữ liệu
            ItemPayload payload = validateAndBuildPayload();
            if (payload == null) return;

            // 2. Chuyển đổi trạng thái UI (Loading)
            setFormDisabled(true);
            isSubmitting = true;
            showMessage("Processing your item...", Color.BLUE);

            // 3. Gửi API
            itemsService.createItem(gson.toJson(payload))
                    .thenAccept(res -> Platform.runLater(() -> {
                        if ("success".equals(res.getStatus())) {
                            showMessage(res.getMessage(), Color.GREEN);
                            new PauseTransition(Duration.seconds(1)).setOnFinished(e -> handleSwitchToHomePage());
                        } else {
                            handleFailure(res.getMessage());
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> handleFailure("Connection failed: " + ex.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            handleFailure("An unexpected error occurred.");
            e.printStackTrace();
        }
    }
    private ItemPayload validateAndBuildPayload() throws IOException {
        String name = txtItemName.getText().trim();
        String desc = txtItemDesc.getText().trim();
        Toggle cat = categoryGroup.getSelectedToggle();

        Double initPrice = parseNumeric(txtInitPrice.getText());
        Double bidStep = parseNumeric(txtBidStep.getText());

        // Kiểm tra trống
        if (isAnyEmpty(name, desc, cat, startDateP.getValue(), endDateP.getValue(),
                cbStartTime.getValue(), cbEndTime.getValue()) || selectedFiles.isEmpty()) {
            showMessage("Please fill in all fields and upload at least one image.", Color.RED);
            return null;
        }

        // Kiểm tra giá
        if (initPrice <= 0 || bidStep <= 0) {
            showMessage("Prices must be greater than zero.", Color.RED);
            return null;
        }

        // Xử lý thời gian
        LocalDateTime start = LocalDateTime.of(startDateP.getValue(), LocalTime.parse(cbStartTime.getValue(), TIME_FORMAT));
        LocalDateTime end = LocalDateTime.of(endDateP.getValue(), LocalTime.parse(cbEndTime.getValue(), TIME_FORMAT));
        LocalDateTime now = LocalDateTime.now();

        if (start.isBefore(now)) {
            showMessage("Start time cannot be in the past.", Color.RED);
            return null;
        }
        if (!end.isAfter(start)) {
            showMessage("End time must be after start time.", Color.RED);
            return null;
        }

        // Chuyển đổi ảnh sang Base64
        List<String[]> imagesBase64 = new ArrayList<>();
        for (File file : selectedFiles) {
            String[] b64 = ImageUtil.convertImgToBase64(file);
            if (b64 != null) imagesBase64.add(b64);
        }

        return new ItemPayload(name, cat.getUserData().toString(), desc, imagesBase64,
                start, end, initPrice, bidStep, UserSession.getInstance().getLoggedInUser().getId());
    }
    public void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        List<File> files = fileChooser.showOpenMultipleDialog(dragDropArea.getScene().getWindow());

        if (files != null) {
            if (selectedFiles.size() + files.size() > MAX_IMAGES) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText("Only upload max " + MAX_IMAGES + " images");
                });
                return;
            }

            selectedFiles.addAll(files);
            updateUI();
        }
    }
    private void updateUI() {
        boolean hasImages = !selectedFiles.isEmpty();
        dragDropArea.setVisible(!hasImages);
        dragDropArea.setManaged(!hasImages);
        imagesPreviewContainer.setVisible(hasImages);
        imagesPreviewContainer.setManaged(hasImages);

        imagesPreviewContainer.getChildren().removeIf(node -> node != smallAddBtn);
        for (File file : selectedFiles) {
            imagesPreviewContainer.getChildren().add(imagesPreviewContainer.getChildren().indexOf(smallAddBtn), createImageCard(file));
        }
        smallAddBtn.setVisible(selectedFiles.size() < MAX_IMAGES);
    }
    private StackPane createImageCard(File file) {
        StackPane card = new StackPane();
        ImageView iv = new ImageView(new Image(file.toURI().toString()));
        iv.setFitWidth(150);
        iv.setFitHeight(120);
        iv.setPreserveRatio(true);

        VBox imgWrapper = new VBox(iv);
        imgWrapper.getStyleClass().add("image-border-container");
        imgWrapper.setAlignment(Pos.CENTER);
        imgWrapper.setPrefSize(150, 120);

        Rectangle clip = new Rectangle(150, 120);
        clip.setArcWidth(20); clip.setArcHeight(20);
        imgWrapper.setClip(clip);

        Button btnDel = new Button("✕");
        btnDel.getStyleClass().add("delete-photo-btn");
        btnDel.setOnAction(e -> {
            selectedFiles.remove(file);
            updateUI();
        });

        StackPane.setAlignment(btnDel, Pos.TOP_RIGHT);
        card.getChildren().addAll(imgWrapper, btnDel);
        return card;
    }

    private void handleFailure(String message) {
        isSubmitting = false;
        setFormDisabled(false);
        showMessage(message, Color.RED);
    }

    private void setFormDisabled(boolean disabled) {
        // Khóa toàn bộ các controls chính
        btnSubmit.setDisable(disabled);
        btnSubmit.setText(disabled ? "Creating..." : "Add Item");
        btnChooseImage.setDisable(disabled);
        txtItemName.setDisable(disabled);
        txtItemDesc.setDisable(disabled);
    }

    private void showMessage(String msg, Color color) {
        lblMessage.setTextFill(color);
        lblMessage.setText(msg);
    }

    private Double parseNumeric(String str) {
        try { return Double.parseDouble(str); }
        catch (Exception e) { return -1.0; }
    }

    private boolean isAnyEmpty(Object... vals) {
        for (Object v : vals) if (v == null || v.toString().trim().isEmpty()) return true;
        return false;
    }

    public void handleSwitchToHomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
            Parent root = loader.load();

            HomePageController homePageController = loader.getController();
            Scene currentScene = lblMessage.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle(String.format("%s - Homepage", AppConfig.getAppName()));

            PauseTransition refreshDelay =
                    new PauseTransition(Duration.seconds(0.5));
            refreshDelay.setOnFinished(event -> {
                homePageController.refreshItems();
            });
            refreshDelay.play();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("The HomePage.fxml file was not found! Please check the path again.");
        }
    }

}