package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.UserSession;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.model.enums.ActionType;
import com.auction.shared.model.payloads.ProductPayload;
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
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class AuctionFormController {
    @FXML
    private Label txtUser;
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
    private TextField txtMaxPrice;
    @FXML
    private TextField txtMinPrice;

    @FXML
    private Button btnChooseImage;

    // Biến này sẽ lưu trữ file ảnh mà client chọn
    private File selectedImageFile;
    private NetworkService network = NetworkService.getInstance();
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

        txtUser.setText(UserSession.getInstance().getLoggedInUser().getUsername());
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

    private String convertImageToBase64(File selectedImageFile) {
        String base64Image = "";
        String imageExtension = "";
        try {
            String fileName = selectedImageFile.getName();
            imageExtension = fileName.substring(fileName.lastIndexOf("."));
            byte[] fileContent = Files.readAllBytes(selectedImageFile.toPath());
            base64Image = Base64.getEncoder().encodeToString(fileContent);
            return base64Image;
        } catch (IOException e) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Error reading image file!");
            e.printStackTrace();
            return null;
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
        Double maxPrice = convertNumeric(txtMaxPrice.getText().trim());
        Double minPrice = convertNumeric(txtMinPrice.getText().trim());
        //User id
        String userId = UserSession.getInstance().getLoggedInUser().getId();

        //Kiem tra
        if (isAnyNull(productName, productDesc, selectedToggle, startDate, endDate, startTime, endTime, initPrice, bidStep, selectedImageFile)) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Please fill in all required fields.");
            return;
        }
        if (maxPrice == null) {
            maxPrice = 0.0;
        }
        if (minPrice == null) {
            minPrice = 0.0;
        }
        if (initPrice == -2 || bidStep == -2 || maxPrice == -2 || minPrice == -2) {
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
        String imageBase64 = convertImageToBase64(selectedImageFile);
        if (imageBase64 == null) {
            return;
        }
        //Xu ly phan loai san pham
        selectedCategory = selectedToggle.getUserData().toString();


        ProductPayload payload = new ProductPayload(productName, selectedCategory, productDesc, imageBase64, startDateTime, endDateTime, initPrice, bidStep, maxPrice, minPrice, userId);
        String jsonPayload = gson.toJson(payload);
        CompletableFuture.supplyAsync(() -> {
            return network.sendRequest(new RequestMessage(ActionType.ADDPRODUCT, jsonPayload));
        }).thenAccept(res -> {
            if (res == null) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText("Unable to connect to the server. Please try again later.");
                });
                return;
            }
            if ("SUCCESS".equals(res.getStatus())) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.GREEN);
                    lblMessage.setText(res.getMessage());
                });
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(e -> handleSwitchToHomePage());
                pause.play();
            } else {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(Color.RED);
                    lblMessage.setText(res.getMessage());
                });
            }

        });

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
