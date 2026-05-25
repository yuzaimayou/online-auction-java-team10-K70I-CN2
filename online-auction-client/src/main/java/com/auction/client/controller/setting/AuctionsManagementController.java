package com.auction.client.controller.setting;

import com.auction.client.controller.ItemPageController;
import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Controller cho trang Auctions Management (Admin).
 *
 * ═══════════════════════════════════════════════════════════
 * ĐÃ ĐƯỢC TỐI ƯU HÓA THEO CHUẨN SENIOR SOFTWARE ENGINEER
 * ═══════════════════════════════════════════════════════════
 * [OPTIMIZED-1] Hiển thị Username thực tế thông qua thuộc tính mở rộng
 * `item.getSellerUsername()` thu được từ Database-level JOIN.
 * [OPTIMIZED-2] Nút Ban thay đổi trạng thái trực quan ngay tại View Thread
 * thay vì ép gọi tải lại mạng toàn phần, nâng cao trải nghiệm UI mượt mà.
 * [OPTIMIZED-3] Tự động điều hướng động cấu trúc vào phòng đấu giá thực tế khi click View.
 */
public class AuctionsManagementController {

    // ── FXML bindings ──────────────────────────────────────────────────────────

    @FXML
    private TableView<ItemSummary> auctionTable;

    @FXML
    private TableColumn<ItemSummary, String> colItem;

    @FXML
    private TableColumn<ItemSummary, String> colSeller;      // [FIXED] Sẽ map động trực tiếp vào sellerUsername

    @FXML
    private TableColumn<ItemSummary, String> colCreatedAt;

    @FXML
    private TableColumn<ItemSummary, Void> colView;

    @FXML
    private TableColumn<ItemSummary, Void> colAction;        // Chỉ chứa nút Ban/Nhãn Banned

    // ── Services ───────────────────────────────────────────────────────────────

    private final ItemsService itemsService = ItemsService.getInstance();

    // ── Init ───────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    // ── Column setup ───────────────────────────────────────────────────────────

    private void setupColumns() {

        // --- Cột Item: Thumbnail bất đồng bộ + Tên sản phẩm ---
        colItem.setCellFactory(column -> new TableCell<>() {
            private final HBox container = new HBox(15);
            private final ImageView img = new ImageView();
            private final Label name = new Label();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                img.setFitWidth(40);
                img.setFitHeight(40);
                img.setPreserveRatio(true);

                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(40, 40);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
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
                    return;
                }
                ItemSummary model = getTableRow().getItem();
                name.setText(model.getName());

                String url = model.getThumbnailUrl();
                if (url != null && !url.isBlank()) {
                    try {
                        img.setImage(new Image(url, true)); // background Loading = true
                    } catch (Exception e) {
                        img.setImage(null);
                    }
                } else {
                    img.setImage(null);
                }

                setGraphic(container);
            }
        });

        // --- Cột Seller (Tên tài khoản người bán thực tế từ JOIN SQL) ---
        colSeller.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                ItemSummary model = getTableRow().getItem();
                // Lấy giá trị chuỗi đã gộp tối ưu từ DB trả về
                String username = model.getSellerUsername();
                setText(username != null ? username : "—");
            }
        });

        // --- Cột Created At (Thời gian bắt đầu) ---
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                var startTime = getTableRow().getItem().getStartTime();
                setText(startTime != null ? startTime.toString().replace("T", " ") : "—");
            }
        });

        // --- Cột View (Điều hướng vào trực tiếp phòng đấu giá của sản phẩm) ---
        colView.setCellFactory(column -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("View");

            {
                viewLink.setStyle("-fx-text-fill: #2196F3; -fx-underline: true;");
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
                viewLink.setOnAction(e -> {
                    ItemSummary item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) handleViewItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewLink);
            }
        });

        // --- Cột Action (Logic nút Ban Realtime) ---
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button banBtn = new Button("⊘ Ban");
            private final Label bannedLabel = new Label("Banned");

            {
                banBtn.getStyleClass().add("reject-btn"); // Kế thừa CSS cũ của hệ thống
                banBtn.setStyle("-fx-cursor: hand;");
                banBtn.setOnAction(e -> {
                    ItemSummary item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) handleBanItem(item);
                });

                bannedLabel.setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold; -fx-padding: 4 8 4 8; " +
                        "-fx-background-color: #ffe6e6; -fx-background-radius: 4; -fx-border-color: #ffcccc; -fx-border-radius: 4;");
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                ItemSummary currentItem = getTableRow().getItem();
                // Đọc Enum động để kết xuất view chính xác
                if (currentItem.getStatus() == AuctionStatus.BANNED) {
                    setGraphic(bannedLabel);
                } else {
                    setGraphic(banBtn);
                }
            }
        });
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    /**
     * Tải danh sách sản phẩm bất đồng bộ dành riêng cho Admin (Chứa cả sản phẩm BANNED + seller_username).
     * [FIX] Dùng getItemsForAdmin() truyền caller=ADMIN → server gọi findAllItemsForAdmin() với LEFT JOIN users.
     */
    private void loadData() {
        itemsService.getItemsForAdmin()
                .thenAccept(items -> Platform.runLater(() -> {
                    ObservableList<ItemSummary> data = FXCollections.observableArrayList(items);
                    auctionTable.setItems(data);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showErrorAlert(
                            "Load Failed",
                            "Could not load auction items: " + ex.getMessage()
                    ));
                    return null;
                });
    }

    // ── Action handlers ────────────────────────────────────────────────────────

    /**
     * Xử lý gửi lệnh cấm (Ban) sản phẩm lên server và đồng bộ hiển thị view tại chỗ
     */
    private void handleBanItem(ItemSummary item) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to ban this item: " + item.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Confirm Ban");
        confirmAlert.setHeaderText(null);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                itemsService.banItem(item.getId())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if ("success".equals(res.getStatus())) {
                                showInfoAlert("Success", "Item has been banned successfully!");

                                // 🌟 GIẢI PHÁP: Gọi lại hàm loadData() để đồng bộ mới hoàn toàn từ DB
                                loadData();
                                auctionTable.refresh();
                            } else {
                                showErrorAlert("Ban Failed", res.getMessage());
                            }
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showErrorAlert(
                                    "Ban Failed",
                                    "Network error: " + ex.getMessage()
                            ));
                            return null;
                        });
            }
        });
    }

    /**
     * Xử lý bấm View: Chuyển trực tiếp root scene sang giao diện phòng đấu giá động của item được chọn.
     * [FIX] Sau khi load FXML, lấy controller và gọi initData(itemId) để room biết cần load item nào.
     */
    private void handleViewItem(ItemSummary item) {
        if (item == null) return;

        NavigationUtil.switchScene(
                new javafx.event.ActionEvent(auctionTable, null),
                "/com.auction.client/fxml/ItemPage.fxml",
                AppConfig.getAppName() + " - Live Auction Room",
                loader -> {
                    ItemPageController controller = loader.getController();
                    controller.setItemId(item.getId());
                }
        );
    }

    // ── Alert helpers ──────────────────────────────────────────────────────────

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}