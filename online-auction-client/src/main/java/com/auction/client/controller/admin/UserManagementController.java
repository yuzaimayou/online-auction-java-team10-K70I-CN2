package com.auction.client.controller.admin;

import com.auction.client.service.UserService;
import com.auction.client.ui.table.UserTableFactory;
import com.auction.client.util.AlertUtil;
import com.auction.client.service.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UserManagementController {

    @FXML
    private TableView<UserRowViewModel> userTable;
    @FXML
    private TableColumn<UserRowViewModel, String> colUser;
    @FXML
    private TableColumn<UserRowViewModel, String> colEmail;
    @FXML
    private TableColumn<UserRowViewModel, String> colStatus;
    @FXML
    private TableColumn<UserRowViewModel, String> colRole;
    @FXML
    private TableColumn<UserRowViewModel, Void> colAction;

    private final UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colUser.setCellValueFactory(data -> data.getValue().userNameProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colRole.setCellValueFactory(data -> data.getValue().roleProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        colUser.setCellFactory(UserTableFactory.leftAlignCell());
        colEmail.setCellFactory(UserTableFactory.leftAlignCell());
        colRole.setCellFactory(UserTableFactory.roleCell());
        colStatus.setCellFactory(UserTableFactory.statusBadgeCell());
        colAction.setCellFactory(UserTableFactory.banActionCell(this::handleBanAction));
    }

    private void loadData() {
        userService.getAllUsers()
                .thenAccept(users -> Platform.runLater(() ->
                        userTable.setItems(FXCollections.observableArrayList(users))
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Lỗi kết nối", "Không thể tải danh sách: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleBanAction(UserRowViewModel user) {
        boolean confirmed = AlertUtil.showConfirm(
                "Xác nhận",
                "Bạn có chắc chắn muốn khóa người dùng: " + user.getUserName() + "? Mọi Auto Bid của họ sẽ bị hủy."
        );
        if (!confirmed) return;

        String adminId = UserSession.getInstance().getCurrentUserId();
        userService.banUser(user.getId(), adminId)
                .thenAccept(isSuccess -> Platform.runLater(() -> {
                    if (isSuccess) {
                        user.setStatus("Suspended");
                        user.setRole("banned_user");

                        userTable.refresh();
                        AlertUtil.showInfo("Thành công", "Tài khoản đã được khóa.");
                    } else {
                        AlertUtil.showError("Lỗi", "Không thể khóa người dùng từ phía Server.");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> AlertUtil.showError("Lỗi kết nối", ex.getMessage()));
                    return null;
                });
    }
}