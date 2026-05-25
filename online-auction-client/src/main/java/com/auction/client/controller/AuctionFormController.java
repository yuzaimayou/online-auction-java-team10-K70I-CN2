package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.util.NavigationUtil;
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
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.auction.client.util.ToastUtil;

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
    private ImageView imageViewItem;
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
    private File selectedImageFile;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private final ItemsService itemsService = ItemsService.getInstance();
    private boolean isSubmitting = false;


    @FXML
    public void initialize() {
        cbStartTime.getItems().clear();
        cbEndTime.getItems().clear();

        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                cbStartTime.getItems().add(time);
                cbEndTime.getItems().add(time);
            }
        }
        cbStartTime.setValue("06:50");
        cbEndTime.setValue("10:37");

        // xuống dòng cho item desciption
        txtItemDesc.setWrapText(true);
        txtItemDesc.textProperty().addListener((observable, oldValue, newValue) -> {
            javafx.scene.text.Text helper = new javafx.scene.text.Text();
            helper.setText(newValue);
            helper.setFont(txtItemDesc.getFont());

            helper.setWrappingWidth(txtItemDesc.getWidth() - 40);

            double textHeight = helper.getLayoutBounds().getHeight();
            double newHeight = textHeight + 40;

            txtItemDesc.setPrefHeight(Math.max(80, newHeight));
        });

    }


    private boolean isAnyNull(Object... items) {
        for (Object item : items) {
            if (item == null) return true;
        }
        return false;
    }

    private Double convertNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(str);
            if (d <= 0) {
                return -2.0;
            }
            return d;
        } catch (NumberFormatException e) {
            return -2.0;
        }
    }


    @FXML
    public void handleAddItem(ActionEvent event) {
        if (isSubmitting) {
            return;
        }

        String itemName = txtItemName.getText().trim();
        String itemDesc = txtItemDesc.getText().trim();
        Toggle selectedToggle = categoryGroup.getSelectedToggle();
        String selectedCategory;
        //time
        LocalDate startDate = startDateP.getValue();
        LocalDate endDate = endDateP.getValue();
        String startTime = cbStartTime.getValue();
        String endTime = cbEndTime.getValue();

        //price
        Double initPrice = convertNumeric(txtInitPrice.getText().trim());
        Double bidStep = convertNumeric(txtBidStep.getText().trim());
        //User id
        String userId = UserSession.getInstance().getLoggedInUser().getId();

        //Kiem tra
        if (isAnyNull(itemName, itemDesc, selectedToggle, startDate, endDate, startTime, endTime, initPrice, bidStep)
                || selectedFiles.isEmpty()) {
            ToastUtil.showInfo(
                    lblMessage.getScene(), "Please fill in all required fields.");
            return;
        }
        if (initPrice == -2 ) {
            ToastUtil.showInfo(
                    lblMessage.getScene(), "Price must be a positive number.");
            return;
        }
        if (bidStep == -2) {
            ToastUtil.showInfo(
                    lblMessage.getScene(), "Bid steps must be a positive number.");
            return;
        }
        if (bidStep > initPrice) {
            ToastUtil.showError(
                    lblMessage.getScene(), "Bid step cannot be greater than the starting price!");
            return;
        }
        //Xu ly thoi gian
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime parsedStartTime = LocalTime.parse(startTime, timeFormatter);
        LocalTime parsedEndTime = LocalTime.parse(endTime, timeFormatter);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, parsedStartTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, parsedEndTime);
        LocalDateTime now = LocalDateTime.now();

        if (startDateTime.isBefore(now)) {
            ToastUtil.showError(
                    lblMessage.getScene(),"Start time cannot be in the past.");
            return;
        }

        if (endDateTime.isBefore(now)) {
            ToastUtil.showError(
                    lblMessage.getScene(),"End time cannot be in the past.");
            return;
        }
        if (endDateTime.isBefore(startDateTime) || endDateTime.equals(startDateTime)) {
            ToastUtil.showError(
                    lblMessage.getScene(),"End time must be strictly after the start time.");
            return;
        }
        //Xu ly phan loai san pham
        selectedCategory = selectedToggle.getUserData().toString();
        //Xu ly hinh anh
        List<String[]> imagesConverted = new ArrayList<>();
        try {
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                for (File file : selectedFiles) {
                    String[] base64 = ImageUtil.convertImgToBase64(file);
                    if (base64 != null) {
                        imagesConverted.add(base64);
                    }
                }
            }
            if (imagesConverted == null) {
                ToastUtil.showInfo(
                        lblMessage.getScene(),"Image processing failed. Please try again.");
                return;
            }
        } catch (IOException e) {
            ToastUtil.showInfo(
                    lblMessage.getScene(),"Error processing images: " + e.getMessage());
            e.printStackTrace();
        }

        ItemPayload payload = new ItemPayload(itemName, selectedCategory, itemDesc, imagesConverted, startDateTime, endDateTime, initPrice, bidStep, userId);
        String jsonPayload = gson.toJson(payload);
        isSubmitting = true;

        Platform.runLater(() -> {
            smallAddBtn.setDisable(true);
            smallAddBtn.setOpacity(0.5);

            btnSubmit.setDisable(true);
            btnSubmit.setText("Creating...");
        });
        itemsService.createItem(jsonPayload)
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        Platform.runLater(() -> {
                            ToastUtil.showSuccess(
                                    lblMessage.getScene(), responseMessage.getMessage());
                        });
                        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));

                        pause.setOnFinished(e ->
                                NavigationUtil.handleSwitchToHomePage(lblMessage)
                        );

                        pause.play();
                    } else {

                        isSubmitting = false;

                        Platform.runLater(() -> {

                            ToastUtil.showInfo(
                                    lblMessage.getScene(),responseMessage.getMessage());

                            smallAddBtn.setDisable(false);
                            smallAddBtn.setOpacity(1);

                            Button clickedButton = (Button) event.getSource();
                            clickedButton.setDisable(false);
                            clickedButton.setText("Add Item");
                        });
                    }
                })
                .exceptionally(e -> {

                    e.printStackTrace();

                    isSubmitting = false;

                    Platform.runLater(() -> {

                        ToastUtil.showInfo(
                                lblMessage.getScene(),"Failed to connect to server. Please submit again");

                        smallAddBtn.setDisable(false);
                        smallAddBtn.setOpacity(1);

                        btnSubmit.setDisable(false);
                        btnSubmit.setText("Add Item");
                    });

                    return null;
                });
    }

    private List<File> selectedFiles = new ArrayList<>();
    private final int MAX_IMAGES = 5;

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

            lblMessage.setText("");
            updateUI();
        }
    }

    private void updateUI() {
        if (selectedFiles.isEmpty()) {
            dragDropArea.setVisible(true);
            dragDropArea.setManaged(true);
            imagesPreviewContainer.setVisible(false);
            imagesPreviewContainer.setManaged(false);
        } else {
            dragDropArea.setVisible(false);
            dragDropArea.setManaged(false);
            imagesPreviewContainer.setVisible(true);
            imagesPreviewContainer.setManaged(true);

            imagesPreviewContainer.getChildren().removeIf(node -> node != smallAddBtn);

            for (File file : selectedFiles) {
                imagesPreviewContainer.getChildren().add(imagesPreviewContainer.getChildren().indexOf(smallAddBtn), createImageCard(file));
            }

            smallAddBtn.setVisible(selectedFiles.size() < MAX_IMAGES);
        }
    }

    private StackPane createImageCard(File file) {
        StackPane card = new StackPane();
        card.setPickOnBounds(false);

        double fixedWidth = 150;
        double fixedHeight = 120;

        VBox imageContainer = new VBox();
        imageContainer.getStyleClass().add("image-border-container");
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setMinWidth(fixedWidth);
        imageContainer.setMaxWidth(fixedWidth);
        imageContainer.setMinHeight(fixedHeight);
        imageContainer.setMaxHeight(fixedHeight);

        ImageView iv = new ImageView();
        Image img = new Image(file.toURI().toString());

        iv.setImage(img);
        iv.setPreserveRatio(true);

        double imgRatio = img.getWidth() / img.getHeight();
        double containerRatio = fixedWidth / fixedHeight;

        if (imgRatio > containerRatio) {
            iv.setFitHeight(fixedHeight);
        } else {
            iv.setFitWidth(fixedWidth);
        }

        Rectangle clip = new Rectangle(fixedWidth, fixedHeight);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageContainer.setClip(clip);

        imageContainer.getChildren().add(iv);

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("delete-photo-btn");

        StackPane.setAlignment(btnDelete, Pos.TOP_RIGHT);

        btnDelete.setOnAction(e -> {
            selectedFiles.remove(file);
            updateUI();
        });

        card.getChildren().addAll(imageContainer, btnDelete);

        return card;
    }
    @FXML
    public void handleSwitchToHomePage(ActionEvent event) {
        NavigationUtil.handleSwitchToHomePage(lblMessage);
    }
}