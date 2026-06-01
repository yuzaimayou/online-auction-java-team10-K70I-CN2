package com.auction.client.controller.common;

import com.auction.client.service.ChatBotService;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class ChatBoxAiController {
    private StackPane bubble;
    private VBox chatBox;

    private static final double CHAT_WIDTH = 320;
    private static final double CHAT_HEIGHT = 450;
    private final ChatBotService chatBotService = ChatBotService.getInstance();
    private final BooleanProperty sendingMsg = new SimpleBooleanProperty(false);
    @FXML
    private TextField txtMessage;
    @FXML
    private Button btnSend;

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

    @FXML
    private void initialize() {
        BooleanBinding isMessageEmpty = Bindings.createBooleanBinding(
                () -> txtMessage.getText() == null || txtMessage.getText().trim().isEmpty(),
                txtMessage.textProperty()
        );

        btnSend.disableProperty().bind(isMessageEmpty.or(sendingMsg));
    }

    @FXML
    public void sendMessage() {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        sendingMsg.set(true);

        System.out.println("Message sent to AI: " + message);
        txtMessage.clear();
        chatBotService.sendMsg(message)
                .thenAccept(response -> {
                    System.out.println("Response from AI: " + response.getData());
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                })
                .whenComplete((res, ex) -> sendingMsg.set(false));
    }

    public Node getBubble() {
        return bubble;
    }

    public Node getChatBox() {
        return chatBox;
    }

    public ChatBotService getChatBotService() {
        return chatBotService;
    }
}