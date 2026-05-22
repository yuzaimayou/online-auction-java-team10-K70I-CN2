package com.auction.client.controller;

import com.auction.client.controller.common.NavBarController;
import com.auction.client.controller.common.SearchStoreController;
import com.auction.client.service.ItemsService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.ToastService;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class HomePageController {

    private final NetworkService network      = NetworkService.getInstance();
    private final ItemsService   itemsService = ItemsService.getInstance();

    private String currentCategory = "ALL";

    @FXML
    private javafx.scene.control.ScrollPane mainScrollPane;
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
        network.leaveRoom();
        SearchStoreController.searchQueryProperty().addListener(
                (obs, oldVal, newVal) -> fetchItemsFromServer()
        );

        mainScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue() - 40;
            ongoingAuctionsContainer.setPrefWidth(width);
            upcomingAuctionsContainer.setPrefWidth(width);
            endedAuctionsContainer.setPrefWidth(width);
        });

        fetchItemsFromServer();
    }

    private void fetchItemsFromServer() {
        String search = SearchStoreController.getSearchQuery();
        itemsService.getItems(search, currentCategory)
                .thenAccept(items -> Platform.runLater(() -> loadItemsToUI(items)))
                .exceptionally(e -> {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        if (mainScrollPane.getScene() != null) {
                            ToastService.showError(mainScrollPane.getScene(), "Cannot load auction items.");
                        }
                    });
                    return null;
                });
    }

    public void loadItemsToUI(List<ItemSummary> itemsFromServer) {
        ongoingAuctionsContainer.getChildren().clear();
        upcomingAuctionsContainer.getChildren().clear();
        endedAuctionsContainer.getChildren().clear();

        int ongoingCount = 0, upcomingCount = 0, endedCount = 0;

        if (itemsFromServer != null) {
            for (ItemSummary item : itemsFromServer) {
                if (item.getStatus() == AuctionStatus.BANNED) continue;

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com.auction.client/fxml/ItemCardHP.fxml"));
                    VBox cardBox = loader.load();
                    cardBox.setPrefWidth(280);
                    cardBox.setMinWidth(280);
                    cardBox.setMaxWidth(280);

                    ItemCardHPController cardController = loader.getController();
                    cardController.setData(item);
                    AuctionStatus status = AuctionStatus.compute(item.getStartTime(), item.getEndTime());

                    switch (status) {
                        case ONGOING  -> { ongoingAuctionsContainer.getChildren().add(cardBox);  ongoingCount++;  }
                        case UPCOMING -> { upcomingAuctionsContainer.getChildren().add(cardBox); upcomingCount++; }
                        case ENDED    -> { endedAuctionsContainer.getChildren().add(cardBox);    endedCount++;    }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        updateSectionVisibility(ongoingCount, upcomingCount, endedCount);
    }

    private void updateSectionVisibility(int ongoing, int upcoming, int ended) {
        setSectionVisible(ongoingSection,  ongoing  > 0);
        setSectionVisible(upcomingSection, upcoming > 0);
        setSectionVisible(endedSection,    ended    > 0);
    }

    private void setSectionVisible(VBox section, boolean visible) {
        section.setVisible(visible);
        section.setManaged(visible);
    }

    @FXML
    private void handleCategoryClick(MouseEvent event) {
        VBox clickedBox = (VBox) event.getSource();
        String rawId = clickedBox.getId();

        if (rawId == null || rawId.isBlank()) return;
        String targetCategory = switch (rawId.toUpperCase()) {
            case "FASHION"     -> "Fashion";
            case "ELECTRONICS" -> "Electronics";
            case "HOME"        -> "Home";
            case "ART"         -> "Art";
            case "BOOK"       -> "Book";
            case "JEWELRY"     -> "Jewelry";
            case "SPORTS"      -> "Sports";
            default            -> rawId;
        };
        if (targetCategory.equalsIgnoreCase(currentCategory)) {
            currentCategory = "ALL";
        } else {
            currentCategory = targetCategory;
        }

        clickedBox.getParent().getChildrenUnmodifiable().forEach(node -> {
            if (node instanceof VBox) {
                node.getStyleClass().remove("active-category");
            }
        });

        if (!"ALL".equals(currentCategory)) {
            clickedBox.getStyleClass().add("active-category");
        }
        fetchItemsFromServer();
    }

    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/AuctionFormPage.fxml"));
            Parent root = loader.load();

            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Add Item");

        } catch (IOException e) {
            e.printStackTrace();
            ToastService.showError(((Node) event.getSource()).getScene(), "Could not open Auction Form page.");
        }
    }

    public void refreshItems() {
        fetchItemsFromServer();
    }

    public void refreshNavBarInfo() {
        if (navBarController != null) {
            navBarController.refreshUserInfo();
        }
    }
}