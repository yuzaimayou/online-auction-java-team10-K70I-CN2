package com.auction.client.controller;

import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    // Biến này sẽ lưu trữ file ảnh mà client chọn
    private File selectedImageFile;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();


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
        if (isAnyNull(productName, productDesc, selectedToggle, startDate, endDate, startTime, endTime, initPrice, bidStep, selectedImageFile)) {
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
            String[] imageConverted = ImageUtil.convertImgToBase64(selectedImageFile);
            if (imageConverted == null) {
                return;
            }

            //Xu ly phan loai san pham
            selectedCategory = selectedToggle.getUserData().toString();


            ProductPayload payload = new ProductPayload(productName, selectedCategory, productDesc, imageConverted, startDateTime, endDateTime, initPrice, bidStep, userId);
            String jsonPayload = gson.toJson(payload);
            String httpUrl = String.format("%s/api/add-product", AppConfig.getHttpUrl());
            System.out.println("Debug: Sending POST request to " + httpUrl);

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("%s/api/add-product", AppConfig.getHttpUrl())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> {
                        ResponseMessage responseMessage = gson.fromJson(responseBody, ResponseMessage.class);

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
                        javafx.application.Platform.runLater(() -> {
                            ;
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

    public void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add image product");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.heic", "*.webp")
        );
        Stage stage = (Stage) btnChooseImage.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            Image image = new Image(selectedImageFile.toURI().toString());
            imageViewProduct.setImage(image);
            String localPath = selectedImageFile.getAbsolutePath();
        }
    }
}
