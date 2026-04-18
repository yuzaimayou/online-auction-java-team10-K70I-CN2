package com.auction.client.controller;

import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.product.Item;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ProductPageController {
    private Item item;
    @FXML
    private Label productNameLabel;
    @FXML
    private Label productDesLabel;
    @FXML
    private ImageView productImage;
    @FXML
    private Label sellerLabel;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label startPriceLabel;
    @FXML
    private Label bidStepLabel;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label endTimeLabel;

    public void initData(Item item) {
        this.item = item;
        displayDataProduct(item);
    }

    public void initialize() {


    }

    private void displayDataProduct(Item item) {
        ClientImageUtil.displayImage(item.getImagePath(), "images", productImage);
        productDesLabel.setText(item.getDescription());
        productNameLabel.setText(item.getName());
        sellerLabel.setText(item.getSellerId());
        currentPriceLabel.setText(String.format("Current bid: %.0f", item.getCurrentPrice()));
        startPriceLabel.setText(String.valueOf(item.getStartingPrice()));
        bidStepLabel.setText(String.valueOf(item.getBidStep()));
        startTimeLabel.setText(String.valueOf(item.getStartTime()));
        endTimeLabel.setText(String.valueOf(item.getEndTime()));

    }

    @FXML
    private void handleBackToHome(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/HomePage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu Trực tuyến");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot find file AuctionFormPage.fxml! Check link again.");
        }
    }
}
