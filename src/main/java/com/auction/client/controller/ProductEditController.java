package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.UserSession;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.model.enums.ActionType;
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
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class ProductEditController {
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
        private String currentProductId; // ID của sản phẩm đang sửa
        private final List<String> listImagesBase64 = new ArrayList<>(); // Danh sách chuỗi ảnh Base64
        private final NetworkService network = NetworkService.getInstance();
        private final Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        @FXML
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

    }
