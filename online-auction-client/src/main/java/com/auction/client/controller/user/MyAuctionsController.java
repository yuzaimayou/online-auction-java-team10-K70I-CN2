package com.auction.client.controller.user;

import com.auction.client.controller.auction.ItemEditController;
import com.auction.client.network.AuctionSocketClient;
import com.auction.client.service.ItemsService;
import com.auction.client.service.UserSession;
import com.auction.client.ui.util.ModalUtil;
import com.auction.client.ui.table.ItemTableFactory;
import com.auction.client.util.*;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class MyAuctionsController {

    @FXML
    private TableView<ItemSummary> auctionTable;
    @FXML
    private TableColumn<ItemSummary, ItemSummary> itemCol;
    @FXML
    private TableColumn<ItemSummary, String> categoryCol;
    @FXML
    private TableColumn<ItemSummary, ItemSummary> statusCol;
    @FXML
    private TableColumn<ItemSummary, Double> priceCol;
    @FXML
    private TableColumn<ItemSummary, LocalDateTime> endTimeCol;
    @FXML
    private TableColumn<ItemSummary, ItemSummary> actionCol;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;

    private final ItemsService itemsService = ItemsService.getInstance();
    private final User loggedInUser = UserSession.getInstance().getLoggedInUser();
    private final ObservableList<ItemSummary> masterData = FXCollections.observableArrayList();
    private FilteredList<ItemSummary> filteredData;
    private final AuctionSocketClient network = AuctionSocketClient.getInstance();

    @FXML
    public void initialize() {
        auctionTable.setSelectionModel(null);
        setupStatusFilter();
        setupColumns();
        setupFilterLogic();

        Platform.runLater(() -> {
            disableColumnHeaderClick();
            registerWindowFocusReload();
        });

        loadData();
        setupSocketListener();
    }

    private void setupStatusFilter() {
        if (statusFilter == null) return;
        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Upcoming", "Ongoing", "Ended", "Banned"
        ));
        statusFilter.setValue("All Status");
    }

    private void setupColumns() {
        itemCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue()));
        itemCol.setCellFactory(ItemTableFactory.itemCell(50, 50));

        categoryCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(ItemTableFactory.categoryBadgeCell());

        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        statusCol.setCellFactory(ItemTableFactory.statusBadgeCell());
        // Đọc giá trị Double thuần túy từ Model, việc vẽ chuỗi tiền tệ đẩy sang Factory xử lý
        priceCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCurrentPrice()));
        priceCol.setCellFactory(ItemTableFactory.priceFormattedCell());

        endTimeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getEndTime()));
        endTimeCol.setCellFactory(ItemTableFactory.endTimeCell());

        actionCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue()));
        actionCol.setCellFactory(ItemTableFactory.sellerActionCell(
                this::openEditModal,
                this::handleDeleteItem,
                this::handleViewItem
        ));

        ItemTableFactory.disableSorting(itemCol, categoryCol, statusCol, priceCol, actionCol);
    }

    private void disableColumnHeaderClick() {
        var header = auctionTable.lookup(".column-header-background");
        if (header != null) header.setMouseTransparent(true);
    }

    private void registerWindowFocusReload() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        stage.focusedProperty().addListener((obs, was, isNow) -> {
            if (isNow) loadData();
        });
    }

    private void setupFilterLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilter.getValue();

        filteredData.setPredicate(item -> {
            boolean matchesSearch = keyword.isEmpty()
                    || (item.getName() != null && item.getName().toLowerCase().contains(keyword))
                    || (item.getCategory() != null && item.getCategory().toLowerCase().contains(keyword));

            boolean matchesStatus = true;
            if (selectedStatus != null && !"All Status".equals(selectedStatus)) {
                String s = item.getStatus().name();
                matchesStatus = switch (selectedStatus) {
                    case "Upcoming" -> "UPCOMING".equals(s);
                    case "Ended" -> "ENDED".equals(s);
                    case "Banned" -> "BANNED".equals(s);
                    case "Ongoing" -> !"UPCOMING".equals(s) && !"ENDED".equals(s) && !"BANNED".equals(s);
                    default -> true;
                };
            }
            return matchesSearch && matchesStatus;
        });
    }

    private void loadData() {
        if (loggedInUser == null) return;

        itemsService.getAllFromSeller(loggedInUser.getId())
                .thenAccept(items -> Platform.runLater(() -> {
                    masterData.setAll(items);
                    auctionTable.setItems(filteredData);
                    applyFilters();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Load Failed", ex.getMessage()));
                    return null;
                });
    }

    private void setupSocketListener() {
        network.setAuctionRoomListener(new com.auction.client.network.AuctionRoomListener() {
            @Override
            public void onItemBanned(String itemId) {
                Platform.runLater(() -> {
                    boolean isUpdated = false;
                    for (ItemSummary item : masterData) {
                        if (item.getId().equals(itemId)) {
                            item.setStatus(AuctionStatus.BANNED);
                            isUpdated = true;
                            break;
                        }
                    }

                    if (isUpdated) {
                        auctionTable.refresh();
                    }
                });
            }
            @Override public void onNewBid(BidPayload p) {}
            @Override public void onAuctionExtended(LocalDateTime t) {}
            @Override public void onAutoBidState(AutoBidPayload data) {}
        });
    }

    private void handleViewItem(ItemSummary item) {
        if (item == null) return;
        NavigationUtil.switchToItemPage(new javafx.event.ActionEvent(auctionTable, null), item.getId(), item.getName());
    }


    private void handleDeleteItem(ItemSummary item) {
        if (item == null) return;
        if (item.getStatus() == AuctionStatus.BANNED) {
            AlertUtil.showError("Action Not Allowed", "You cannot delete a banned item.");
            return;
        }
        boolean confirmed = AlertUtil.showConfirm("Confirm Delete", "Delete \"" + item.getName() + "\"? This action cannot be undone.");
        if (!confirmed) return;

        itemsService.deleteItem(item.getId())
                .thenAccept(res -> Platform.runLater(() -> {
                    if ("success".equals(res.getStatus())) {
                        masterData.remove(item);
                        AlertUtil.showInfo("Success", "Item deleted successfully.");
                    } else {
                        AlertUtil.showError("Delete Failed", res.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Connection Error", ex.getMessage()));
                    return null;
                });
    }

    private void openEditModal(ItemSummary item) {
        if (item == null) return;
        StackPane sceneRoot = (StackPane) auctionTable.getScene().getRoot();
        ModalUtil.showEditModal(
                sceneRoot,
                "/com.auction.client/fxml/ItemEdit.fxml",
                loader -> {
                    ItemEditController ctrl = loader.getController();
                    ctrl.setItemId(item.getId());
                },
                this::loadData
        );
    }
}