package com.auction.client.controller.setting;

import com.auction.client.util.NavigationUtil;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingController {
    //Constants
    private static final String PATH_PROFILE = "/com.auction.client/fxml/setting/ProfilePage.fxml";
    private static final String PATH_MY_AUCTIONS = "/com.auction.client/fxml/setting/MyAuctionsPage.fxml";
    private static final String PATH_HISTORY = "/com.auction.client/fxml/HistoryBidPage.fxml";
    private static final String PATH_LOGIN = "/com.auction.client/fxml/authenticator/Login.fxml";
    private static final String PATH_USERS_MANAGEMENT = "/com.auction.client/fxml/setting/UserManagementPage.fxml";
    private static final String PATH_AUCTIONS_MANAGEMENT = "/com.auction.client/fxml/setting/AuctionsManagementPage.fxml";
    private static final String PATH_MY_BIDS = "/com.auction.client/fxml/setting/MyBids.fxml";

    // State
    public static String targetTab = "ProfileInfo";
    private static volatile SettingController instance;

    @FXML
    private VBox dynamicContent;
    @FXML
    private Label lblUserName;
    @FXML
    private ToggleButton profileInfoBtn;
    @FXML
    private ToggleGroup menuGroup;
    @FXML
    private ToggleButton myAuctionsBtn;
    @FXML
    private ToggleButton historyBidBtn;

    @FXML
    private VBox adminSection;
    @FXML
    private StackPane adminSignal;

    public SettingController() { instance = this; }
    public static SettingController getInstance() { return instance; }

    @FXML
    public void initialize() {
        setupUserContext();
        setupToggleGroupBehavior();
        initialNavigation();
    }
    private void setupUserContext() {

        User currentUser =
                UserSession.getInstance().getLoggedInUser();

        if (currentUser == null) {
            return;
        }

        lblUserName.setText(currentUser.getUsername());

        // boolean isAdmin = UserSession.getInstance().isAdmin();
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getUsername());

        adminSignal.setVisible(isAdmin);
        adminSignal.setManaged(isAdmin);

        adminSection.setVisible(isAdmin);
        adminSection.setManaged(isAdmin);
    }
    private void setupToggleGroupBehavior() {
        menuGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                menuGroup.selectToggle(oldToggle);
            }
        });
    }
    private void initialNavigation() {
        switch (targetTab) {
            case "MyAuctions": myAuctionsBtn.setSelected(true);loadPage(PATH_MY_AUCTIONS);
                break;
            case "HistoryBid": historyBidBtn.setSelected(true);loadPage(PATH_HISTORY);
                break;
            default: profileInfoBtn.setSelected(true);loadPage(PATH_PROFILE);
                break;
        }
        targetTab = "ProfileInfo";
    }

    public void setDynamicContent(String fxmlPath) {
        loadPage(fxmlPath);
    }
    private void loadPage(String fxmlPath) {

        try {
            dynamicContent.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            VBox.setVgrow(page, Priority.ALWAYS);
            dynamicContent.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không tìm thấy file: " + fxmlPath);
        }
    }
    @FXML
    public void handleProfileInfo(ActionEvent event) {
        loadPage(PATH_PROFILE);;
    }
    @FXML
    public void handleMyAuctions(ActionEvent event) {
        loadPage(PATH_MY_AUCTIONS);
    }
    @FXML
    public void handleHistoryBid(ActionEvent event) {
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        instance = null;
        NavigationUtil.switchToLogin(event);
    }
    @FXML
    public void handleMyBids(ActionEvent actionEvent) {
        loadPage(PATH_MY_BIDS);
    }
    @FXML
    public void handleUsersManagement(ActionEvent actionEvent) {
        loadPage(PATH_USERS_MANAGEMENT);
    }
    @FXML
    public void handleAuctionsManagement(ActionEvent actionEvent) {
        loadPage(PATH_AUCTIONS_MANAGEMENT);
    }

    public VBox getDynamicContent() {
        return dynamicContent;
    }
}
