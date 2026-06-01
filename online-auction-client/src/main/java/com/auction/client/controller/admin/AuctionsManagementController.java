package com.auction.client.controller.admin;

import com.auction.client.service.ItemsService;
import com.auction.client.ui.table.ItemTableFactory;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.ItemSummary;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionsManagementController {

    @FXML private TableView<ItemSummary> auctionTable;
    @FXML private TableColumn<ItemSummary, ItemSummary> colItem;
    @FXML private TableColumn<ItemSummary, String> colSeller;
    @FXML private TableColumn<ItemSummary, String> colCreatedAt;
    @FXML private TableColumn<ItemSummary, Void> colView;
    @FXML private TableColumn<ItemSummary, Void> colAction;

    private final ItemsService itemsService = ItemsService.getInstance();

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        auctionTable.setSelectionModel(null);
    }

    private void setupColumns() {
        colItem.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colItem.setCellFactory(ItemTableFactory.itemCell(50, 50));
        colSeller.setCellFactory(ItemTableFactory.sellerCell());
        colCreatedAt.setCellFactory(ItemTableFactory.createdAtCell());

        // Sạch sẽ tuyệt đối nhờ hàm mới tinh trong Factory
        colView.setCellFactory(ItemTableFactory.viewLinkCell(this::handleViewItem));
        colAction.setCellFactory(ItemTableFactory.adminActionCell(this::handleBanItem));

        ItemTableFactory.disableSorting(colItem, colSeller, colCreatedAt, colView, colAction);
    }

    private void loadData() {
        itemsService.getItemsForAdmin()
                .thenAccept(items -> Platform.runLater(() ->
                        auctionTable.setItems(FXCollections.observableArrayList(items))
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Load Failed", "Could not load: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleBanItem(ItemSummary item) {
        boolean confirmed = AlertUtil.showConfirm("Confirm Ban", "Are you sure you want to ban: " + item.getName() + "?");
        if (!confirmed) return;

        itemsService.banItem(item.getId())
                .thenAccept(res -> Platform.runLater(() -> {
                    if ("success".equals(res.getStatus())) {
                        auctionTable.getItems().remove(item); // Tối ưu không cần load lại toàn bộ bảng
                        AlertUtil.showInfo("Success", "Item has been banned.");
                    } else {
                        AlertUtil.showError("Ban Failed", res.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Ban Failed", "Network error: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleViewItem(ItemSummary item) {
        NavigationUtil.switchToItemPage(new javafx.event.ActionEvent(auctionTable, null), item.getId(), item.getName());
    }
}