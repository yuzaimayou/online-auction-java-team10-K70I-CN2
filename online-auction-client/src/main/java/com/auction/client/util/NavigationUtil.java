package com.auction.client.util;

import com.auction.client.controller.common.ChatBoxAiController;
import com.auction.client.controller.HomePageController;
import com.auction.client.controller.setting.SettingController;
import com.auction.client.controller.auth.VerifyController;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationUtil {
    private static Parent homePageRoot;
    private static HomePageController homePageController;
    private static final ChatBoxAiController chatAiWidget = new ChatBoxAiController();

    private static Parent wrapWithChatBox(Parent root) {
        StackPane container;
        if (root instanceof StackPane) {
            container = (StackPane) root;
        } else {
            container = new StackPane(root);
        }

        Node bubble = chatAiWidget.getBubble();
        Node chatBox = chatAiWidget.getChatBox();

        if (bubble.getParent() != null) {
            ((StackPane) bubble.getParent()).getChildren().removeAll(chatBox, bubble);
        }

        container.getChildren().addAll(chatBox, bubble);

        StackPane.setAlignment(bubble, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bubble, new javafx.geometry.Insets(0, 30, 30, 0));
        StackPane.setAlignment(chatBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatBox, new javafx.geometry.Insets(0, 30, 30, 30));

        return container;
    }
    public static void switchToOtpScreen(ActionEvent event, String registeredEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource(
                            "/com.auction.client/fxml/authenticator/Verify.fxml" )
            );
            Node verifyNode = loader.load();
            VerifyController verifyController = loader.getController();
            verifyController.setEmail(registeredEmail);
            Node sourceNode = (Node) event.getSource();
            StackPane dynamicContentArea =
                    (StackPane) sourceNode.getScene().lookup("#dynamicContentArea");

            if (dynamicContentArea != null) {
                dynamicContentArea.getChildren().clear();
                dynamicContentArea.getChildren().add(verifyNode);
            } else {
                System.err.println("Không tìm thấy StackPane dynamicContentArea");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleSwitchToHomePage(Label label) {
        try {
            System.out.println("Da chuyen sang trang chu");
            if (homePageRoot == null || homePageController == null) {
                FXMLLoader loader = new FXMLLoader(
                        NavigationUtil.class.getResource(
                                "/com.auction.client/fxml/HomePage.fxml")
                );
                homePageRoot = loader.load();
                homePageController = loader.getController();

            } else {
                // Các lần sau: Tái sử dụng UI cũ cho mượt
                // NHƯNG PHẢI GỌI HÀM CẬP NHẬT TÊN NGƯỜI DÙNG
                if (homePageController != null) {
                    homePageController.refreshNavBarInfo(); // <-- Gọi hàm này để báo NavBar đổi tên
                    homePageController.refreshItems();;   // Refresh sản phẩm như cũ
                }

            }
            // ============================================================
            Parent wrappedRoot = wrapWithChatBox(homePageRoot);
            Scene currentScene = label.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(wrappedRoot);
            stage.setTitle(String.format("%s - Home", AppConfig.getAppName()));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file HomePage.fxml!");
        }
    }

    public static void clearCache() {
        homePageRoot = null;
        homePageController = null;
    }

    public static void handleSwitchToSetting(Label label) {
        handleSwitchToSetting(label, "");
    }

    public static void handleSwitchToSetting(Label label, String option) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource(
                            "/com.auction.client/fxml/setting/Setting.fxml"));
            Parent root = loader.load();
            Parent wrappedRoot = wrapWithChatBox(root);
            SettingController settingController = loader.getController();

            Scene currentScene = label.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(wrappedRoot);
            stage.setTitle(String.format( "%s - Profile Page", AppConfig.getAppName()));

            if (option.equals("profile")) {
                settingController.handleProfileInfo(new ActionEvent());
            } else if (option.equals("myAuctions")) {
                settingController.handleMyAuctions(new ActionEvent());
            } else if (option.equals("historyBid")) {
                settingController.handleHistoryBid(new ActionEvent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void handleSwitchToItemPage(Label contextLabel, String itemId, String itemName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource("/com.auction.client/fxml/ItemPage.fxml")
            );
            Parent root = loader.load();
            com.auction.client.controller.ItemPageController controller = loader.getController();
            controller.setItemId(itemId);
            Parent wrappedRoot = wrapWithChatBox(root);

            Scene currentScene = contextLabel.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(wrappedRoot);
            stage.setTitle(String.format("Online Auction System - %s", itemName));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi load trang sản phẩm: " + e.getMessage());
        }
    }
    public static void switchToLogin(ActionEvent event) {

        try {

            Parent loginRoot = FXMLLoader.load(
                    NavigationUtil.class.getResource(
                            "/com.auction.client/fxml/authenticator/Login.fxml"
                    )
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.getScene().setRoot(loginRoot);

            stage.setTitle("Login - Auction System");

        } catch (IOException e) {

            System.err.println("Không tìm thấy file Login.fxml!");

            e.printStackTrace();
        }
    }
}