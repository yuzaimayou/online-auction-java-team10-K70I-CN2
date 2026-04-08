package com.auction.client.controller;

import com.auction.shared.model.product.Item;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ProductPageController {
    private Item item;

    public void initData(Item item) {
        this.item = item;
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
            System.err.println("Không tìm thấy file AuctionFormPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}
