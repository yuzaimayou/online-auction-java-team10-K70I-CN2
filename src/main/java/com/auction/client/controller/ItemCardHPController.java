package com.auction.client.controller;

import com.auction.shared.model.product.Item;
import com.auction.shared.util.ImageUtil;
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

public class ItemCardHPController {
    @FXML
    private ImageView productImage;
    @FXML
    private Label productNameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label endTimeLabel;
    @FXML
    private Label priceLabel;

    public void setData(Item item) {
        productNameLabel.setText(item.getName());
        priceLabel.setText(String.valueOf(item.getStartingPrice()));
        endTimeLabel.setText(String.valueOf(item.getEndTime()));
        ImageUtil.displayImage(item.getImagePath(), "images", productImage);
    }

    @FXML
    public void handleSwitchToProductPage(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ProductPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu Trực tuyến ");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file ProductPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}
