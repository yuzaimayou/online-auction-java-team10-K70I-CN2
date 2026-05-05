package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MyAuctionsController {
    private ItemsService itemsService = ItemsService.getInstance();
    private final DateTimeFormatter multiLineFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm a");
    private Gson gson = new GsonUtil().getInstance();
    @FXML
    private ToggleButton profileInfoBtn;
    @FXML
    private ToggleButton myAuctionsBtn;
    @FXML
    private ToggleButton historyBidBtn;

    @FXML
    private TableView<Item> auctionTable;
    @FXML
    private TableColumn<Item, Item> productCol;
    @FXML
    private TableColumn<Item, String> categoryCol;
    @FXML
    private TableColumn<Item, String> statusCol;
    @FXML
    private TableColumn<Item, String> priceCol;
    @FXML
    private TableColumn<Item, LocalDateTime> endTimeCol;
    @FXML
    private TableColumn<Item, Item> actionCol;

    private ObservableList<Item> masterData = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        if (myAuctionsBtn != null) {
            myAuctionsBtn.setSelected(true);
        }
        auctionTable.setSelectionModel(null);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node header = auctionTable.lookup(".column-header-background");
            if (header != null) {
                header.setMouseTransparent(true);
            }
        });

        // ================= GỌI HELPER ĐỂ SETUP BẢNG =================
        // [CHỈNH SỬA] Truyền thêm this::handleDeleteProduct vào tham số cuối cùng
        MyAuctionsTableHelper.setupTableColumns(
                productCol, categoryCol, statusCol, priceCol, endTimeCol, actionCol,
                this::handleSwitchToProductEdit,
                this::handleDeleteProduct // <-- Tham số Callback xóa
        );

        displayItems();
    }


    private void handleEditItem(Item item) {
        System.out.println("Đang chỉnh sửa: " + item.getName());
    }

    // [THÊM MỚI] Hàm xử lý logic xóa sản phẩm
    private void handleDeleteProduct(Item itemToDelete) {
        // 1. Hiển thị cảnh báo xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa sản phẩm: " + itemToDelete.getName());
        alert.setContentText("Bạn có chắc chắn muốn xóa hoàn toàn sản phẩm này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // 2. Gửi request lên Server qua ItemsService
            // LƯU Ý: Bạn cần tạo hàm deleteItem(int/String id) trong ItemsService nếu chưa có.
            itemsService.deleteItem(itemToDelete.getId())
                    .thenAccept(responseMessage -> {
                        javafx.application.Platform.runLater(() -> {
                            if ("success".equals(responseMessage.getStatus())) {
                                System.out.println("Xóa sản phẩm thành công trên server!");

                                // 3. Xóa sản phẩm khỏi danh sách hiển thị trên Table
                                masterData.remove(itemToDelete);

                                // Thông báo cho người dùng
                                Alert success = new Alert(Alert.AlertType.INFORMATION);
                                success.setTitle("Thành công");
                                success.setHeaderText(null);
                                success.setContentText("Đã xóa sản phẩm thành công!");
                                success.show();
                            } else {
                                // Lỗi từ phía server trả về
                                System.err.println("Lỗi từ server khi xóa: " + responseMessage.getMessage());
                                Alert error = new Alert(Alert.AlertType.ERROR);
                                error.setTitle("Lỗi xóa sản phẩm");
                                error.setHeaderText(null);
                                error.setContentText("Không thể xóa sản phẩm: " + responseMessage.getMessage());
                                error.show();
                            }
                        });
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        javafx.application.Platform.runLater(() -> {
                            Alert error = new Alert(Alert.AlertType.ERROR);
                            error.setTitle("Lỗi kết nối");
                            error.setHeaderText(null);
                            error.setContentText("Lỗi khi kết nối với server để xóa sản phẩm.");
                            error.show();
                        });
                        return null;
                    });
        }
    }
    private void displayItems() {
        itemsService.getAllFromSeller()
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        System.out.println("Lấy danh sách sản phẩm của người bán thành công!");
                        // Xử lý hiển thị sản phẩm ở đây
                        Type listType = new TypeToken<List<Item>>() {
                        }.getType();
                        List<Item> items = gson.fromJson(responseMessage.getData(), listType);
                        javafx.application.Platform.runLater(() -> {

                            masterData.setAll(items);
                            auctionTable.setItems(masterData);

                            // Cập nhật giao diện với danh sách sản phẩm
                            // Ví dụ: hiển thị tên sản phẩm đầu tiên
                            if (!items.isEmpty()) {
                                System.out.println("Tên sản phẩm đầu tiên: " + items.get(0).getName());
                            } else {
                                System.out.println("Không có sản phẩm nào của người bán này.");
                            }
                        });
                    } else {
                        System.err.println("Lỗi khi lấy danh sách sản phẩm: " + responseMessage.getMessage());
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    System.err.println("Lỗi khi gửi yêu cầu lấy danh sách sản phẩm: " + e.getMessage());
                    return null;
                });
    }


    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) myAuctionsBtn.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSwitchToProductEdit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/ProductEdit.fxml")
            );
            Parent root = loader.load();
            SettingController.targetTab = "MyAuctions";

            Node sourceNode = (Node) event.getSource();
            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle(String.format("%s - Product Edit", AppConfig.getAppName()));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file ProductEdit.fxml! Kiểm tra lại đường dẫn.");
        }
    }


}