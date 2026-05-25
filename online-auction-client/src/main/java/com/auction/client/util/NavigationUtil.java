package com.auction.client.util;

import com.auction.client.controller.HomePageController;
import com.auction.client.controller.ItemPageController;
import com.auction.client.controller.auth.VerifyController;
import com.auction.client.controller.common.ChatBoxAiController;
import com.auction.client.controller.setting.SettingController;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class NavigationUtil {
    private static Parent homePageRoot;
    private static HomePageController homePageController;
    private static ItemPageController currentItemController;
    private static final ChatBoxAiController chatAiWidget = new ChatBoxAiController();

    public static void switchScene(
            Event event,
            String fxmlPath,
            String title
    ) {
        switchScene(event, fxmlPath, title, null);
    }

    public static void switchScene(
            Event event,
            String fxmlPath,
            String title,
            Consumer<FXMLLoader> initController
    ) {
        try {
            disposeCurrentPage();
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(fxmlPath));

            Parent root = loader.load();
            if (initController != null) {
                initController.accept(loader);
            }

            Parent wrappedRoot = wrapWithChatBox(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.getScene().setRoot(wrappedRoot);
            stage.setTitle(title);
            Object controller = loader.getController();

            if (controller instanceof ItemPageController itemController) {
                currentItemController = itemController;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void switchToLogin(Event event) {
        switchScene(
                event,
                "/com.auction.client/fxml/authenticator/Login.fxml",
                "Login - Auction System"
        );
    }
    public static void switchToRegister(Event event) {
        switchScene(
                event,
                "/com.auction.client/fxml/authenticator/Register.fxml",
                "Register - Auction System"
        );
    }
    public static void switchToOtpScreen(Event event, String email) {
        switchScene(
                event,
                "/com.auction.client/fxml/authenticator/Verify.fxml",
                "Verify OTP",
                loader -> {
                    VerifyController controller = loader.getController();
                    controller.setEmail(email);}
        );
    }
    public static void handleSwitchToHomePage(Label label) {
        try {
            disposeCurrentPage();
            if (homePageRoot == null) {
                FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(
                                        "/com.auction.client/fxml/HomePage.fxml"));
                homePageRoot = loader.load();
                homePageController = loader.getController();}
            else {
                homePageController.refreshNavBarInfo();
                homePageController.refreshItems();
            }
            Parent wrappedRoot = wrapWithChatBox(homePageRoot);
            Stage stage = (Stage) label.getScene().getWindow();
            stage.getScene().setRoot(wrappedRoot);
            stage.setTitle(AppConfig.getAppName() + " - Home");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleSwitchToAuctionFormPage(Event event) {
        switchScene(
                event,
                "/com.auction.client/fxml/AuctionFormPage.fxml",
                AppConfig.getAppName() + " - Add Item"
        );
    }

    public static void switchToItemPage(Event event, String itemId, String itemName) {
        switchScene(
                event,
                "/com.auction.client/fxml/ItemPage.fxml",
                AppConfig.getAppName() + " - " + itemName,

                loader -> {
                    ItemPageController controller = loader.getController();
                    controller.setItemId(itemId);}
        );
    }

    public static void handleSwitchToSetting(Event event) {
        handleSwitchToSetting(
                event,
                ""
        );
    }

    public static void handleSwitchToSetting(Event event, String option) {
        switchScene(
                event,
                "/com.auction.client/fxml/setting/Setting.fxml",
                AppConfig.getAppName() + " - Profile",
                loader -> {
                    SettingController controller = loader.getController();
                    switch (option) {
                        case "profile" ->
                                controller.handleProfileInfo(null);
                        case "myAuctions" ->
                                controller.handleMyAuctions(null);
                        case "historyBid" ->
                                controller.handleHistoryBid(null);
                    }
                }
        );
    }
    private static void disposeCurrentPage() {
        if (currentItemController != null) {
            currentItemController.dispose();
            currentItemController = null;
        }
    }
    private static Parent wrapWithChatBox(Parent root) {
        StackPane container;
        if (root instanceof StackPane) {
            container = (StackPane) root;
        }
        else {
            container = new StackPane(root);
        }
        Node bubble = chatAiWidget.getBubble();
        Node chatBox = chatAiWidget.getChatBox();

        if (bubble.getParent() != null) {
            ((StackPane) bubble.getParent()).getChildren().removeAll(
                            chatBox,
                            bubble
                    );
        }
        container.getChildren().addAll(chatBox, bubble);
        StackPane.setAlignment(bubble, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bubble, new Insets(0, 30, 30, 0));
        StackPane.setAlignment(chatBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatBox, new Insets(0, 30, 30, 30));

        return container;
    }

    public static void clearCache() {
        disposeCurrentPage();
        homePageRoot = null;
        homePageController = null;
    }
}