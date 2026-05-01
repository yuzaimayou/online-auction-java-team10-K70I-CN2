package com.auction.client.controller.setting;

import com.auction.client.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingController {

    public static String targetTab = "ProfileInfo";

    @FXML
    private VBox dynamicContent;
    @FXML
    private ToggleButton profileInfoBtn;
    @FXML
    private ToggleGroup menuGroup;
    @FXML
    private ToggleButton myAuctionsBtn;
    @FXML
    private ToggleButton historyBidBtn;

    @FXML
    public void initialize() {
        // Lắng nghe thay đổi của ToggleGroup
        menuGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                menuGroup.selectToggle(oldToggle);
            }
        });

        if ("MyAuctions".equals(targetTab)) {
            myAuctionsBtn.setSelected(true);
            loadPage("/com.auction.client/fxml/setting/MyAuctionsPage.fxml");
        } else if ("HistoryBid".equals(targetTab)) {
            historyBidBtn.setSelected(true);
            // Bạn có thể mở comment dòng dưới khi đã có file HistoryBidPage.fxml
            // loadPage("/com.auction.client/fxml/setting/HistoryBidPage.fxml");
        } else {
            profileInfoBtn.setSelected(true);
            loadPage("/com.auction.client/fxml/setting/ProfilePage.fxml");
        }
    }

    private void loadPage(String fxmlPath) {
        dynamicContent.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            dynamicContent.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file: " + fxmlPath);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        handleSwitchToAuthenPage(event);

    }

    @FXML
    public void handleProfileInfo(ActionEvent event) {
        targetTab = "ProfileInfo"; // ---> CODE MỚI: Lưu lại trạng thái
        loadPage("/com.auction.client/fxml/setting/ProfilePage.fxml");
    }

    @FXML
    public void handleMyAuctions(ActionEvent event) {
        targetTab = "MyAuctions"; // ---> CODE MỚI: Lưu lại trạng thái
        loadPage("/com.auction.client/fxml/setting/MyAuctionsPage.fxml");
    }

    @FXML
    public void handleHistoryBid(ActionEvent event) {
        targetTab = "HistoryBid"; // ---> CODE MỚI: Lưu lại trạng thái
        // loadPage("/com.auction.client/fxml/setting/HistoryBidPage.fxml");
    }

    @FXML
    public void handleSwitchToAuthenPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com.auction.client/fxml/authenticator/AuthPage.fxml")
            );
            Parent root = loader.load();

            Node sourceNode = (Node) event.getSource();
            Scene currentScene = sourceNode.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            stage.setTitle("Online Auction System - Authentication");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file AuthPage.fxml! Kiểm tra lại đường dẫn.");
        }
    }
}
