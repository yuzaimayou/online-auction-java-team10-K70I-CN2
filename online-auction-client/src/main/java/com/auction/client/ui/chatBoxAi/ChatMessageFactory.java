package com.auction.client.ui.chatBoxAi;

import com.auction.shared.message.AIResponseData;
import com.auction.shared.model.item.ItemSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class ChatMessageFactory {

    /**
     * Bubble tin nhắn của User
     */
    public static Node createUserMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(220);
        lbl.setStyle(
                "-fx-background-color: #4485f4;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 18 18 4 18;" +
                        "-fx-padding: 10 14;"
        );

        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    /**
     * Bubble tin nhắn của Bot
     */
    public static Node createBotMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(220);
        lbl.setTextAlignment(TextAlignment.LEFT);
        lbl.setStyle(
                "-fx-background-color: #f0f0f0;" +
                        "-fx-text-fill: #333;" +
                        "-fx-background-radius: 18 18 18 4;" +
                        "-fx-padding: 10 14;"
        );

        // Avatar bot nhỏ bên trái
        Circle avatar = new Circle(14, Color.web("#1a2a47"));
        Label icon = new Label("✦");
        icon.setStyle("-fx-text-fill: white; -fx-font-size: 10;");
        javafx.scene.layout.StackPane avatarPane =
                new javafx.scene.layout.StackPane(avatar, icon);
        avatarPane.setAlignment(Pos.CENTER);

        HBox row = new HBox(8, avatarPane, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static Node createTypingIndicator() {
        Label lbl = new Label("● ● ●");
        lbl.setStyle(
                "-fx-text-fill: #999;" +
                        "-fx-font-size: 14;" +
                        "-fx-background-color: #f0f0f0;" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 10 14;"
        );
        lbl.setId("typingIndicator");

        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setId("typingRow");
        return row;
    }

    /**
     * Cards danh sách sản phẩm gợi ý (ItemSummary list)
     * Hiển thị dưới dạng VBox chứa các card nhỏ
     */
    public static Node createItemCards(List<ItemSummary> items) {
        VBox container = new VBox(8);
        container.setMaxWidth(240);

        for (ItemSummary item : items) {
            VBox card = new VBox(4);
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #e0e0e0;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 6, 0, 0, 2);" +
                            "-fx-cursor: hand;"
            );

            Label nameLbl = new Label(item.getName());
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #1a2a47;");
            nameLbl.setWrapText(true);


            card.getChildren().addAll(nameLbl);
            container.getChildren().add(card);
        }

        HBox row = new HBox(container);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, 36));
        return row;
    }

    /** * Tạo nút Bubble (icon nổi) ở góc màn hình
     * @param onClickAction Hành động sẽ thực thi khi user click vào bubble
     */
    public static StackPane createChatBubble(Runnable onClickAction) {
        Circle circle = new Circle(30, Color.web("#4485f4"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        Label icon = new Label("✦");
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold;");

        StackPane bubble = new StackPane(circle, icon);
        bubble.setAlignment(Pos.CENTER);
        bubble.setPrefSize(60, 60);
        bubble.setMaxSize(60, 60);
        bubble.setStyle("-fx-cursor: hand;");
        bubble.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0, 0.5)));

        bubble.setOnMouseClicked(event -> onClickAction.run());

        return bubble;
    }
}