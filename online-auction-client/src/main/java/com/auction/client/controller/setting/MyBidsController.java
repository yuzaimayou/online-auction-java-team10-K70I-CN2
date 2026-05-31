package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.*;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.MyBidSummary;          // ← KHÁC
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
import static com.auction.client.util.AuctionTableCellFactory.disableSorting;

public class MyBidsController {

    @FXML private TableView<MyBidSummary> bidsTable;          // ← KHÁC
    @FXML private TableColumn<MyBidSummary, MyBidSummary> itemCol;
    @FXML private TableColumn<MyBidSummary, String> currentBidCol;
    @FXML private TableColumn<MyBidSummary, String> yourBidCol; // ← KHÁC (cột mới)
    @FXML private TableColumn<MyBidSummary, String> statusCol;
    @FXML private TableColumn<MyBidSummary, LocalDateTime> endTimeCol;
    @FXML private TableColumn<MyBidSummary, MyBidSummary> actionCol;
    @FXML private TextField searchField;

    private final ItemsService itemsService = ItemsService.getInstance();
    private final User loggedInUser = UserSession.getInstance().getLoggedInUser();
    private final ObservableList<MyBidSummary> masterData = FXCollections.observableArrayList(); // ← KHÁC
    private FilteredList<MyBidSummary> filteredData;           // ← KHÁC

    @FXML
    public void initialize() {
        bidsTable.setSelectionModel(null);
        setupColumns();
        setupFilterLogic();
        Platform.runLater(() -> {
            Stage stage = (Stage) bidsTable.getScene().getWindow();
            stage.focusedProperty().addListener((obs, was, isNow) -> { if (isNow) loadData(); });
        });
        loadData();
    }

    private void setupColumns() {
        // Giống MyAuctionsController
        itemCol.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()));
        itemCol.setCellFactory(col -> new TableCell<>() {
            private final javafx.scene.image.ImageView img = new javafx.scene.image.ImageView();
            private final Label name = new Label();
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(12, img, name);
            {
                img.setFitWidth(50); img.setFitHeight(50);
                img.setPreserveRatio(true);
                name.getStyleClass().add("item-name-label");
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
            }
            @Override
            protected void updateItem(MyBidSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                name.setText(item.getItemName());
                img.setImage(null);
                if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isBlank()) {
                    ClientImageUtil.displayImage(item.getThumbnailUrl(), "images", img, 50, 50);
                }
                setGraphic(container);
            }
        });

        // Current Bid — giá phiên hiện tại
        currentBidCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("$%,.0f", d.getValue().getCurrentPrice()))
        );
        currentBidCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : price);
                setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            }
        });
        // ← KHÁC: Your Bid — giá cao nhất người dùng đặt
        yourBidCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("$%,.0f", d.getValue().getMyHighestBid()))
        );
        yourBidCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : price);
                setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            }
        });
        // ← KHÁC: Status với logic WON / WINNING
        statusCol.setCellValueFactory(d -> {
            MyBidSummary item = d.getValue();
            if (item.isWinner() && item.getStatus() == AuctionStatus.ENDED)
                return new SimpleStringProperty("WON");
            if (item.isWinner() && item.getStatus() == AuctionStatus.ONGOING)
                return new SimpleStringProperty("WINNING");
            return new SimpleStringProperty(item.getStatus().name());
        });
        statusCol.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            { label.getStyleClass().add("status-badge-base"); setAlignment(javafx.geometry.Pos.CENTER); }
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                label.getStyleClass().removeAll("status-upcoming","status-ended","status-banned","status-live","status-won");
                switch (status) {
                    case "WON"     -> { label.setText("🏆 Won");     label.getStyleClass().add("status-ended"); }
                    case "WINNING" -> { label.setText("🔥 Winning"); label.getStyleClass().add("status-live"); }
                    case "UPCOMING"-> { label.setText("Upcoming");   label.getStyleClass().add("status-upcoming"); }
                    case "ENDED"   -> { label.setText("Ended");      label.getStyleClass().add("status-ended"); }
                    case "BANNED"  -> { label.setText("⛔ Banned");  label.getStyleClass().add("status-banned"); }
                    default        -> { label.setText("Ongoing");    label.getStyleClass().add("status-live"); }
                }
                setGraphic(label);
            }
        });

        endTimeCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEndTime()));
        endTimeCol.setCellFactory(col -> new TableCell<>() {
            private final Label dateLabel = new Label();
            private final Label timeLabel = new Label();
            private final javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4, dateLabel, timeLabel);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                setAlignment(javafx.geometry.Pos.CENTER);
                dateLabel.getStyleClass().add("end-time-date");
                timeLabel.getStyleClass().add("end-time-hour");
            }
            @Override
            protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                if (empty || dt == null) { setGraphic(null); return; }
                dateLabel.setText(dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                timeLabel.setText(dt.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));
                setGraphic(box);
            }
        });

        // Action — mở ItemPage, giống MyAuctionsController
        actionCol.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            { viewBtn.getStyleClass().add("action-btn-view"); }
            @Override
            protected void updateItem(MyBidSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                viewBtn.setOnAction(e -> NavigationUtil.switchToItemPage(
                        new javafx.event.ActionEvent(bidsTable, null),
                        item.getItemId(),
                        item.getItemName()
                ));
                setGraphic(viewBtn);
            }
        });

        disableSorting(itemCol, statusCol, actionCol);
    }

    private void setupFilterLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        filteredData.setPredicate(item ->
                keyword.isEmpty() || item.getItemName().toLowerCase().contains(keyword)
        );
    }

    // ← KHÁC: gọi getMyBids() thay vì getAllFromSeller()
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
}