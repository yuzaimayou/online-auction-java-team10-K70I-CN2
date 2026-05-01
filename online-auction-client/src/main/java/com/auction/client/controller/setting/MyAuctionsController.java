package com.auction.client.controller.setting;

import com.auction.client.service.ItemsService;
import com.auction.client.util.AppConfig;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class MyAuctionsController {
    private ItemsService itemsService = ItemsService.getInstance();
    private Gson gson = new GsonUtil().getInstance();

    @FXML
    private ToggleButton profileInfoBtn;

    @FXML
    private ToggleButton myAuctionsBtn;

    @FXML
    private ToggleButton historyBidBtn;


    @FXML
    public void initialize() {
        if (myAuctionsBtn != null) {
            myAuctionsBtn.setSelected(true);
        }
        displayItems();

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