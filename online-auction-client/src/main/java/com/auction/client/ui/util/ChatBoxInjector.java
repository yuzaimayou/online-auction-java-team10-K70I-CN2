package com.auction.client.ui.util;

import com.auction.client.controller.common.ChatBoxAiController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class ChatBoxInjector {

    private static final ChatBoxAiController chatAiWidget = new ChatBoxAiController();

    private ChatBoxInjector() {}

    public static Parent wrap(Parent root) {
        StackPane container = (root instanceof StackPane sp) ? sp : new StackPane(root);

        Node bubble  = chatAiWidget.getBubble();
        Node chatBox = chatAiWidget.getChatBox();

        if (bubble.getParent() != null) {
            ((StackPane) bubble.getParent()).getChildren().removeAll(chatBox, bubble);
        }

        container.getChildren().addAll(chatBox, bubble);
        StackPane.setAlignment(bubble,  Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bubble,     new Insets(0, 30, 30, 0));
        StackPane.setAlignment(chatBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatBox,    new Insets(0, 30, 30, 30));

        return container;
    }
}