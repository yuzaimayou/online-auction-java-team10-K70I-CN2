package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.model.payloads.ProductPayload;
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
    private TextField txtProductName;
    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private DatePicker startDateP;
    @FXML
    private DatePicker endDateP;
    @FXML
    private ImageView imageViewProduct;
    @FXML
    private ComboBox<String> cbStartTime;
    @FXML
    private ComboBox<String> cbEndTime;
    @FXML
    private TextArea txtProductDesc;
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
    private File selectedImageFile;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private final ItemsService itemsService = ItemsService.getInstance();


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

        // xuống dòng cho product desciption
        txtProductDesc.setWrapText(true);
        txtProductDesc.textProperty().addListener((observable, oldValue, newValue) -> {
            javafx.scene.text.Text helper = new javafx.scene.text.Text();
            helper.setText(newValue);
            helper.setFont(txtProductDesc.getFont());

            helper.setWrappingWidth(txtProductDesc.getWidth() - 40);

            double textHeight = helper.getLayoutBounds().getHeight();
            double newHeight = textHeight + 40;

            txtProductDesc.setPrefHeight(Math.max(80, newHeight));
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
    public void handleAddProduct(ActionEvent event) {

        String productName = txtProductName.getText().trim();
        String productDesc = txtProductDesc.getText().trim();
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
        if (isAnyNull(productName, productDesc, selectedToggle, startDate, endDate, startTime, endTime, initPrice, bidStep)
                || selectedFiles.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Please fill in all required fields.");
            return;
        }
        if (initPrice == -2 || bidStep == -2) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Price must be a positive number.");
            return;
        }
        //Xu ly thoi gian
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime parsedStartTime = LocalTime.parse(startTime, timeFormatter);
        LocalTime parsedEndTime = LocalTime.parse(endTime, timeFormatter);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, parsedStartTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, parsedEndTime);
        if (endDateTime.isBefore(startDateTime) || endDateTime.equals(startDateTime)) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("End time must be after the start time.");
            return;
        }
        //Xu ly hinh anh
        try {
            String[] imageConverted = ImageUtil.convertImgToBase64(selectedFiles.get(0));

            if (imageConverted == null) {
                lblMessage.setTextFill(Color.RED);
                lblMessage.setText("Image processing failed.");
                return;
            }

            //Xu ly phan loai san pham
            selectedCategory = selectedToggle.getUserData().toString();


            ProductPayload payload = new ProductPayload(productName, selectedCategory, productDesc, imageConverted, startDateTime, endDateTime, initPrice, bidStep, userId);
            String jsonPayload = gson.toJson(payload);
            String httpUrl = String.format("%s/api/add-product", AppConfig.getHttpUrl());
            System.out.println("Debug: Sending POST request to " + httpUrl);

            HttpClient httpClient = HttpClient.newHttpClient();
            itemsService.createItem(jsonPayload)
                    .thenAccept(responseMessage -> {
                        if ("success".equals(responseMessage.getStatus())) {
                            Platform.runLater(() -> {
                                lblMessage.setTextFill(Color.GREEN);
                                lblMessage.setText(responseMessage.getMessage());
                            });
                            PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                            pause.setOnFinished(e -> handleSwitchToHomePage());
                            pause.play();
                        } else {
                            Platform.runLater(() -> {
                                lblMessage.setTextFill(Color.RED);
                                lblMessage.setText(responseMessage.getMessage());
                            });
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            lblMessage.setTextFill(Color.RED);
                            lblMessage.setText("Failed to connect to server");
                        });
                        return null;
                    });

        } catch (IOException e) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Error reading image file!");
            e.printStackTrace();

        }

    }

    public void handleSwitchToHomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
            Parent root = loader.load();

            HomePageController homePageController = loader.getController();
            Scene currentScene = lblMessage.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Homepage");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("The HomePage.fxml file was not found! Please check the path again.");
        }
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
}