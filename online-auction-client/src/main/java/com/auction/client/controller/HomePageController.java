package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.stream.Collectors;

public class HomePageController {

    //network
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new GsonUtil().getInstance();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private FlowPane ongoingAuctionsContainer;
    @FXML
    private FlowPane upcomingAuctionsContainer;
    @FXML
    private FlowPane endedAuctionsContainer;
    @FXML
    private VBox ongoingSection;
    @FXML
    private VBox upcomingSection;
    @FXML
    private VBox endedSection;
    @FXML
    private NavBarController navBarController;

    private List<Item> masterItemList;
    private String currentCategory = "ALL";

    @FXML
    public void initialize() {
        SearchStoreController.searchQueryProperty().addListener((obs, oldVal, newVal) -> {
            applyFilter();
        });
        NetworkService.getInstance().leaveRoom();

        System.out.println("Đã vào trang chủ!");

        mainScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue() - 40;
            ongoingAuctionsContainer.setPrefWidth(width);
            upcomingAuctionsContainer.setPrefWidth(width);
            endedAuctionsContainer.setPrefWidth(width);
        });

        getDataItemsAndDisplay();
    }

    @FXML
    private void getDataItemsAndDisplay() {
        System.out.println("Dang tien hanh lay du lieu");


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
                            this.masterItemList = dataItems;
                            applyFilter();
                        });
                    } else {
                        System.out.println(res.getMessage());
                    }
                });
    }

    private void applyFilter() {
        if (masterItemList == null)
            return;

        String query = SearchStoreController.getSearchQuery().toLowerCase().trim();

        List<Item> filtered = masterItemList.stream()
                .filter(item -> {
                    // Lọc theo Category
                    boolean matchesCategory = currentCategory.equals("ALL") ||
                            (item.getCategory() != null && item.getCategory().toString().equalsIgnoreCase(currentCategory));

                    // Lọc theo Search Query
                    boolean matchesSearch = query.isEmpty() ||
                            (item.getName() != null && item.getName().toLowerCase().contains(query));

                    return matchesCategory && matchesSearch;
                })
                .collect(Collectors.toList());

        loadItemsToUI(filtered);
    }

    @FXML
    public void loadItemsToUI(List<Item> itemsFromServer) {
        Platform.runLater(() -> {
            ongoingAuctionsContainer.getChildren().clear();
            upcomingAuctionsContainer.getChildren().clear();
            endedAuctionsContainer.getChildren().clear();

            int ongoingCount = 0;
            int upcomingCount = 0;
            int endedCount = 0;

            for (Item item : itemsFromServer) {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ItemCardHP.fxml"));
                    VBox cardBox = fxmlLoader.load();

                    cardBox.setPrefWidth(280);
                    cardBox.setMinWidth(280);
                    cardBox.setMaxWidth(280);

                    ItemCardHPController cardHPController = fxmlLoader.getController();
                    cardHPController.setData(item);

                    String status = (item.getStatus() != null)
                            ? item.getStatus().toString().toUpperCase() : "";

                    if (status.contains("ONGOING") || status.contains("LIVE")) {
                        ongoingAuctionsContainer.getChildren().add(cardBox);
                        ongoingCount++;
                    } else if (status.contains("UPCOMING")) {
                        upcomingAuctionsContainer.getChildren().add(cardBox);
                        upcomingCount++;
                    } else {
                        endedAuctionsContainer.getChildren().add(cardBox);
                        endedCount++;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            updateSectionVisibility(ongoingCount, upcomingCount, endedCount);
        });
    }

    private void updateSectionVisibility(int ongoing, int upcoming, int ended) {

        ongoingSection.setVisible(ongoing > 0);
        ongoingSection.setManaged(ongoing > 0);

        upcomingSection.setVisible(upcoming > 0);
        upcomingSection.setManaged(upcoming > 0);

        endedSection.setVisible(ended > 0);
        endedSection.setManaged(ended > 0);
    }


    @FXML
    private void handleCategoryClick(MouseEvent event) {
        VBox clicked = (VBox) event.getSource();

        String category = clicked.getId();
        boolean isReset = category.equalsIgnoreCase(currentCategory);

        if (isReset) {
            currentCategory = "ALL";
        } else {
            currentCategory = category;
        }

        clicked.getParent().getChildrenUnmodifiable().forEach(node -> {
            node.getStyleClass().remove("active-category");
        });

        if (!"ALL".equals(currentCategory)) {
            clicked.getStyleClass().add("active-category");
        }

        applyFilter();
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

    public void refreshProducts() {

        System.out.println("Refreshing homepage products...");
        Platform.runLater(() -> {
            ongoingAuctionsContainer.getChildren().clear();
            upcomingAuctionsContainer.getChildren().clear();
            endedAuctionsContainer.getChildren().clear();
        });

        getDataItemsAndDisplay();
    }
    public void refreshNavBarInfo() {
        if (navBarController != null) {
            navBarController.refreshUserInfo();
        } else {
            System.out.println("Cảnh báo: Không kết nối được với NavBarController.");
        }
    }
}

