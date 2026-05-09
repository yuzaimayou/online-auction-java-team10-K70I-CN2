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
    public void initialize() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && lblUserName != null) {
            // Hiển thị Username hoặc FirstName tùy theo model của bạn
            boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin");

            System.out.println("=== DEBUG LOGIC USERNAME ===");
            System.out.println("User đang đăng nhập: " + currentUser.getUsername());
            System.out.println("Có phải admin hệ thống không: " + isAdmin);
            System.out.println("============================");

            if (adminSection != null) {
                // setVisible: Ẩn/hiện về mặt hình ảnh
                adminSection.setVisible(isAdmin);
                // setManaged: Nếu false, layout sẽ coi như VBox này không tồn tại và thu gọn khoảng trống
                adminSection.setManaged(isAdmin);
            }
            // -------------------------
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
