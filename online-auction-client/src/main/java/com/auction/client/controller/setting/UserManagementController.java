package com.auction.client.controller.setting;

import com.auction.client.model.UserModel;
import com.auction.client.network.HttpClientProvider;
import com.auction.client.util.UserSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.geometry.Insets;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

public class UserManagementController {

    @FXML private TableView<UserModel> userTable;
    @FXML private TableColumn<UserModel, String> colUser;
    @FXML private TableColumn<UserModel, String> colEmail;
    @FXML private TableColumn<UserModel, String> colStatus;
    @FXML private TableColumn<UserModel, String> colRole;
    @FXML private TableColumn<UserModel, Void> colAction;

    private final Gson gson = new Gson();
    private static final String BASE_URL = "http://localhost:8080/api";

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

        applyLeftAlignment(colUser);
        applyLeftAlignment(colEmail);
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("banned_user".equalsIgnoreCase(item) ? "User" : item);
                    setAlignment(Pos.CENTER_LEFT);
                    setPadding(new Insets(0, 0, 0, 15));
                }
            }
        });

        // UI Badge cho Status (Active / Suspended)
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    if ("Active".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #137333; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle("-fx-background-color: #FCE8E6; -fx-text-fill: #C5221F; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                    setPadding(new Insets(0, 0, 0, 15));
                }
            }
        });

        // Xử lý nút Ban logic chặt chẽ
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button banBtn = new Button("Ban");
            {
                banBtn.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DADCE0; -fx-border-radius: 4; -fx-cursor: hand;");
                banBtn.setOnAction(event -> {
                    UserModel user = getTableView().getItems().get(getIndex());
                    handleBanAction(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                UserModel user = getTableRow().getItem();
                String currentAdminId = UserSession.getInstance().getCurrentUserId();

                boolean isSelf   = user.getId() != null && user.getId().equals(currentAdminId);
                boolean isAdmin  = "Admin".equalsIgnoreCase(user.getRole());
                boolean isBanned = isBannedUser(user);

                setGraphic((isSelf || isAdmin || isBanned) ? null : banBtn);
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new Insets(0, 0, 0, 15));
            }
        });
    }

    private boolean isBannedUser(UserModel user) {
        return "Suspended".equalsIgnoreCase(user.getStatus())
                || "banned_user".equalsIgnoreCase(user.getRole());
    }

    private void applyLeftAlignment(TableColumn<UserModel, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_LEFT);
                    setPadding(new Insets(0, 0, 0, 15));
                }
            }
        });
    }

    private void handleBanAction(UserModel user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Khóa tài khoản: " + user.getUserName());
        confirm.setContentText("Bạn có chắc chắn muốn khóa người dùng này? Mọi Auto Bid của họ sẽ bị hủy.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            executeBanRequest(user);
        }
    }

    private void executeBanRequest(UserModel user) {
        String adminId = UserSession.getInstance().getCurrentUserId();

        JsonObject payload = new JsonObject();
        payload.addProperty("targetUserId", user.getId());
        if (adminId != null) {
            payload.addProperty("adminId", adminId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                // Lưu ý: Đảm bảo URI khớp với mapping bạn tạo trong MainServer. Ví dụ /api/users/ban
                .uri(URI.create(BASE_URL + "/users/ban"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        // Sử dụng HttpClientProvider tập trung để gọi API bất đồng bộ
        HttpClientProvider.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setStatus("Suspended");
                        user.setRole("banned_user");
                        userTable.getColumns().get(0).setVisible(false);
                        userTable.getColumns().get(0).setVisible(true);
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tài khoản đã được khóa.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể khóa người dùng. HTTP " + response.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", ex.getMessage()));
                    return null;
                });
    }

    private void loadData() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .GET()
                .build();

        HttpClientProvider.get().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    System.out.println("HTTP Status: " + response.statusCode());
                    System.out.println("Response body: " + response.body()); // log để kiểm tra
                    if (response.statusCode() == 200) {
                        Type listType = new TypeToken<List<com.google.gson.JsonObject>>() {
                        }.getType();
                        List<com.google.gson.JsonObject> raw = gson.fromJson(response.body(), listType);
                        List<UserModel> users = new java.util.ArrayList<>();
                        for (com.google.gson.JsonObject obj : raw) {
                            users.add(new UserModel(
                                    obj.has("id") ? obj.get("id").getAsString() : "",
                                    obj.has("username") ? obj.get("username").getAsString() : "",
                                    obj.has("email") ? obj.get("email").getAsString() : "",
                                    obj.has("status") ? obj.get("status").getAsString() : "Active",
                                    obj.has("role") ? obj.get("role").getAsString() : ""
                            ));
                        }
                        userTable.setItems(FXCollections.observableArrayList(users));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Server trả về HTTP " + response.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                            "Không thể kết nối server: " + ex.getMessage()));
                    return null;
                });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}