package com.auction.client.controller.setting;

import com.auction.client.model.ItemModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

public class AuctionsManagementController {

    @FXML
    private TableView<ItemModel> auctionTable;
    @FXML
    private TableColumn<ItemModel, String> colItem;
    @FXML
    private TableColumn<ItemModel, String> colSeller;
    @FXML
    private TableColumn<ItemModel, String> colCreatedAt;
    @FXML
    private TableColumn<ItemModel, Void> colView;
    @FXML
    private TableColumn<ItemModel, Void> colAction;

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colItem.setCellFactory(column -> new TableCell<>() {
            private final HBox container = new HBox(15);
            private final ImageView img = new ImageView();
            private final Label name = new Label();
            {
                container.setAlignment(Pos.CENTER_LEFT);
                img.setFitWidth(40); img.setFitHeight(40);

                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(40, 40);
                clip.setArcWidth(10); clip.setArcHeight(10);
                img.setClip(clip);

                container.getChildren().addAll(img, name);
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ItemModel model = getTableRow().getItem();
                    name.setText(model.getItemName());
                    setGraphic(container);
                }
            }
        });

        colSeller.setCellValueFactory(data -> data.getValue().sellerProperty());
        colCreatedAt.setCellValueFactory(data -> data.getValue().dateProperty());

        colView.setCellFactory(column -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("View");
            {
                viewLink.getStyleClass().add("view-link");
                setAlignment(Pos.CENTER_LEFT); // Căn ô sang trái
                setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewLink);
            }
        });

        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button acceptBtn = new Button("✓ Accept");
            private final Button rejectBtn = new Button("✕ Reject");
            private final HBox container = new HBox(10, acceptBtn, rejectBtn);
            {
                acceptBtn.getStyleClass().add("accept-btn");
                rejectBtn.getStyleClass().add("reject-btn");
                container.setAlignment(Pos.CENTER_LEFT); // Nút lệch trái theo yêu cầu
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }
    private void loadData() {
        javafx.collections.ObservableList<ItemModel> data = javafx.collections.FXCollections.observableArrayList(
                new ItemModel("Apple MacBook Pro M3", "Store_AZ", "2025-05-10 14:30", "/images/macbook.png"),
                new ItemModel("iPhone 15 Pro Max", "Apple_VN", "2025-05-11 09:00", "/images/iphone.png"),
                new ItemModel("iPhone 15 Pro Max", "Apple_VN", "2025-05-11 09:00", "/images/iphone.png"),
                new ItemModel("iPhone 15 Pro Max", "Apple_VN", "2025-05-11 09:00", "/images/iphone.png"),
                new ItemModel("iPhone 15 Pro Max", "Apple_VN", "2025-05-11 09:00", "/images/iphone.png"),
                new ItemModel("iPhone 15 Pro Max", "Apple_VN", "2025-05-11 09:00", "/images/iphone.png"),
                new ItemModel("Sony WH-1000XM5", "Gadget_World", "2025-05-11 10:15", "/images/sony.png")
        );
        auctionTable.setItems(data);
    }
}