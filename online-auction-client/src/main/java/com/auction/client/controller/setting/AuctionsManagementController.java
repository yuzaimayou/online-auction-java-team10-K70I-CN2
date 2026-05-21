package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.shared.model.item.ItemSummary;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

import java.util.List;

/**
 * Controller cho trang Auctions Management (Admin).
 *
 * ═══════════════════════════════════════════════════════════
 * THAY ĐỔI SO VỚI PHIÊN BẢN CŨ
 * ═══════════════════════════════════════════════════════════
 *
 * [CHANGE-1] Xóa bỏ chức năng Accept/Reject (colAction trước có 2 nút):
 *   - Trước: Button "✓ Accept" + Button "✕ Reject"
 *   - Sau:   Button "⊘ Ban" duy nhất
 *
 * [CHANGE-2] Kết nối dữ liệu thật từ server thay cho hardcode:
 *   - Trước: loadData() tạo ObservableList tĩnh bằng tay
 *   - Sau:   loadData() gọi ItemsService.getItems(null, "ALL") bất đồng bộ
 *             rồi cập nhật TableView trên JavaFX thread qua Platform.runLater()
 *
 * [CHANGE-3] Model của TableView chuyển từ ItemModel (client-only) sang ItemSummary (shared):
 *   - Trước: TableView<ItemModel>
 *   - Sau:   TableView<ItemSummary>
 *   => Không cần ItemModel nữa cho trang này.
 *
 * [CHANGE-4] colItem hiển thị thumbnail từ thumbnailUrl lấy server trả về.
 *   - Image load bất đồng bộ (background=true) để không đóng băng UI.
 *   - Fallback placeholder khi URL null/lỗi.
 *
 * [CHANGE-5] Thêm handleBanItem(ItemSummary item):
 *   - Gọi ItemsService.banItem(item.getId()) — xem ghi chú CLIENT-SIDE bên dưới.
 *   - Hiển thị Alert xác nhận trước khi thực hiện.
 *   - Sau khi ban thành công: reload lại danh sách.
 *
 * ═══════════════════════════════════════════════════════════
 * THAY ĐỔI CẦN THỰC HIỆN Ở PHÍA CLIENT (ItemsService.java)
 * ═══════════════════════════════════════════════════════════
 *
 * [CLIENT-SIDE] Thêm method banItem(String itemId) vào ItemsService:
 *
 *   public CompletableFuture<ResponseMessage> banItem(String itemId) {
 *       // Tạo payload tối giản chỉ chứa status BANNED
 *       String jsonPayload = "{\"status\":\"BANNED\"}";
 *       HttpRequest request = HttpRequest.newBuilder()
 *           .uri(URI.create(String.format("%s/api/items/%s/ban", AppConfig.getHttpUrl(), itemId)))
 *           .header("Content-Type", "application/json")
 *           .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
 *           .build();
 *       return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
 *           .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
 *   }
 *
 *   Hoặc — nếu muốn dùng lại endpoint PUT /api/items/{id} hiện có
 *   mà không thêm endpoint mới — xem ghi chú SERVER-SIDE bên dưới.
 *
 * ═══════════════════════════════════════════════════════════
 * THAY ĐỔI BẮT BUỘC Ở PHÍA SERVER (tối thiểu)
 * ═══════════════════════════════════════════════════════════
 *
 * Có 2 phương án — chọn 1:
 *
 * PHƯƠNG ÁN A — Thêm endpoint mới /api/items/{id}/ban  (KHUYẾN NGHỊ, ít rủi ro nhất):
 *   1. MainServer.java:
 *      httpServer.createContext("/api/items/", new ItemDetailHandler()); // đã có
 *      httpServer.createContext("/api/items/ban/", new BanItemHandler()); // [SERVER-ADD-1] THÊM MỚI
 *
 *   2. Tạo BanItemHandler.java (server):
 *      - Nhận PUT /api/items/ban/{itemId}
 *      - Gọi itemService.banItem(itemId)
 *
 *   3. ItemService.java (server) — thêm method:  [SERVER-ADD-2]
 *      public boolean banItem(String itemId) {
 *          return itemRepository.updateStatus(itemId, ItemStatusConstants.BANNED);
 *      }
 *
 *   4. ItemRepository.java — thêm method:        [SERVER-ADD-3]
 *      public boolean updateStatus(String itemId, String status) {
 *          String sql = "UPDATE items SET status = ? WHERE id = ?";
 *          try (Connection conn = DatabaseManager.getConnection();
 *               PreparedStatement stmt = conn.prepareStatement(sql)) {
 *              stmt.setString(1, status);
 *              stmt.setString(2, itemId);
 *              return stmt.executeUpdate() > 0;
 *          } catch (Exception e) {
 *              LOGGER.severe("Failed to update status: " + e.getMessage());
 *              return false;
 *          }
 *      }
 *
 * PHƯƠNG ÁN B — Tái sử dụng PUT /api/items/{id} hiện có (ít thay đổi server hơn):
 *   Cần sửa ItemDetailHandler.updateItem() để không bắt buộc toàn bộ ItemPayload,
 *   chấp nhận partial update với chỉ trường "status". Rủi ro: logic hiện tại
 *   gọi setItem() sẽ NPE nếu thiếu field.  => Không khuyến nghị, dễ gây lỗi.
 *
 * => CONTROLLER NÀY GIẢ SỬ PHƯƠNG ÁN A ĐÃ ĐƯỢC TRIỂN KHAI.
 * ═══════════════════════════════════════════════════════════
 */
public class AuctionsManagementController {

    // ── FXML bindings ──────────────────────────────────────────────────────────

    @FXML
    private TableView<ItemSummary> auctionTable;           // [CHANGE-3] ItemModel → ItemSummary

    @FXML
    private TableColumn<ItemSummary, String> colItem;

    @FXML
    private TableColumn<ItemSummary, String> colSeller;

    @FXML
    private TableColumn<ItemSummary, String> colCreatedAt;

    @FXML
    private TableColumn<ItemSummary, Void> colView;

    @FXML
    private TableColumn<ItemSummary, Void> colAction;      // [CHANGE-1] Chỉ còn nút Ban

    // ── Services ───────────────────────────────────────────────────────────────

    private final ItemsService itemsService = ItemsService.getInstance();

    // ── Init ───────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadData();     // [CHANGE-2] Load dữ liệu thật từ server
    }

    // ── Column setup ───────────────────────────────────────────────────────────

    private void setupColumns() {

        // --- Cột Item: thumbnail + tên ---
        // [CHANGE-4] Dùng thumbnailUrl từ ItemSummary thay cho image path hardcode
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

                // [CHANGE-4] Load thumbnail bất đồng bộ; fallback nếu URL null/lỗi
                String url = model.getThumbnailUrl();
                if (url != null && !url.isBlank()) {
                    try {
                        img.setImage(new Image(url, true)); // background=true
                    } catch (Exception e) {
                        img.setImage(null);
                    }
                } else {
                    img.setImage(null);
                }

                setGraphic(container);
            }
        });

        // --- Cột Seller ---
        // ItemSummary không có sellerId hiển thị tên — dùng getId() tạm hoặc
        // mở rộng ItemSummary sau.
        // [NOTE] Nếu muốn hiển thị tên seller, cần thêm field sellerName vào
        //        ItemSummary (server) và mapRowToItemSummary() trong ItemRepository.
        //        Hiện tại dùng sellerName nếu có, fallback về "—".
        colSeller.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                // [NOTE] ItemSummary chưa có getSellerName() — tạm để "—"
                // Khi thêm field sellerName vào ItemSummary thì thay dòng này:
                // setText(getTableRow().getItem().getSellerName());
                setText("—");
            }
        });

        // --- Cột Created At ---
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                // [NOTE] ItemSummary chưa có getCreatedAt() — dùng startTime tạm thay
                // Khi thêm field create_at vào ItemSummary thì thay dòng này:
                // setText(getTableRow().getItem().getCreatedAt().toString());
                var startTime = getTableRow().getItem().getStartTime();
                setText(startTime != null ? startTime.toString() : "—");
            }
        });

        // --- Cột View ---
        colView.setCellFactory(column -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("View");

            {
                viewLink.getStyleClass().add("view-link");
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

        // --- Cột Action ---
        // Thay 2 nút Accept/Reject bằng 1 nút Ban duy nhất
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button banBtn = new Button("⊘ Ban");

            {
                banBtn.getStyleClass().add("reject-btn"); // tái dùng style đỏ từ CSS
                banBtn.setOnAction(e -> {
                    ItemSummary item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) handleBanItem(item);
                });
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
                // Ẩn nút Ban nếu item đã bị banned rồi
                boolean alreadyBanned = "BANNED".equals(getTableRow().getItem().getStatus() != null
                        ? getTableRow().getItem().getStatus().name()
                        : null);
                setGraphic(alreadyBanned ? new Label("Banned") : banBtn);
            }
        });
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    /**
     * Load danh sách item từ server bất đồng bộ.
     * Dùng ItemsService.getItems(null, "ALL") — gọi GET /api/items không filter.
     */
    private void loadData() {
        itemsService.getItems(null, "ALL")
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
     *  Xử lý ban item.
     * Hiện thị dialog xác nhận → gọi ItemsService.banItem() → reload danh sách.
     *
     * Yêu cầu: ItemsService.banItem(String itemId) phải được thêm vào — xem
     */
    private void handleBanItem(ItemSummary item) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Ban");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to ban this item: " + item.getName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ItemsService.getInstance().banItem(item.getId())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if ("success".equals(res.getStatus())) {
                                showInfoAlert("Success", "Item has been banned successfully!");

                                // Cách 1: Tải lại toàn bộ bảng dữ liệu từ Server
                                loadData();

                                // Cách 2 (Tối ưu hơn): Xóa trực tiếp khỏi danh sách đang hiển thị trên UI công khai
                                // tableView.getItems().remove(item);
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
     * Xử lý click "View" — mở trang chi tiết item.
     * TODO: Navigate đến ItemDetailPage với item.getId()
     */
    private void handleViewItem(ItemSummary item) {
        // TODO: dùng SceneManager / NavigationService để chuyển trang
        System.out.println("[AuctionsManagement] View item: " + item.getId());
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