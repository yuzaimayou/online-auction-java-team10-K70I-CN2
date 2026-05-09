package com.auction.client.controller.setting;

import com.auction.client.util.ClientImageUtil;
import com.auction.shared.model.item.Item;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class MyAuctionsTableHelper {

    /**
     * Hàm cấu hình toàn bộ các cột cho bảng Auction
     *
     * @param onEditAction   Cầu nối (Callback) để gọi hàm chuyển trang Edit bên Controller
     * @param onDeleteAction Cầu nối để gọi hàm Xóa bên Controller (THÊM MỚI CHỖ NÀY)
     */
    public static void setupTableColumns(
            TableColumn<Item, Item> productCol,
            TableColumn<Item, String> categoryCol,
            TableColumn<Item, String> statusCol,
            TableColumn<Item, String> priceCol,
            TableColumn<Item, LocalDateTime> endTimeCol,
            TableColumn<Item, Item> actionCol,
            EventHandler<ActionEvent> onEditAction,
            Consumer<Item> onDeleteAction) {

        //  TẮT TÍNH NĂNG SORT
        productCol.setSortable(false);
        categoryCol.setSortable(false);
        statusCol.setSortable(false);
        priceCol.setSortable(false);
        endTimeCol.setSortable(false);
        actionCol.setSortable(false);

        // PRODUCT
        productCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        productCol.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Tạo khung chứa ảnh
                    ImageView imageView = new ImageView();
                    imageView.setFitWidth(50); // Chỉnh chiều rộng ảnh thumbnail
                    imageView.setFitHeight(50);
                    imageView.setPreserveRatio(true);

                    // GỌI HÀM TẢI ẢNH CỦA BẠN
                    // Tham số "images" là tên thư mục chứa ảnh trên Server
                    if (item.getImagesPath() != null && !item.getImagesPath().isEmpty()) {
                        ClientImageUtil.displayImage(item.getImagesPath().get(0), "images", imageView, 200, 200);
                    }

                    // Tên sản phẩm
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

                    // Gộp ảnh và tên vào 1 hàng ngang (HBox)
                    HBox hBox = new HBox(15); // Khoảng cách giữa ảnh và chữ là 15
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(imageView, nameLabel);

                    setGraphic(hBox);
                    setText(null);
                }
            }
        });

        // CATEGORY
        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        categoryCol.setCellFactory(column -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(category);
                    label.getStyleClass().add("category-badge");

                    String cat = category.toLowerCase();
                    if (cat.contains("fashion")) label.getStyleClass().add("cat-fashion");
                    else if (cat.contains("electronics")) label.getStyleClass().add("cat-electronics");
                    else if (cat.contains("home")) label.getStyleClass().add("cat-home");
                    else if (cat.contains("art")) label.getStyleClass().add("cat-art");
                    else if (cat.contains("jewelry")) label.getStyleClass().add("cat-jewelry");
                    setGraphic(label);
                }
            }
        });

        //  STATUS
        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            LocalDateTime now = LocalDateTime.now();
            String status = "Ongoing";
            if (now.isBefore(item.getStartTime())) status = "Upcoming";
            else if (now.isAfter(item.getEndTime())) status = "Ended";
            return new SimpleStringProperty(status);
        });
        statusCol.setCellFactory(column -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(status);
                    label.getStyleClass().add("status-badge-base");
                    if ("Upcoming".equals(status)) label.getStyleClass().add("status-upcoming");
                    else if ("Ended".equals(status)) label.getStyleClass().add("status-ended");
                    else label.getStyleClass().add("status-live");
                    setGraphic(label);
                }
            }
        });

        //  PRICE
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty("$" + String.format("%,.0f", cellData.getValue().getCurrentPrice())));

        //  END TIME
        endTimeCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getEndTime()));
        endTimeCol.setCellFactory(column -> new TableCell<Item, LocalDateTime>() {
            private final Label dateLabel = new Label();
            private final Label timeLabel = new Label();
            private final VBox container = new VBox(dateLabel, timeLabel);

            {
                container.setSpacing(2);
                container.setAlignment(Pos.CENTER_LEFT);
                dateLabel.getStyleClass().add("end-time-date");
                timeLabel.getStyleClass().add("end-time-hour");
            }

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

                    dateLabel.setText(item.format(dateFormatter));
                    timeLabel.setText(item.format(timeFormatter));

                    setGraphic(container);
                }
            }
        });

        //  ACTION
        actionCol.setPrefWidth(220);
        actionCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        actionCol.setCellFactory(column -> new TableCell<Item, Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox actions = new HBox(8);
                    actions.setAlignment(Pos.CENTER);

                    Button editBtn = new Button("Edit");
                    editBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    editBtn.getStyleClass().add("action-btn-edit");
                    editBtn.setOnAction(onEditAction);

                    Button viewBtn = new Button("View");
                    viewBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    viewBtn.getStyleClass().add("action-btn-view");

                    Button deleteBtn = new Button("Delete");
                    deleteBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    deleteBtn.getStyleClass().add("action-btn-delete");

                    // THÊM SỰ KIỆN CHO NÚT DELETE: Truyền Item hiện tại ngược về Controller
                    deleteBtn.setOnAction(e -> {
                        if (onDeleteAction != null) {
                            onDeleteAction.accept(item);
                        }
                    });

                    actions.getChildren().addAll(editBtn, viewBtn, deleteBtn);
                    setGraphic(actions);
                }
            }
        });
    }
}