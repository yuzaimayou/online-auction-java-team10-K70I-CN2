package com.auction.client.util;

import com.auction.client.controller.common.HomePageController;
import com.auction.client.controller.auction.ItemPageController;
import com.auction.client.controller.auth.VerifyController;
import com.auction.client.controller.user.SettingController;

import com.auction.client.ui.chatBoxAi.ChatBoxInjector;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class NavigationUtil {

    public static void switchScene(Event event, String fxmlPath, String title) {
        switchScene(event, fxmlPath, title, null);
    }

    public static void switchScene(
            Event event,
            String fxmlPath,
            String title,
            Consumer<FXMLLoader> initController
    ) {
        try {
            PageCache.disposeCurrentItem();

            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource(fxmlPath)
            );

            Parent root = loader.load();

            if (initController != null) {
                initController.accept(loader);
            }

            Parent wrappedRoot = ChatBoxInjector.wrap(root);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.getScene().setRoot(wrappedRoot);
            stage.setTitle(title);

            Object controller = loader.getController();
            if (controller instanceof ItemPageController itemController) {
                PageCache.setCurrentItem(itemController);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void switchToLogin(Event event) {
        switchScene(event,
                "/com.auction.client/fxml/authenticator/Login.fxml",
                "Login - Auction System");
    }

    public static void switchToRegister(Event event) {
        switchScene(event,
                "/com.auction.client/fxml/authenticator/Register.fxml",
                "Register - Auction System");
    }

    public static void switchToOtpScreen(Event event, String email) {
        switchScene(event,
                "/com.auction.client/fxml/authenticator/Verify.fxml",
                "Verify OTP",
                loader -> {
                    VerifyController controller = loader.getController();
                    controller.setEmail(email);
                });
    }

    public static void handleSwitchToAuctionFormPage(Event event) {
        switchScene(event,
                "/com.auction.client/fxml/AuctionFormPage.fxml",
                AppConfig.getAppName() + " - Add Item");
    }

    public static void switchToItemPage(Event event, String itemId, String itemName) {
        switchScene(event,
                "/com.auction.client/fxml/ItemPage.fxml",
                AppConfig.getAppName() + " - " + itemName,
                loader -> {
                    ItemPageController controller = loader.getController();
                    controller.setItemId(itemId);
                });
    }

    public static void handleSwitchToHomePage(Label label) {
        try {
            PageCache.disposeCurrentItem();

            if (PageCache.getHomeRoot() == null) {
                FXMLLoader loader = new FXMLLoader(
                        NavigationUtil.class.getResource(
                                "/com.auction.client/fxml/HomePage.fxml"
                        )
                );
                Parent root = loader.load();
                HomePageController ctrl = loader.getController();
                PageCache.setHome(root, ctrl);
            } else {
                PageCache.getHomeController().refreshNavBarInfo();
                PageCache.getHomeController().refreshItems();
            }

            Parent wrappedRoot = ChatBoxInjector.wrap(PageCache.getHomeRoot());
            Stage stage = (Stage) label.getScene().getWindow();
            stage.getScene().setRoot(wrappedRoot);
            stage.setTitle(AppConfig.getAppName() + " - Home");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleSwitchToSetting(Event event) {
        handleSwitchToSetting(event, "");
    }

    public static void handleSwitchToSetting(Event event, String option) {
        switchScene(event,
                "/com.auction.client/fxml/setting/Setting.fxml",
                AppConfig.getAppName() + " - Profile",
                loader -> {
                    SettingController controller = loader.getController();

                    switch (option) {
                        case "profile" -> controller.handleProfileInfo(null);
                        case "myAuctions" -> controller.handleMyAuctions(null);
                    }
                });
    }
}