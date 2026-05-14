package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class ChatBoxAiController {
    private StackPane bubble;
    private VBox chatBox;

    private static final double CHAT_WIDTH = 320;
    private static final double CHAT_HEIGHT = 450;

    public ChatBoxAiController() {
        createBubble();
        createChatBox();
    }

    private void createBubble() {
        Circle circle = new Circle(30, Color.web("#4485f4"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        Label icon = new Label("✦");
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold;");

        bubble = new StackPane(circle, icon);
        bubble.setAlignment(Pos.CENTER);
        bubble.setPrefSize(60, 60);
        bubble.setMaxSize(60, 60);
        bubble.setStyle("-fx-cursor: hand;");
        bubble.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0, 0.5)));

        bubble.setOnMouseClicked(event -> toggleChatBox());
    }

    private void createChatBox() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com.auction.client/fxml/ChatBoxAi.fxml"));
            chatBox = loader.load();
            chatBox.setVisible(false);
            chatBox.setManaged(false);

            chatBox.setMaxWidth(CHAT_WIDTH);
            chatBox.setMaxHeight(CHAT_HEIGHT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleChatBox() {
        boolean isVisible = chatBox.isVisible();
        chatBox.setVisible(!isVisible);
        chatBox.setManaged(!isVisible);

        if (!isVisible) {
            chatBox.setTranslateX(-10);
            chatBox.setTranslateY(-70);
        }
    }

    public Node getBubble() { return bubble; }
    public Node getChatBox() { return chatBox; }
}