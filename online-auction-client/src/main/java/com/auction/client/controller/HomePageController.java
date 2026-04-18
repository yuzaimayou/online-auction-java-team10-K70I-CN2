package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;

public class HomePageController {

    //network
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new GsonUtil().getInstance();
    @FXML
    private FlowPane auctionsListContainer;


    @FXML
    public void initialize() {

        System.out.println("Đã vào trang chủ!");

        getDataItemsAndDisplay();
    }

    @FXML
    private void getDataItemsAndDisplay() {
        System.out.println("Dang tien hanh lay du lieu");

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/products", AppConfig.getHttpUrl())))
                .GET()
                .build();
        httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(java.net.http.HttpResponse::body)
                .thenAccept(responseBody -> {
                    ResponseMessage res = gson.fromJson(responseBody, ResponseMessage.class);
                    if ("success".equals(res.getStatus())) {
                        System.out.println(res.getMessage());

                        Type listType = new TypeToken<List<Item>>() {
                        }.getType();
                        List<Item> dataItems = gson.fromJson(res.getData(), listType);
                        javafx.application.Platform.runLater(() -> {
                            loadItemsToUI(dataItems);
                        });
                    } else {
                        System.out.println(res.getMessage());
                    }
                });
//        CompletableFuture.supplyAsync(() -> {
//            return network.sendRequest(new RequestMessage(ActionType.GETDATAPRODUCT));
//        }).thenAccept(res -> {
//            if (res == null) {
//                System.err.println("Unable to connect to the server");
//                return;
//            }
//            if ("SUCCESS".equals(res.getStatus())) {
//                System.out.println(res.getMessage());
//                Type listType = new TypeToken<List<Item>>() {
//                }.getType();
//                List<Item> dataItems = gson.fromJson(res.getData(), listType);
//                Platform.runLater(() -> {
//                    loadItemsToUI(dataItems);
//                });
//            } else {
//                System.out.println(res.getMessage());
//            }
//        });
    }

    @FXML
    public void loadItemsToUI(List<Item> itemsFromServer) {
        auctionsListContainer.getChildren().clear();
        for (Item item : itemsFromServer) {
            try {
                System.out.printf("Load item: %s ", item.getName());
                FXMLLoader fxmlLoader = new FXMLLoader();

                fxmlLoader.setLocation(getClass().getResource("/com.auction.client/fxml/ItemCardHP.fxml"));
                VBox cardBox = fxmlLoader.load();

                ItemCardHPController cardHPController = fxmlLoader.getController();
                cardHPController.setData(item);

                auctionsListContainer.getChildren().add(cardBox);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error while loading data item!");
            }
        }
    }

    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/AuctionFormPage.fxml"));
            Parent root = loader.load();
            Node sourceNode = (Node) event.getSource();

            AuctionFormController auctionFormController = loader.getController();

            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Add Product");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file AuctionFormPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }


}

