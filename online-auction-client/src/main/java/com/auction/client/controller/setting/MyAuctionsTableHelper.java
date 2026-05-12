package com.auction.client.controller.setting;

import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.item.ItemSummary;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class MyAuctionsTableHelper {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    public static void setupTableColumns(
            TableColumn<ItemSummary, ItemSummary> itemCol,
            TableColumn<ItemSummary, String> categoryCol,
            TableColumn<ItemSummary, String> statusCol,
            TableColumn<ItemSummary, String> priceCol,
            TableColumn<ItemSummary, LocalDateTime> endTimeCol,
            TableColumn<ItemSummary, ItemSummary> actionCol,
            Consumer<ItemSummary> onEditAction,
            Consumer<ItemSummary> onDeleteAction,
            Consumer<ItemSummary> onViewAction) {

        //  TẮT TÍNH NĂNG SORT
        itemCol.setSortable(false);
        categoryCol.setSortable(false);
        statusCol.setSortable(false);
        priceCol.setSortable(false);
        endTimeCol.setSortable(false);
        actionCol.setSortable(false);

        // ITEM
        itemCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        itemCol.setCellFactory(param -> new TableCell<ItemSummary, ItemSummary>() {
            private final ImageView imageView = new ImageView();
            private final Label nameLabel = new Label();
            private final HBox container = new HBox(15, imageView, nameLabel);
            {
                imageView.setFitWidth(50);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
                container.setAlignment(Pos.CENTER_LEFT);

            }
            @Override
            protected void updateItem(ItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    imageView.setImage(null);
                } else {
                    nameLabel.setText(item.getName());
                    imageView.setImage(null);
                    if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isBlank()) {
                        ClientImageUtil.displayImage(item.getThumbnailUrl(), "images", imageView, 50, 50);
                    }
                    setGraphic(container);
                }
            }
        });

        // CATEGORY
        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        categoryCol.setCellFactory(column -> new TableCell<ItemSummary, String>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(category);
                    label.getStyleClass().add("category-badge");

                    String cat = category.toLowerCase();
                    if (cat.contains("fashion")) label.getStyleClass().add("cat-fashion");
                    else if (cat.contains("electronics")) label.getStyleClass().add("cat-electronics");
                    else if (cat.contains("home")) label.getStyleClass().add("cat-home");
                    else if (cat.contains("art")) label.getStyleClass().add("cat-art");
                    else if (cat.contains("jewelry")) label.getStyleClass().add("cat-jewelry");
                    setGraphic(label);
                }
            }
        });

        //  STATUS
        statusCol.setCellValueFactory(cellData -> {
            ItemSummary item = cellData.getValue();
            LocalDateTime now = LocalDateTime.now();
            String status = "Ongoing";
            if (now.isBefore(item.getStartTime())) status = "Upcoming";
            else if (now.isAfter(item.getEndTime())) status = "Ended";
            return new SimpleStringProperty(status);
        });
        statusCol.setCellFactory(column -> new TableCell<ItemSummary, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(status);
                    label.getStyleClass().add("status-badge-base");
                    if ("Upcoming".equals(status)) label.getStyleClass().add("status-upcoming");
                    else if ("Ended".equals(status)) label.getStyleClass().add("status-ended");
                    else label.getStyleClass().add("status-live");
                    setGraphic(label);
                }
            }
        });

        //  PRICE
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty("$" + String.format("%,.0f", cellData.getValue().getCurrentPrice())));

        //  END TIME
        endTimeCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getEndTime()));
        endTimeCol.setCellFactory(column -> new TableCell<ItemSummary, LocalDateTime>() {
            private final Label dateLabel = new Label();
            private final Label timeLabel = new Label();
            private final VBox container = new VBox(dateLabel, timeLabel);

            {
                container.setSpacing(2);
                container.setAlignment(Pos.CENTER_LEFT);
                dateLabel.getStyleClass().add("end-time-date");
                timeLabel.getStyleClass().add("end-time-hour");
            }

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

                    dateLabel.setText(item.format(dateFormatter));
                    timeLabel.setText(item.format(timeFormatter));

                    setGraphic(container);
                }
            }
        });

        //  ACTION
        actionCol.setPrefWidth(220);
        actionCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        actionCol.setCellFactory(column -> new TableCell<ItemSummary, ItemSummary>() {
            private final Button editBtn = new Button("Edit");
            private final Button viewBtn = new Button("View");
            private final Button deleteBtn = new Button("Delete");
            private final HBox container = new HBox(8, editBtn, viewBtn, deleteBtn);
            private ItemSummary currentItem;

            {
                container.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("action-btn-edit");
                viewBtn.getStyleClass().add("action-btn-view");
                deleteBtn.getStyleClass().add("action-btn-delete");

                editBtn.setMinWidth(Region.USE_PREF_SIZE);
                viewBtn.setMinWidth(Region.USE_PREF_SIZE);
                deleteBtn.setMinWidth(Region.USE_PREF_SIZE);

                viewBtn.setOnAction(e -> { if (onViewAction != null) onViewAction.accept(currentItem); });
                editBtn.setOnAction(e -> { if (onEditAction != null) onEditAction.accept(currentItem); });
                deleteBtn.setOnAction(e -> { if (onDeleteAction != null) onDeleteAction.accept(currentItem); });
            }

            @Override
            protected void updateItem(ItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                this.currentItem = item;
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }
}