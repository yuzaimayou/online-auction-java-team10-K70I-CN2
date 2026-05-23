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

    @SafeVarargs
    private static void disableSorting(TableColumn<ItemSummary, ?>... cols) {
        for (TableColumn<ItemSummary, ?> col : cols) col.setSortable(false);
    }

    // CỘT SẢN PHẨM (Hình ảnh + Tên)
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

    // ── 2. CỘT DANH MỤC (Phân tách rõ Style)
    private static void setupCategoryColumn(TableColumn<ItemSummary, String> col) {
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        col.setCellFactory(column -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add("category-badge");
                setAlignment(Pos.CENTER); // Căn giữa Badge trong Cell
            }

            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setGraphic(null);
                    return;
                }

                label.setText(category);

                label.getStyleClass().removeAll("cat-fashion", "cat-electronics", "cat-home", "cat-art", "cat-jewelry");

                String cat = category.toLowerCase();
                if (cat.contains("fashion"))     label.getStyleClass().add("cat-fashion");
                else if (cat.contains("electronics")) label.getStyleClass().add("cat-electronics");
                else if (cat.contains("home"))        label.getStyleClass().add("cat-home");
                else if (cat.contains("art"))         label.getStyleClass().add("cat-art");
                else if (cat.contains("jewelry"))     label.getStyleClass().add("cat-jewelry");

                setGraphic(label);
            }
        });
    }

    //  CỘT TRẠNG THÁI (Đầy đủ Live, Upcoming, Ended, Banned)
    private static void setupStatusColumn(TableColumn<ItemSummary, String> col) {
        col.setCellValueFactory(data -> {
            ItemSummary item = data.getValue();
            if (item == null) return new SimpleStringProperty("");

            if (item.getStatus() == AuctionStatus.BANNED) {
                return new SimpleStringProperty("BANNED");
            }

            AuctionStatus status = AuctionStatus.compute(item.getStartTime(), item.getEndTime());
            return new SimpleStringProperty(status.name());
        });

        col.setCellFactory(column -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add("status-badge-base");
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                label.getStyleClass().removeAll("status-upcoming", "status-ended", "status-banned", "status-live");

                switch (status) {
                    case "UPCOMING" -> {
                        label.setText("Upcoming");
                        label.getStyleClass().add("status-upcoming");
                    }
                    case "ENDED" -> {
                        label.setText("Ended");
                        label.getStyleClass().add("status-ended");
                    }
                    case "BANNED" -> {
                        label.setText("⛔ Banned");
                        label.getStyleClass().add("status-banned");
                    }
                    default -> {
                        label.setText("");
                        label.getStyleClass().add("status-live");
                    }
                }
                setGraphic(label);
            }
        });
    }

    // CỘT GIÁ CẢ
    private static void setupPriceColumn(TableColumn<ItemSummary, String> col) {
        col.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%,.0f", data.getValue().getCurrentPrice())));
        // Căn phải giá tiền cho đúng chuẩn UI
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(price);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    //. CỘT THỜI GIAN KẾT THÚC (Căn giữa: Ngày ở trên - Giờ ở dưới)
    private static void setupEndTimeColumn(TableColumn<ItemSummary, LocalDateTime> col) {
        col.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        col.setCellFactory(column -> new TableCell<>() {
            private final Label dateLabel = new Label();
            private final Label timeLabel = new Label();
            private final VBox  container = new VBox(4, dateLabel, timeLabel);

            {
                container.setAlignment(Pos.CENTER);
                this.setAlignment(Pos.CENTER);
                dateLabel.getStyleClass().add("end-time-date");
                timeLabel.getStyleClass().add("end-time-hour");
            }

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
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
                    // Chỉ hiện nút Edit khi item đang UPCOMING
                    // ONGOING, ENDED, BANNED → ẩn nút Edit hoàn toàn
                    AuctionStatus computed = item.getStatus() == AuctionStatus.BANNED
                            ? AuctionStatus.BANNED
                            : AuctionStatus.compute(item.getStartTime(), item.getEndTime());

                    boolean canEdit = computed == AuctionStatus.UPCOMING;
                    editBtn.setVisible(canEdit);
                    editBtn.setManaged(canEdit);
                    setGraphic(container);
                }
            }
        });
    }
}