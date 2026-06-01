package com.auction.client.ui.table;

import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.item.MyBidSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class BidTableFactory {
    private static final Insets CELL_PADDING = new Insets(0, 0, 0, 20);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private BidTableFactory() {}

    public static Callback<TableColumn<MyBidSummary, MyBidSummary>, TableCell<MyBidSummary, MyBidSummary>> itemCell(double thumbW, double thumbH) {
        return col -> new TableCell<>() {
            private final ImageView img = new ImageView();
            private final Label name = new Label();
            private final HBox container = new HBox(12, img, name);
            {
                img.setFitWidth(thumbW); img.setFitHeight(thumbH);
                img.setPreserveRatio(true);
                name.getStyleClass().add("item-name-label");
                container.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING);
            }
            @Override
            protected void updateItem(MyBidSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                name.setText(item.getItemName());
                img.setImage(null);
                if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isBlank()) {
                    ClientImageUtil.displayImage(item.getThumbnailUrl(), "images", img);
                }
                setGraphic(container);
            }
        };
    }

    public static Callback<TableColumn<MyBidSummary, Double>, TableCell<MyBidSummary, Double>> priceFormattedCell() {
        return col -> new TableCell<>() {
            { setAlignment(Pos.CENTER_RIGHT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : String.format("$%,.0f", price));
            }
        };
    }

    public static Callback<TableColumn<MyBidSummary, String>, TableCell<MyBidSummary, String>> statusBadgeCell() {
        return col -> new TableCell<>() {
            private final Label label = new Label();
            { label.getStyleClass().add("status-badge-base"); setAlignment(Pos.CENTER); }
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }

                label.getStyleClass().removeAll("status-winning", "status-won", "status-outbid", "status-upcoming", "status-ended", "status-banned", "status-live");

                switch (status) {
                    case "WON" -> { label.setText("Won"); label.getStyleClass().add("status-won"); }
                    case "WINNING" -> { label.setText("Winning"); label.getStyleClass().add("status-winning"); }
                    case "OUTBID" -> { label.setText("Outbid"); label.getStyleClass().add("status-outbid"); }
                    default -> label.setText(status);
                }
                setGraphic(label);
            }
        };
    }

    public static Callback<TableColumn<MyBidSummary, LocalDateTime>, TableCell<MyBidSummary, LocalDateTime>> endTimeCell() {
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

    public static Callback<TableColumn<MyBidSummary, MyBidSummary>, TableCell<MyBidSummary, MyBidSummary>> actionViewCell(Consumer<MyBidSummary> onViewClick) {
        return col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            { viewBtn.getStyleClass().add("action-btn-view"); setAlignment(Pos.CENTER); }
            @Override
            protected void updateItem(MyBidSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                viewBtn.setOnAction(e -> onViewClick.accept(item));
                setGraphic(viewBtn);
            }
        };
    }
}