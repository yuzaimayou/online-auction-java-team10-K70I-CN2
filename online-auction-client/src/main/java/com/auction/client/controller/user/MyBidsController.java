package com.auction.client.controller.user;

import com.auction.client.service.ItemsService;
import com.auction.client.ui.table.BidTableFactory;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.NavigationUtil;
import com.auction.client.service.UserSession;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.MyBidSummary;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;

import static com.auction.client.ui.table.ItemTableFactory.disableSorting;

public class MyBidsController {

    @FXML private TableView<MyBidSummary> bidsTable;
    @FXML private TableColumn<MyBidSummary, MyBidSummary> itemCol;
    @FXML private TableColumn<MyBidSummary, Double> currentBidCol; // Đổi sang Double
    @FXML private TableColumn<MyBidSummary, Double> yourBidCol;    // Đổi sang Double
    @FXML private TableColumn<MyBidSummary, String> statusCol;
    @FXML private TableColumn<MyBidSummary, LocalDateTime> endTimeCol;
    @FXML private TableColumn<MyBidSummary, MyBidSummary> actionCol;
    @FXML private TextField searchField;

    private final ItemsService itemsService = ItemsService.getInstance();
    private final User loggedInUser = UserSession.getInstance().getLoggedInUser();
    private final ObservableList<MyBidSummary> masterData = FXCollections.observableArrayList();
    private FilteredList<MyBidSummary> filteredData;

    @FXML
    public void initialize() {
        bidsTable.setSelectionModel(null);
        setupColumns();
        setupFilterLogic();

        Platform.runLater(() -> {
            Stage stage = (Stage) bidsTable.getScene().getWindow();
            if (stage != null) {
                stage.focusedProperty().addListener((obs, was, isNow) -> { if (isNow) loadData(); });
            }
        });
        loadData();
    }

    private void setupColumns() {
        itemCol.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()));
        itemCol.setCellFactory(BidTableFactory.itemCell(50, 50));

        // Đẩy giá trị Double thuần, định dạng chuỗi tiền tệ do Factory phụ trách
        currentBidCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCurrentPrice()));
        currentBidCol.setCellFactory(BidTableFactory.priceFormattedCell());

        yourBidCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMyHighestBid()));
        yourBidCol.setCellFactory(BidTableFactory.priceFormattedCell());

        // Logic tính toán trạng thái (Bussiness Logic) giữ nguyên ở Controller
        statusCol.setCellValueFactory(d -> {
            MyBidSummary item = d.getValue();
            if (item.isWinner()) {
                return item.getStatus() == AuctionStatus.ENDED
                        ? new SimpleStringProperty("WON")
                        : new SimpleStringProperty("WINNING");
            }
            return new SimpleStringProperty("OUTBID");
        });
        statusCol.setCellFactory(BidTableFactory.statusBadgeCell());

        endTimeCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEndTime()));
        endTimeCol.setCellFactory(BidTableFactory.endTimeCell());

        actionCol.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()));
        actionCol.setCellFactory(BidTableFactory.actionViewCell(this::handleViewItem));

        disableSorting(itemCol, statusCol, actionCol);
    }

    private void setupFilterLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        filteredData.setPredicate(item -> keyword.isEmpty() || item.getItemName().toLowerCase().contains(keyword));
    }

    private void loadData() {
        if (loggedInUser == null) return;
        itemsService.getMyBids()
                .thenAccept(items -> Platform.runLater(() -> {
                    masterData.setAll(items);
                    bidsTable.setItems(filteredData);
                    applyFilters();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Load Failed", ex.getMessage()));
                    return null;
                });
    }

    private void handleViewItem(MyBidSummary item) {
        NavigationUtil.switchToItemPage(
                new javafx.event.ActionEvent(bidsTable, null),
                item.getItemId(),
                item.getItemName()
        );
    }
}