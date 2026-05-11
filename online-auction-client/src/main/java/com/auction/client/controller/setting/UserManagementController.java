package com.auction.client.controller.setting;

import com.auction.client.model.UserModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.geometry.Insets;

public class UserManagementController {

    @FXML
    private TableView<UserModel> userTable;
    @FXML
    private TableColumn<UserModel, String> colUser;
    @FXML
    private TableColumn<UserModel, String> colEmail;
    @FXML
    private TableColumn<UserModel, String> colStatus;
    @FXML
    private TableColumn<UserModel, String> colRole;
    @FXML
    private TableColumn<UserModel, String> colJoinDate;
    @FXML
    private TableColumn<UserModel, Void> colAction;

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colUser.setCellValueFactory(data -> data.getValue().userNameProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colRole.setCellValueFactory(data -> data.getValue().roleProperty());
        colJoinDate.setCellValueFactory(data -> data.getValue().joinDateProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        applyLeftAlignment(colUser);
        applyLeftAlignment(colEmail);
        applyLeftAlignment(colRole);
        applyLeftAlignment(colJoinDate);

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label label = new Label(item);
                    label.getStyleClass().add(item.equalsIgnoreCase("Active") ? "status-active" : "status-suspended");
                    setGraphic(label);
                    setAlignment(Pos.CENTER_LEFT);
                    setPadding(new Insets(0, 0, 0, 15));
                }
            }
        });

        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button banBtn = new Button("Ban");
            {
                banBtn.getStyleClass().add("ban-btn");
                banBtn.setOnAction(event -> {
                    UserModel user = getTableView().getItems().get(getIndex());
                    System.out.println("Banning user: " + user.getUserName());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(banBtn);
                    setAlignment(Pos.CENTER_LEFT);
                    setPadding(new Insets(0, 0, 0, 15));
                }
            }
        });
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
    private void loadData() {
        ObservableList<UserModel> users = FXCollections.observableArrayList(
                new UserModel("user123", "user123@gmail.com", "Active", "User", "10/01/2025"),
                new UserModel("user456", "user456@gmail.com", "Active", "User", "11/01/2025"),
                new UserModel("john.doe", "john.doe@example.com", "Suspended", "User", "12/01/2025"),
                new UserModel("jane.smith", "jane.smith@example.com", "Active", "User", "13/01/2025"),
                new UserModel("bidmaster", "bidmaster@gmail.com", "Active", "Moderator", "14/01/2025")
        );
        userTable.setItems(users);
    }
}