package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.service.ItemsService;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.client.service.ToastService;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
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
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.List;

public class HomePageController {

    //network
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new GsonUtil().getInstance();
    private final ItemsService itemsService =
            ItemsService.getInstance();
    private String currentCategory = "ALL";
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

    @FXML
    public void initialize() {
        SearchStoreController.searchQueryProperty().addListener((obs, oldVal, newVal) -> {
            fetchItemsFromServer();});
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
        fetchItemsFromServer();
    }
    private void fetchItemsFromServer() {
        String search = SearchStoreController.getSearchQuery();
        itemsService.getItems(search, currentCategory)
                .thenAccept(items -> {
                    Platform.runLater(() -> {loadItemsToUI(items);});
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        ToastService.showError(
                                mainScrollPane.getScene(), "Không thể tải danh sách sản phẩm!");
                    });
                    e.printStackTrace();
                    return null;
                });
    }
    private AuctionStatus resolveRealtimeStatus(ItemSummary item) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(item.getStartTime())) {
            return AuctionStatus.UPCOMING;
        }
        if (now.isAfter(item.getEndTime())) {
            return AuctionStatus.ENDED;
        }
        return AuctionStatus.ONGOING;
    }

    public void loadItemsToUI(List<ItemSummary> itemsFromServer) {
        Platform.runLater(() -> {
            ongoingAuctionsContainer.getChildren().clear();
            upcomingAuctionsContainer.getChildren().clear();
            endedAuctionsContainer.getChildren().clear();

            int ongoingCount = 0;
            int upcomingCount = 0;
            int endedCount = 0;

            for (ItemSummary item : itemsFromServer) {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ItemCardHP.fxml"));
                    VBox cardBox = fxmlLoader.load();

                    cardBox.setPrefWidth(280);
                    cardBox.setMinWidth(280);
                    cardBox.setMaxWidth(280);

                    ItemCardHPController cardHPController = fxmlLoader.getController();
                    cardHPController.setData(item);

                    AuctionStatus status = resolveRealtimeStatus(item);

                    if (status == AuctionStatus.ONGOING) {
                        ongoingAuctionsContainer.getChildren().add(cardBox);
                        ongoingCount++;
                    } else if (status == AuctionStatus.UPCOMING) {
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

    // event handlers
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
        fetchItemsFromServer();
    }

    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/AuctionFormPage.fxml"));
            Parent root = loader.load();
            Node sourceNode = (Node) event.getSource();

            //AuctionFormController auctionFormController = loader.getController();

            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Add Item");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file AuctionFormPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }

    // utilities
    public void refreshItems() {
        System.out.println("Refreshing homepage items...");
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
            System.out.println("Cảnh báo: Không kết nối được với NavBarController..");
        }
    }
}

