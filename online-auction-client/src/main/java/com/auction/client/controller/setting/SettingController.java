package com.auction.client.controller.setting;

import com.auction.client.util.UserSession;
import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
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
    private Label lblUserName;

    // ĐÃ THÊM: Map với FXML để quản lý ẩn/hiện
    @FXML
    private VBox adminSection;
    @FXML
    private StackPane adminSignal;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && lblUserName != null) {
            lblUserName.setText(currentUser.getUsername());
            // Hiển thị Username hoặc FirstName tùy theo model của bạn
            boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin");

            if (adminSignal != null) {
                adminSignal.setVisible(isAdmin);
                adminSignal.setManaged(isAdmin);
            }

            if (adminSection != null) {
                adminSection.setVisible(isAdmin);
                adminSection.setManaged(isAdmin);
            }
        }
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
        } else {
            profileInfoBtn.setSelected(true);
            loadPage("/com.auction.client/fxml/setting/ProfilePage.fxml");
        }
        targetTab = "ProfileInfo";
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
        handleSwitchToLogin(event);

    }

    @FXML
    public void handleProfileInfo(ActionEvent event) {
        loadPage("/com.auction.client/fxml/setting/ProfilePage.fxml");
    }

    @FXML
    public void handleMyAuctions(ActionEvent event) {
        loadPage("/com.auction.client/fxml/setting/MyAuctionsPage.fxml");
    }

    @FXML
    public void handleHistoryBid(ActionEvent event) {
    }

    @FXML
    protected void handleSwitchToLogin(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com.auction.client/fxml/authenticator/Login.fxml"));

            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            stage.getScene().setRoot(loginRoot);

        } catch (IOException e) {
            System.err.println("Không tìm thấy file Login.fxml!");
            e.printStackTrace();
        }
    }

    public void handleMyBids(ActionEvent actionEvent) {
    }

    public void handleChangePassword(ActionEvent actionEvent) {
    }

    public void handleUsersManagement(ActionEvent actionEvent) {
    }

    public void handleAuctionsManagement(ActionEvent actionEvent) {
    }
}
