package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MyAuctionsController {
    // Điều hướng
    @FXML
    private ToggleButton profileInfoBtn;

    @FXML
    private ToggleButton myAuctionsBtn;

    @FXML
    private ToggleButton historyBidBtn;

    // Bảng dữ liệu
    @FXML
    private TableView<Item> auctionTable;

    @FXML
    private TableColumn<Item, Item> itemCol;

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

    private ItemsService itemsService = ItemsService.getInstance();
    private final DateTimeFormatter multiLineFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm a");
    private Gson gson = new GsonUtil().getInstance();
    private User loggedInUser = UserSession.getInstance().getLoggedInUser();
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
        MyAuctionsTableHelper.setupTableColumns(
                itemCol, categoryCol, statusCol, priceCol, endTimeCol, actionCol,
                this::handleSwitchToItemEdit,
                this::handleDeleteItem // <-- Tham số Callback xóa
        );
        displayItems();
    }

    // Đổ dữ liệu
    private void displayItems() {
        itemsService.getAllFromSeller(loggedInUser.getId(), loggedInUser.getUsername())
                .thenAccept(responseMessage -> {
                    if ("success".equals(responseMessage.getStatus())) {
                        System.out.println("Lấy danh sách sản phẩm của người bán thành công!");

                        Type listType = new TypeToken<List<Item>>() {}.getType();
                        List<Item> items = gson.fromJson(responseMessage.getData(), listType);
                        Platform.runLater(() -> {
                            masterData.setAll(items);
                            auctionTable.setItems(masterData);

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

    private void handleEditItem(Item item) {
        System.out.println("Đang chỉnh sửa: " + item.getName());
    }

    // Event handlers
    @FXML
    public void handleSwitchToItemEdit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/ItemEdit.fxml")
            );
            Parent root = loader.load();
            SettingController.targetTab = "MyAuctions";

            Node sourceNode = (Node) event.getSource();
            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle(String.format("%s - Item Edit", AppConfig.getAppName()));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file ItemEdit.fxml! Kiểm tra lại đường dẫn.");
        }
    }
    private void handleDeleteItem(Item itemToDelete) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa sản phẩm: " + itemToDelete.getName());
        alert.setContentText("Bạn có chắc chắn muốn xóa hoàn toàn sản phẩm này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            itemsService.deleteItem(itemToDelete.getId())
                    .thenAccept(responseMessage -> {
                        javafx.application.Platform.runLater(() -> {
                            if ("success".equals(responseMessage.getStatus())) {
                                System.out.println("Xóa sản phẩm thành công trên server!");

                                masterData.remove(itemToDelete);

                                Alert success = new Alert(Alert.AlertType.INFORMATION);
                                success.setTitle("Thành công");
                                success.setHeaderText(null);
                                success.setContentText("Đã xóa sản phẩm thành công!");
                                success.show();
                            } else {
                                // Lỗi từ server
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
    }