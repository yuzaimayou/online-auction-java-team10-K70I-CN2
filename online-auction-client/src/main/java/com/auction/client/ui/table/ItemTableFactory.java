package com.auction.client.ui.table;

import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.DateTimeUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ItemTableFactory {
    private static final Insets CELL_PADDING = new Insets(0, 0, 0, 20);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private ItemTableFactory() {}

    public static Callback<TableColumn<ItemSummary, ItemSummary>, TableCell<ItemSummary, ItemSummary>> itemCell(double thumbW, double thumbH) {
        return column -> new TableCell<>() {
            private final ImageView img = new ImageView();
            private final Label name = new Label();
            private final HBox container = new HBox(15, img, name);
            {
                img.setFitWidth(thumbW); img.setFitHeight(thumbH);
                img.setPreserveRatio(true); img.setSmooth(true);
                name.getStyleClass().add("item-name-label");
                container.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING);
            }
            @Override
            protected void updateItem(ItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                name.setText(item.getName()); img.setImage(null);
                if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isBlank()) {
                    ClientImageUtil.displayImage(item.getThumbnailUrl(), "images", img);
                }
                setGraphic(container);
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, String>, TableCell<ItemSummary, String>> categoryBadgeCell() {
        return col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add("category-badge");
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) { setGraphic(null); return; }

                label.setText(category);
                label.getStyleClass().removeAll("cat-fashion", "cat-electronics", "cat-home", "cat-art", "cat-jewelry");

                String cat = category.toLowerCase();
                if (cat.contains("fashion")) label.getStyleClass().add("cat-fashion");
                else if (cat.contains("electronics")) label.getStyleClass().add("cat-electronics");
                else if (cat.contains("home")) label.getStyleClass().add("cat-home");
                else if (cat.contains("art")) label.getStyleClass().add("cat-art");
                else if (cat.contains("jewelry")) label.getStyleClass().add("cat-jewelry");

                setGraphic(label);
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, ItemSummary>, TableCell<ItemSummary, ItemSummary>> statusBadgeCell() {
        return col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add("status-badge-base");
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(ItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                label.getStyleClass().removeAll("status-upcoming", "status-ended", "status-banned", "status-live");

                AuctionStatus status = item.getStatus();
                String statusStr = status != null ? status.name() : "ONGOING";

                switch (statusStr) {
                    case "UPCOMING" -> { label.setText("Upcoming"); label.getStyleClass().add("status-upcoming"); }
                    case "ENDED" -> { label.setText("Ended"); label.getStyleClass().add("status-ended"); }
                    case "BANNED" -> { label.setText("⛔ Banned"); label.getStyleClass().add("status-banned"); }
                    case "WON" -> { label.setText("🏆 Won"); label.getStyleClass().add("status-won"); }
                    case "WINNING" -> { label.setText("🔥 Winning"); label.getStyleClass().add("status-live"); }
                    default -> { label.setText("Ongoing"); label.getStyleClass().add("status-live"); }
                }
                setGraphic(label);
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, Double>, TableCell<ItemSummary, Double>> priceFormattedCell() {
        return col -> new TableCell<>() {
            { setAlignment(Pos.CENTER_RIGHT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.0f", price));
                }
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, LocalDateTime>, TableCell<ItemSummary, LocalDateTime>> endTimeCell() {
        return col -> new TableCell<>() {
            private final Label dateLabel = new Label();
            private final Label timeLabel = new Label();
            private final VBox box = new VBox(4, dateLabel, timeLabel);
            {
                box.setAlignment(Pos.CENTER); setAlignment(Pos.CENTER);
                dateLabel.getStyleClass().add("end-time-date");
                timeLabel.getStyleClass().add("end-time-hour");
            }
            @Override
            protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                if (empty || dt == null) { setGraphic(null); return; }
                dateLabel.setText(dt.format(DATE_FMT));
                timeLabel.setText(dt.format(TIME_FMT));
                setGraphic(box);
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, ItemSummary>, TableCell<ItemSummary, ItemSummary>> sellerActionCell(
            Consumer<ItemSummary> onEdit, Consumer<ItemSummary> onDelete, Consumer<ItemSummary> onView
    ) {
        return column -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button viewBtn = new Button("View");
            private final Button deleteBtn = new Button("Delete");
            private final HBox container = new HBox(8, editBtn, viewBtn, deleteBtn);
            private ItemSummary current;
            {
                container.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("action-btn-edit");
                viewBtn.getStyleClass().add("action-btn-view");
                deleteBtn.getStyleClass().add("action-btn-delete");

                editBtn.setMinWidth(Region.USE_PREF_SIZE);
                viewBtn.setMinWidth(Region.USE_PREF_SIZE);
                deleteBtn.setMinWidth(Region.USE_PREF_SIZE);

                editBtn.setOnAction(e -> {
                    if (onEdit != null && current != null)
                        onEdit.accept(current);
                });
                viewBtn.setOnAction(e -> {
                    if (onView != null && current != null)
                        onView.accept(current);
                });
                deleteBtn.setOnAction(e -> {
                    if (onDelete != null && current != null)
                        onDelete.accept(current);
                });
            }
            @Override
            protected void updateItem(ItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                current = item;
                if (empty || item == null) { setGraphic(null); return; }

                AuctionStatus status = item.getStatus();
                if (status == AuctionStatus.BANNED) {
                    setVisible(editBtn, false); setVisible(deleteBtn, false);
                } else {
                    setVisible(editBtn, status == AuctionStatus.UPCOMING);
                    setVisible(deleteBtn, true);
                }
                setGraphic(container);
            }
            private void setVisible(Button btn, boolean visible) {
                btn.setVisible(visible); btn.setManaged(visible);
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, String>, TableCell<ItemSummary, String>> sellerCell() {
        return col -> new TableCell<>() {
            { setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(String ignored, boolean empty) {
                super.updateItem(ignored, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                String username = getTableRow().getItem().getSellerUsername();
                setText(username != null ? username : "—");
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, String>, TableCell<ItemSummary, String>> createdAtCell() {
        return col -> new TableCell<>() {
            { setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(String ignored, boolean empty) {
                super.updateItem(ignored, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                setText(DateTimeUtil.format(getTableRow().getItem().getStartTime()));
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, Void>, TableCell<ItemSummary, Void>> viewLinkCell(Consumer<ItemSummary> onViewItem) {
        return col -> new TableCell<>() {
            private final Label view = new Label("View");
            {
                view.getStyleClass().add("view-link");
                view.setOnMouseClicked(e -> {
                    ItemSummary item = getTableView().getItems().get(getIndex());
                    if (item != null) onViewItem.accept(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : view);
            }
        };
    }

    public static Callback<TableColumn<ItemSummary, Void>, TableCell<ItemSummary, Void>> adminActionCell(Consumer<ItemSummary> onBan) {
        return column -> new TableCell<>() {
            private final Button banBtn = new Button("⊘ Ban");
            private final Label bannedLabel = new Label("Banned");
            {
                banBtn.getStyleClass().add("reject-btn");
                bannedLabel.getStyleClass().add("status-banned-badge");
                setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING);
                banBtn.setOnAction(e -> {
                    ItemSummary item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) onBan.accept(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(getTableRow().getItem().getStatus() == AuctionStatus.BANNED ? bannedLabel : banBtn);
            }
        };
    }

    @SafeVarargs
    public static <S> void disableSorting(TableColumn<S, ?>... columns) {
        for (TableColumn<S, ?> col : columns) col.setSortable(false);
    }
}