package com.auction.client.controller.setting;

import com.auction.client.controller.ItemEditController;
import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MyAuctionsController {

    // ── FXML fields ────────────────────────────────────────────────────────────
    // FIX [dead code]: xoá các ToggleButton navigation thừa — việc điều hướng
    // sidebar đã được SettingController xử lý, nested controller không cần lặp lại
    @FXML
    private TableView<ItemSummary>              auctionTable;
    @FXML
    private TableColumn<ItemSummary, ItemSummary> itemCol;
    @FXML
    private TableColumn<ItemSummary, String>    categoryCol;
    @FXML
    private TableColumn<ItemSummary, String>    statusCol;
    @FXML
    private TableColumn<ItemSummary, String>    priceCol;
    @FXML
    private TableColumn<ItemSummary, LocalDateTime> endTimeCol;
    @FXML
    private TableColumn<ItemSummary, ItemSummary> actionCol;
    @FXML
    private StackPane rootContainer;

    // ── Dependencies ───────────────────────────────────────────────────────────
    private final ItemsService itemsService = ItemsService.getInstance();
    private final Gson         gson         = GsonUtil.getInstance();
    private final User         loggedInUser = UserSession.getInstance().getLoggedInUser();
    private final ObservableList<ItemSummary> masterData = FXCollections.observableArrayList();

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        auctionTable.setSelectionModel(null);
        Platform.runLater(() -> {
            var header = auctionTable.lookup(".column-header-background");
            if (header != null) header.setMouseTransparent(true);
        });

        MyAuctionsTableHelper.setupTableColumns(
                itemCol, categoryCol, statusCol, priceCol, endTimeCol, actionCol,
                this::openEditModal,
                this::handleDeleteItem,
                this::handleViewItem
        );

        displayItems();
    }

    // ── Data loading ───────────────────────────────────────────────────────────
    private void displayItems() {
        if (loggedInUser == null) return;

        itemsService.getAllFromSeller(loggedInUser.getId())
                .thenAccept(responseMessage -> {
                    if (!"success".equals(responseMessage.getStatus())) {
                        // FIX [error handling]: hiển thị Alert thay vì chỉ log stderr
                        Platform.runLater(() -> showError(
                                "Failed to load auctions",
                                responseMessage.getMessage()
                        ));
                        return;
                    }

                    Type listType = new TypeToken<List<ItemSummary>>() {}.getType();
                    JsonElement jsonElement = gson.toJsonTree(responseMessage.getData());
                    List<ItemSummary> items = gson.fromJson(jsonElement, listType);

                    Platform.runLater(() -> {
                        masterData.setAll(items);
                        auctionTable.setItems(masterData);
                    });
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    // FIX [error handling]: alert thay vì System.err
                    Platform.runLater(() -> showError(
                            "Connection error",
                            "Could not reach the server. Please check your connection."
                    ));
                    return null;
                });
    }

    // ── Event handlers ─────────────────────────────────────────────────────────
    @FXML
    public void handleSwitchToItemEdit(ItemSummary selectedItem) {
        if (selectedItem == null) return;

        if (selectedItem.getStatus() == AuctionStatus.BANNED) {
            showError("Access Denied", "This product has been banned by management and cannot be modified.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/ItemEdit.fxml")
            );
            Parent root = loader.load();

            ItemEditController editController = loader.getController();
            editController.setItemId(selectedItem.getId());
            SettingController.targetTab = "MyAuctions";

            Scene currentScene = auctionTable.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle(String.format("%s - Item Edit", AppConfig.getAppName()));

        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation error", "Could not open Item Edit page.");
        }
    }

    @FXML
    public void handleViewItem(ItemSummary selectedItem) {
        if (selectedItem != null) navigateToDetail(selectedItem.getId());
    }

    private void handleDeleteItem(ItemSummary itemToDelete) {
        if (itemToDelete == null) return;

// [ADD SECURITY] Chặn gửi request xóa nếu sản phẩm đang bị BAN
        if (itemToDelete.getStatus() == AuctionStatus.BANNED) {
            showError("Action Not Allowed", "You cannot delete a product that has been banned.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete: " + itemToDelete.getName());
        confirm.setContentText("This action cannot be undone. Are you sure?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        itemsService.deleteItem(itemToDelete.getId())
                .thenAccept(responseMessage -> Platform.runLater(() -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        masterData.remove(itemToDelete);
                        showInfo("Item deleted successfully.");
                    } else {
                        // FIX [error handling]: tách riêng từng nhánh lỗi
                        showError("Delete failed", responseMessage.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    e.printStackTrace();
                    Platform.runLater(() ->
                            showError("Connection error", "Could not reach the server to delete item."));
                    return null;
                });
    }

    private void navigateToDetail(String itemId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/setting/ItemDetail.fxml")
            );
            Parent detailPage = loader.load();

            ItemDetailPageController detailController = loader.getController();
            detailController.loadItemData(itemId);

            SettingController mainSetting = SettingController.getInstance();
            if (mainSetting == null) return;

            VBox contentArea = mainSetting.getDynamicContent();
            VBox.setVgrow(detailPage, Priority.ALWAYS);
            contentArea.getChildren().setAll(detailPage);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation error", "Could not open item detail page.");
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "An unknown error occurred.");
        alert.show();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void openEditModal(ItemSummary item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ItemEdit.fxml"));
            Parent editForm = loader.load();
            ItemEditController controller = loader.getController();

            controller.setItemId(item.getId());
            StackPane sceneRoot = (StackPane) auctionTable.getScene().getRoot();

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("popup-overlay");
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.prefWidthProperty().bind(sceneRoot.widthProperty());
            overlay.prefHeightProperty().bind(sceneRoot.heightProperty());

            overlay.setOnMouseClicked(e -> {
                sceneRoot.getChildren().remove(overlay);
                displayItems();
            });

            editForm.setOnMouseClicked(e -> e.consume());
            overlay.getChildren().add(editForm);
            sceneRoot.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}