package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.AuctionTableCellFactory;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.ItemSummary;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import static com.auction.client.util.AuctionTableCellFactory.disableSorting;

public class AuctionsManagementController {

    @FXML
    private TableView<ItemSummary> auctionTable;
    @FXML
    private TableColumn<ItemSummary, ItemSummary> colItem;
    @FXML
    private TableColumn<ItemSummary, String> colSeller;
    @FXML
    private TableColumn<ItemSummary, String> colCreatedAt;
    @FXML
    private TableColumn<ItemSummary, Void> colView;
    @FXML
    private TableColumn<ItemSummary, Void> colAction;

    private final ItemsService itemsService = ItemsService.getInstance();

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        auctionTable.setSelectionModel(null);
    }

    private void setupColumns() {
        colItem.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue())
        );
        colItem.setCellFactory(AuctionTableCellFactory.itemCell(50, 50));
        colSeller.setCellFactory(AuctionTableCellFactory.sellerCell());
        colCreatedAt.setCellFactory(AuctionTableCellFactory.createdAtCell());

        colView.setCellFactory(col -> new TableCell<>() {
            private final Label view = new Label("View");

            {
                view.getStyleClass().add("view-link");
                view.setOnMouseClicked(e -> {
                    ItemSummary item = getTableView().getItems().get(getIndex());
                    handleViewItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(view);
                }
            }
        });

        colAction.setCellFactory(
                AuctionTableCellFactory.adminActionCell(this::handleBanItem)
        );

        disableSorting(colItem, colSeller, colCreatedAt, colView, colAction);
    }

    private void loadData() {
        itemsService.getItemsForAdmin()
                .thenAccept(items -> Platform.runLater(() ->
                        auctionTable.setItems(FXCollections.observableArrayList(items))
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            AlertUtil.showError("Load Failed", "Could not load auction items: " + ex.getMessage())
                    );
                    return null;
                });
    }

    private void handleBanItem(ItemSummary item) {
        boolean confirmed = AlertUtil.showConfirm(
                "Confirm Ban",
                "Are you sure you want to ban: " + item.getName() + "?"
        );
        if (!confirmed) return;

        itemsService.banItem(item.getId())
                .thenAccept(res -> Platform.runLater(() -> {
                    if ("success".equals(res.getStatus())) {
                        loadData();
                    } else {
                        AlertUtil.showError("Ban Failed", res.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            AlertUtil.showError("Ban Failed", "Network error: " + ex.getMessage())
                    );
                    return null;
                });
    }

    private void handleViewItem(ItemSummary item) {
        NavigationUtil.switchToItemPage(
                new javafx.event.ActionEvent(auctionTable, null),
                item.getId(),
                item.getName()
        );
    }
}