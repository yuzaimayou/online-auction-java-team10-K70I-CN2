package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.item.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.format.DateTimeFormatter;

public class ItemDetailPageController {

    @FXML
    private Label lblItemName;
    @FXML
    private Label lblCategory;
    @FXML
    private Label lblCurrentPrice;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblEndTime;
    @FXML
    private ImageView mainImageView;
    @FXML
    private FlowPane imageGallery;

    private final ItemsService itemsService = ItemsService.getInstance();
    private final Gson gson = new GsonUtil().getInstance();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {

    }

    /**
     * Hàm này được gọi từ MyAuctionsController để truyền ID sang
     * @param itemId ID của sản phẩm cần xem chi tiết
     */
    public void loadItemData(String itemId) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getHttpUrl() + "/api/items/" + itemId))
                .GET()
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    ResponseMessage response =
                            gson.fromJson(responseBody, ResponseMessage.class);
                    if ("success".equals(response.getStatus())) {
                        Item fullItem =
                                gson.fromJson(response.getData(), Item.class);
                        Platform.runLater(() ->
                                displayItemDetails(fullItem)
                        );
                    } else {
                        System.err.println("Load item failed: "
                                + response.getMessage());
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private void displayItemDetails(Item item) {
        lblItemName.setText(item.getName());
        lblCategory.setText(item.getCategory());
        lblCurrentPrice.setText(String.format("$%,.2f", item.getCurrentPrice()));
        lblDescription.setText(item.getDescription());

        if (item.getEndTime() != null) {
            lblEndTime.setText("Kết thúc lúc: " + item.getEndTime().format(formatter));
        }

        if (item.getImagesPath() != null && !item.getImagesPath().isEmpty()) {
            ClientImageUtil.displayImage(
                    item.getImagesPath().get(0),
                    "images",
                    mainImageView,
                    400, 400
            );

            imageGallery.getChildren().clear();
            for (String path : item.getImagesPath()) {
                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(80);
                thumbView.setFitHeight(80);
                thumbView.setPreserveRatio(true);
                thumbView.getStyleClass().add("thumbnail-item");

                ClientImageUtil.displayImage(path, "images", thumbView, 80, 80);
                imageGallery.getChildren().add(thumbView);
            }
        }
    }

    @FXML
    private void handleBack() {
    }
}