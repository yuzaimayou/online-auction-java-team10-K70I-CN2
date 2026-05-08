package com.auction.client.util;

import com.auction.client.controller.HomePageController;
import com.auction.client.controller.setting.SettingController;
import com.auction.client.controller.user.VerifyController;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
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

    public static void switchToOtpScreen(ActionEvent event, String registeredEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/com.auction.client/fxml/authenticator/Verify.fxml"));
            Node verifyNode = loader.load();
            VerifyController verifyController = loader.getController();
            verifyController.setEmail(registeredEmail);


            Node sourceNode = (Node) event.getSource();
            StackPane dynamicContentArea = (StackPane) sourceNode.getScene().lookup("#dynamicContentArea");

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

            if (homePageController == null) {
                // Lấy đường dẫn file FXML (Đảm bảo đường dẫn này đúng với thư mục resources)
                FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/com.auction.client/fxml/HomePage.fxml"));
                homePageRoot = loader.load();

                //Lay Controller
                homePageController = loader.getController();
                
            } else {
                homePageController.refreshProducts();
            }

            Scene currentScene = label.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(homePageRoot);
            stage.setTitle(String.format("%s - Home", AppConfig.getAppName()));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file HomePage.fxml! Kiểm tra lại đường dẫn.");
        }
    }


    public static void handleSwitchToSetting(Label label) {
        handleSwitchToSetting(label, "");
    }

    public static void handleSwitchToSetting(Label label, String option) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/com.auction.client/fxml/setting/Setting.fxml"));
            Parent root = loader.load();

            SettingController settingController = loader.getController();

            Scene currentScene = label.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            currentScene.setRoot(root);
            stage.setTitle(String.format("%s - Profile Page", AppConfig.getAppName()));

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
}
