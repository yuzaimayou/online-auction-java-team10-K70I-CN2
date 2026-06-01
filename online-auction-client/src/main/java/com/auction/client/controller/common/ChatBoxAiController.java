package com.auction.client.controller.common;

import com.auction.client.service.ChatBotService;
import com.auction.client.ui.chatBoxAi.ChatMessageFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ChatBoxAiController {

    private StackPane bubble;
    private VBox chatBox;

    private static final double CHAT_WIDTH = 320;
    private static final double CHAT_HEIGHT = 450;
    private final ChatBotService chatBotService = ChatBotService.getInstance();
    private final BooleanProperty sendingMsg = new SimpleBooleanProperty(false);

    @FXML private TextArea txtMessage;
    @FXML private Button btnSend;
    @FXML private VBox messageArea;
    @FXML private ScrollPane chatScroll;

    public ChatBoxAiController() {}

    public static ChatBoxAiController create() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChatBoxAiController.class.getResource("/com.auction.client/fxml/ChatBoxAi.fxml"));

            VBox root = loader.load();
            ChatBoxAiController ctrl = loader.getController();

            // Cấu hình giao diện ban đầu
            ctrl.chatBox = root;
            ctrl.chatBox.setVisible(false);
            ctrl.chatBox.setManaged(false);
            ctrl.chatBox.setMaxWidth(CHAT_WIDTH);
            ctrl.chatBox.setMaxHeight(CHAT_HEIGHT);

            // Yêu cầu Factory tạo Bubble UI, truyền method reference để xử lý click
            ctrl.bubble = ChatMessageFactory.createChatBubble(ctrl::toggleChatBox);

            return ctrl;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void initialize() {
        BooleanBinding isMessageEmpty = Bindings.createBooleanBinding(
                () -> txtMessage.getText() == null || txtMessage.getText().trim().isEmpty(),
                txtMessage.textProperty()
        );
        btnSend.disableProperty().bind(isMessageEmpty.or(sendingMsg));
        txtMessage.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (!event.isShiftDown()) {
                    event.consume();
                    if (!btnSend.isDisabled()) sendMessage();
                }
            }
        });
    }

    @FXML
    public void sendMessage() {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) return;

        appendNode(ChatMessageFactory.createUserMessage(message));
        Node typingNode = ChatMessageFactory.createTypingIndicator();
        appendNode(typingNode);

        sendingMsg.set(true);
        txtMessage.clear();

        chatBotService.sendMsg(message)
                .thenAcceptAsync(response -> {
                    javafx.application.Platform.runLater(() -> {
                        messageArea.getChildren().remove(typingNode);

                        if (response == null) {
                            appendNode(ChatMessageFactory.createBotMessage("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại!"));
                            return;
                        }

                        String aiText = response.getAiResponse();
                        if (aiText != null && !aiText.isBlank()) {
                            appendNode(ChatMessageFactory.createBotMessage(aiText));
                        }

                        if (response.getItemSummaries() != null && !response.getItemSummaries().isEmpty()) {
                            appendNode(ChatMessageFactory.createItemCards(response.getItemSummaries()));
                        }
                    });
                })
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() -> {
                        messageArea.getChildren().remove(typingNode);
                        appendNode(ChatMessageFactory.createBotMessage("Không thể kết nối đến server. Vui lòng thử lại!"));
                    });
                    return null;
                })
                .whenComplete((res, ex) -> javafx.application.Platform.runLater(() -> sendingMsg.set(false)));
    }

    @FXML
    public void sendSuggest(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        txtMessage.setText(btn.getText());
        sendMessage();
    }

    @FXML
    public void closeChatBox() {
        chatBox.setVisible(false);
        chatBox.setManaged(false);
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

    private void appendNode(Node node) {
        messageArea.getChildren().add(node);
        chatScroll.applyCss();
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
    }

    // Getters
    public Node getBubble() { return bubble; }
    public Node getChatBox() { return chatBox; }
    public ChatBotService getChatBotService() { return chatBotService; }
}