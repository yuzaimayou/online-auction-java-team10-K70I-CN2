package com.auction.client.controller.setting;

import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
            TableColumn<ItemSummary, ItemSummary>   itemCol,
            TableColumn<ItemSummary, String>        categoryCol,
            TableColumn<ItemSummary, String>        statusCol,
            TableColumn<ItemSummary, String>        priceCol,
            TableColumn<ItemSummary, LocalDateTime> endTimeCol,
            TableColumn<ItemSummary, ItemSummary>   actionCol,
            Consumer<ItemSummary> onEditAction,
            Consumer<ItemSummary> onDeleteAction,
            Consumer<ItemSummary> onViewAction) {

        disableSorting(itemCol, categoryCol, statusCol, priceCol, endTimeCol, actionCol);
        setupItemColumn(itemCol);
        setupCategoryColumn(categoryCol);
        setupStatusColumn(statusCol);
        setupPriceColumn(priceCol);
        setupEndTimeColumn(endTimeCol);
        setupActionColumn(actionCol, onEditAction, onDeleteAction, onViewAction);
    }

    // ── Column setup helpers

    @SafeVarargs
    private static void disableSorting(TableColumn<ItemSummary, ?>... cols) {
        for (TableColumn<ItemSummary, ?> col : cols) col.setSortable(false);
    }
    private static void setupItemColumn(TableColumn<ItemSummary, ItemSummary> col) {
        col.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        col.setCellFactory(param -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label     nameLabel = new Label();
            private final HBox      container = new HBox(15, imageView, nameLabel);

            {
                imageView.setFitWidth(70);
                imageView.setFitHeight(70);
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
                    return;
                }
                nameLabel.setText(item.getName());
                imageView.setImage(null);
                if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isBlank()) {
                    ClientImageUtil.displayImage(item.getThumbnailUrl(), "images", imageView, 400, 400);
                }
                setGraphic(container);
            }
        });
    }

    private static void setupCategoryColumn(TableColumn<ItemSummary, String> col) {
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) { setGraphic(null); return; }

                Label label = new Label(category);
                label.getStyleClass().add("category-badge");

                label.getStyleClass().add(resolveCategoryStyle(category));
                setGraphic(label);
            }
        });
    }

    private static String resolveCategoryStyle(String category) {
        String cat = category.toLowerCase();
        if (cat.contains("fashion"))     return "cat-fashion";
        if (cat.contains("electronics")) return "cat-electronics";
        if (cat.contains("home"))        return "cat-home";
        if (cat.contains("art"))         return "cat-art";
        if (cat.contains("jewelry"))     return "cat-jewelry";
        return "";
    }

    private static void setupStatusColumn(TableColumn<ItemSummary, String> col) {
        col.setCellValueFactory(data -> {
            ItemSummary item = data.getValue();
            if (item == null) {
                return new SimpleStringProperty("");
            }

            if (item.getStatus() == AuctionStatus.BANNED) {
                return new SimpleStringProperty(AuctionStatus.BANNED.getDisplayName());
            }

            AuctionStatus status = AuctionStatus.compute(item.getStartTime(), item.getEndTime());
            return new SimpleStringProperty(status.getDisplayName());
        });

        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }

                Label label = new Label(status);
                label.getStyleClass().add("status-badge-base");
                switch (status) {
                    case "Upcoming" -> label.getStyleClass().add("status-upcoming");
                    case "Ended"    -> label.getStyleClass().add("status-ended");
                    case "BANNED"   -> {
                        label.setText("⛔ Banned");
                        label.getStyleClass().add("status-ended");
                    }
                    default  -> label.getStyleClass().add("status-live");
                }
                setGraphic(label);
            }
        });
    }

    private static void setupPriceColumn(TableColumn<ItemSummary, String> col) {
        col.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%,.0f", data.getValue().getCurrentPrice())));
    }

    private static void setupEndTimeColumn(TableColumn<ItemSummary, LocalDateTime> col) {
        col.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        col.setCellFactory(column -> new TableCell<>() {
            private final Label dateLabel = new Label();
            private final Label timeLabel = new Label();
            private final VBox  container = new VBox(2, dateLabel, timeLabel);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                dateLabel.getStyleClass().add("end-time-date");
                timeLabel.getStyleClass().add("end-time-hour");
            }

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                dateLabel.setText(item.format(DATE_FORMAT));
                timeLabel.setText(item.format(TIME_FORMAT));
                setGraphic(container);
            }
        });
    }

    private static void setupActionColumn(
            TableColumn<ItemSummary, ItemSummary> col,
            Consumer<ItemSummary> onEditAction,
            Consumer<ItemSummary> onDeleteAction,
            Consumer<ItemSummary> onViewAction) {

        col.setPrefWidth(220);
        col.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        col.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button viewBtn   = new Button("View");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   container = new HBox(8, editBtn, viewBtn, deleteBtn);
            private ItemSummary  currentItem;

            {
                container.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("action-btn-edit");
                viewBtn.getStyleClass().add("action-btn-view");
                deleteBtn.getStyleClass().add("action-btn-delete");
                editBtn.setMinWidth(Region.USE_PREF_SIZE);
                viewBtn.setMinWidth(Region.USE_PREF_SIZE);
                deleteBtn.setMinWidth(Region.USE_PREF_SIZE);

                viewBtn.setOnAction(e   -> { if (onViewAction   != null) onViewAction.accept(currentItem); });
                editBtn.setOnAction(e   -> { if (onEditAction   != null) onEditAction.accept(currentItem); });
                deleteBtn.setOnAction(e -> { if (onDeleteAction != null) onDeleteAction.accept(currentItem); });
            }

            @Override
            protected void updateItem(ItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                currentItem = item;

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // Nếu sản phẩm đã bị quản trị viên Ban, vô hiệu hóa (Disable) nút chỉnh sửa
                    // để người dùng không thể cố tình vào thay đổi dữ liệu trái phép.
                    if (item.getStatus() == AuctionStatus.BANNED) {
                        editBtn.setDisable(true);
                    } else {
                        editBtn.setDisable(false);
                    }
                    setGraphic(container);
                }
            }
        });
    }
}