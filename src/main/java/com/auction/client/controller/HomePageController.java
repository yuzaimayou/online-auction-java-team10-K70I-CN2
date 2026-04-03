package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class HomePageController {
    @FXML
    private Label labelUsername;
    @FXML
    private StackPane formAddAuction;

    public HomePageController() {
    }

    @FXML
    public void initialize() {
        System.out.println("Đã vào trang chủ!");
    }

    @FXML
    public void addAuction() {
        try {
            System.out.println("Tao phien dau gia moi");
            Parent formRoot = FXMLLoader.load(getClass().getResource("/com.auction.client/fxml/FormAddAuction.fxml"));
            if (formAddAuction != null) {
                formAddAuction.getChildren().clear();
                formAddAuction.getChildren().add(formRoot);
            } else {
                System.err.println("Error: Khong tim thay StackPane");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed");
        }


    }

    public void initData(String userName, String role) {
        if (userName != null) {
            labelUsername.setText(userName);

        }
    }
}