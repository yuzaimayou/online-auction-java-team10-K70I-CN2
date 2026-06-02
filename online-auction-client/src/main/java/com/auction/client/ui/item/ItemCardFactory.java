package com.auction.client.ui.item;

import com.auction.client.controller.auction.ItemCardHPController;
import com.auction.shared.model.item.ItemSummary;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import java.io.IOException;

/**
 * Trách nhiệm: Khởi tạo và thiết lập các thuộc tính giao diện cho thẻ sản phẩm.
 * Tách biệt hoàn toàn logic render UI và quản lý style danh mục khỏi Controller.
 */
public class ItemCardFactory {

    private static final double CARD_WIDTH = 280;
    private static final String FXML_PATH = "/com.auction.client/fxml/ItemCardHP.fxml";

    public static VBox createCard(ItemSummary item) throws IOException {
        FXMLLoader loader = new FXMLLoader(ItemCardFactory.class.getResource(FXML_PATH));
        VBox cardBox = loader.load();

        cardBox.setPrefWidth(CARD_WIDTH);
        cardBox.setMinWidth(CARD_WIDTH);
        cardBox.setMaxWidth(CARD_WIDTH);

        cardBox.setCache(true);
        cardBox.setCacheShape(true);

        ItemCardHPController cardController = loader.getController();
        cardController.setData(item);

        return cardBox;
    }

    /**
     * Đồng bộ lớp CSS hoạt động cho nút danh mục được bấm chọn.
     * Đảm bảo tính đóng gói mã giao diện đồ họa.
     */
    public static void updateCategoryUIStyle(VBox clickedBox, String currentCategory) {
        if (clickedBox.getParent() == null) return;

        for (Node node : clickedBox.getParent().getChildrenUnmodifiable()) {
            if (node instanceof VBox) {
                node.getStyleClass().remove("active-category");
            }
        }
        if (!"ALL".equals(currentCategory)) {
            clickedBox.getStyleClass().add("active-category");
        }
    }
}