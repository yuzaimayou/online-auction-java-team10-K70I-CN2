package com.auction.client.ui.table;

import com.auction.client.controller.admin.UserRowViewModel;
import com.auction.client.service.UserSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.function.Consumer;

public class UserTableFactory {
    private static final Insets CELL_PADDING = new Insets(0, 0, 0, 15);

    private UserTableFactory() {}

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> leftAlignCell() {
        return col -> new TableCell<>() {
            { setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item);
            }
        };
    }

    public static Callback<TableColumn<UserRowViewModel, String>, TableCell<UserRowViewModel, String>> roleCell() {
        return col -> new TableCell<>() {
            { setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("banned_user".equalsIgnoreCase(item) ? "User" : item);
                }
            }
        };
    }

    public static Callback<TableColumn<UserRowViewModel, String>, TableCell<UserRowViewModel, String>> statusBadgeCell() {
        return column -> new TableCell<>() {
            private final Label badge = new Label();
            { setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().removeAll("badge-active", "badge-suspended");

                    // Khuyên khích Vân dùng CSS class thay vì hardcode .setStyle() nha!
                    if ("Active".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #137333; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle("-fx-background-color: #FCE8E6; -fx-text-fill: #C5221F; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                }
            }
        };
    }

    public static Callback<TableColumn<UserRowViewModel, Void>, TableCell<UserRowViewModel, Void>> banActionCell(Consumer<UserRowViewModel> onBanClick) {
        return column -> new TableCell<>() {
            private final Button banBtn = new Button("Ban");
            {
                banBtn.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DADCE0; -fx-border-radius: 4; -fx-cursor: hand;");
                banBtn.setOnAction(event -> {
                    UserRowViewModel user = getTableView().getItems().get(getIndex());
                    if (user != null) onBanClick.accept(user);
                });
                setAlignment(Pos.CENTER_LEFT); setPadding(CELL_PADDING);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                UserRowViewModel user = getTableRow().getItem();
                String currentAdminId = UserSession.getInstance().getCurrentUserId();

                boolean isSelf   = user.getId() != null && user.getId().equals(currentAdminId);
                boolean isAdmin  = "Admin".equalsIgnoreCase(user.getRole());
                boolean isBanned = "Suspended".equalsIgnoreCase(user.getStatus()) || "banned_user".equalsIgnoreCase(user.getRole());

                setGraphic((isSelf || isAdmin || isBanned) ? null : banBtn);
            }
        };
    }
}