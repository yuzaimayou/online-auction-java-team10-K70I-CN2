package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.product.Item;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;


public class ProductPageController implements NetworkService.MessageListener {
    @FXML
    private Label productNameLabel;
    @FXML
    private Label productDesLabel;
    @FXML
    private ImageView productImage;
    @FXML
    private Label sellerLabel;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label startPriceLabel;
    @FXML
    private Label bidStepLabel;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label endTimeLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button submitBid;
    @FXML
    private HBox thumbnailContainer;

    // Auto Bid UI
    @FXML
    private VBox autoBidForm;
    @FXML
    private VBox autoBidActiveStatus;
    @FXML
    private TextField maxBidField;
    @FXML
    private TextField autoBidStepField;
    @FXML
    private Label userCurrentBidLabel;
    @FXML
    private Button btnAutoBidToggle;

    private static final double IMAGE_WIDTH = 700.0;
    private static final double IMAGE_HEIGHT = 450.0;
    private static final double IMAGE_ARC = 20.0;
    private final long AUTO_BID_DELAY = 50;
    private Item item;
    private final User user = UserSession.getInstance().getLoggedInUser();
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new Gson();

    // Biến điều khiển logic Auto Bid
    private boolean isAutoBidActive = false;
    private double maxBidAmount = 0;
    private double autoBidIncremental = 0;
    private double myLastBid = 0;
    private long lastAutoBidTime = 0;


    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
        clip.setArcWidth(IMAGE_ARC);
        clip.setArcHeight(IMAGE_ARC);
        productImage.setClip(clip);

        productImage.setFitWidth(IMAGE_WIDTH);
        productImage.setFitHeight(IMAGE_HEIGHT);
        productImage.setPreserveRatio(true);

        productImage.imageProperty().addListener((obs, oldImg, newImg) -> {
            if (newImg != null) {
                applyObjectFitCover(newImg);
            }
        });
    }

    public void initData(Item item) {
        this.item = item;
        displayDataProduct(item);
        connectToRealTimeBidding();
        if (item.getSellerId().equals(user.getId())) {
            bidAmountField.setDisable(true);
            submitBid.setDisable(true);
            btnAutoBidToggle.setDisable(true);
        }
    }

    private void connectToRealTimeBidding() {
        network.setListener(this);

        String userId = UserSession.getInstance().getLoggedInUser().getId();
        String itemId = item.getId();

        boolean connected = network.connectToAuctionRoom(itemId, userId);
        if (connected) {
            System.out.println("Connected to auction room for item: " + itemId);
        } else {
            System.err.println("Failed to connect to auction room for item: " + itemId);
        }
    }

    @Override
    public void onMessageReceived(ResponseMessage response) {
        System.out.println(response);
        Platform.runLater(() -> {
            if ("success".equals(response.getStatus()) && "NEW_BID".equals(response.getMessage())) {
                String jsonPayload = response.getData();
                BidPayload bidPayload = gson.fromJson(jsonPayload, BidPayload.class);
                if (bidPayload != null) {
                    System.out.println("Received new bid update: " + jsonPayload);
                    item.setCurrentPrice(bidPayload.getBidPrice());
                    currentPriceLabel.setText(String.format("Current bid: %.0f", item.getCurrentPrice()));

                    // Kích hoạt logic Auto Bid nếu đang bật
                    handleAutoBidLogic(bidPayload.getBidPrice(), bidPayload.getUserId());

                } else {
                    System.err.println("Failed to parse updated item from response: " + jsonPayload);
                }
            } else {

                System.out.println("Received message: " + response.getMessage());
            }
        });

    }

    public void bidHandle() {
        String inputAmount = bidAmountField.getText().trim();

        if (inputAmount.isEmpty()) {
            System.out.println("Please enter a bid amount.");
            return;
        }
        try {
            double bidAmount = Double.parseDouble(inputAmount);
            String roomId = item.getId();

            network.sendBid(roomId, user.getId(), bidAmount, "");
            System.out.println("Bid sent: " + bidAmount);
        } catch (NumberFormatException e) {
            System.out.println("Invalid bid amount. Please enter a numeric value.");
        }

    }

    private void applyObjectFitCover(Image img) {
        if (img == null) return;

        // Khi ảnh load bất đồng bộ, Width/Height ban đầu có thể là 0.
        if (img.getProgress() < 1.0) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0) {
                    Platform.runLater(() -> applyObjectFitCover(img));
                }
            });
            return;
        }

        Platform.runLater(() -> {
            double imgW = img.getWidth();
            double imgH = img.getHeight();

            // Tránh lỗi chia cho 0 trong trường hợp ảnh bị lỗi hoặc không load được
            if (imgW == 0 || imgH == 0) return;

            double targetRatio = IMAGE_WIDTH / IMAGE_HEIGHT;
            double sourceRatio = imgW / imgH;
            double viewW, viewH, viewX, viewY;

            if (sourceRatio > targetRatio) {
                viewH = imgH;
                viewW = imgH * targetRatio;
                viewX = (imgW - viewW) / 2;
                viewY = 0;
            } else {
                viewW = imgW;
                viewH = imgW / targetRatio;
                viewX = 0;
                viewY = (imgH - viewH) / 2;
            }

            productImage.setViewport(new javafx.geometry.Rectangle2D(viewX, viewY, viewW, viewH));
        });
    }

    private void displayDataProduct(Item item) {
        thumbnailContainer.getChildren().clear();

        List<String> images = item.getImagesPath();

        if (images != null && !images.isEmpty()) {
            String mainImageUrl = images.get(0);
            ClientImageUtil.displayImage(mainImageUrl, "images", productImage);

            for (String imgPath : images) {
                if (imgPath == null || imgPath.trim().isEmpty()) continue;

                StackPane thumbPane = new StackPane();
                thumbPane.getStyleClass().add("thumbnail-container");
                thumbPane.setMinWidth(80);
                thumbPane.setMinHeight(60);
                thumbPane.setMaxWidth(80);
                thumbPane.setMaxHeight(60);

                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(80);
                thumbView.setFitHeight(60);
                thumbView.setPreserveRatio(true);

                ClientImageUtil.displayImage(imgPath, "images", thumbView);
                thumbPane.getChildren().add(thumbView);

                thumbPane.setOnMouseClicked(e -> {
                    Image clickedImage = thumbView.getImage();
                    if (clickedImage != null) {
                        productImage.setImage(clickedImage);
                    } else {
                        ClientImageUtil.displayImage(imgPath, "images", productImage);
                    }
                });

                thumbnailContainer.getChildren().add(thumbPane);
            }
        }

        productDesLabel.setText(item.getDescription());
        productNameLabel.setText(item.getName());
        sellerLabel.setText(item.getSellerId());
        currentPriceLabel.setText(String.format("Current bid: %.0f", item.getCurrentPrice()));
        startPriceLabel.setText(String.valueOf(item.getStartingPrice()));
        bidStepLabel.setText(String.valueOf(item.getBidStep()));
        startTimeLabel.setText(String.valueOf(item.getStartTime()));
        endTimeLabel.setText(String.valueOf(item.getEndTime()));
    }

    // Ẩn/Hiện form nhập liệu khi nhấn nút Auto Bid
    @FXML
    private void toggleAutoBidForm() {
        boolean isVisible = autoBidForm.isVisible();
        autoBidForm.setVisible(!isVisible);
        autoBidForm.setManaged(!isVisible);
    }

    // Bắt đầu chế độ tự động
    @FXML
    private void startAutoBid() {
        try {
            maxBidAmount = Double.parseDouble(maxBidField.getText().trim());
            autoBidIncremental = Double.parseDouble(autoBidStepField.getText().trim());

            if (maxBidAmount <= item.getCurrentPrice()) {
                System.out.println("Max bid phải lớn hơn giá hiện tại!");
                return;
            }

            isAutoBidActive = true;

            myLastBid = item.getCurrentPrice();

            updateAutoBidUI(true);

            // Kiểm tra ngay lập tức xem có cần bid luôn không
            handleAutoBidLogic(item.getCurrentPrice(), null);

        } catch (NumberFormatException e) {
            System.out.println("Vui lòng nhập số hợp lệ cho Auto Bid");
        }
    }

    // Dừng chế độ tự động
    @FXML
    private void stopAutoBid() {
        isAutoBidActive = false;
        updateAutoBidUI(false);
    }

    // Tự động tính toán giá khi có giá mới
    private void handleAutoBidLogic(double serverCurrentPrice, String lastBidderId) {
        if (!isAutoBidActive) return;

        // Nếu mình đã là người cao nhất thì không cần bid thêm
        if (user.getId().equals(lastBidderId)) {
            myLastBid = serverCurrentPrice;
            userCurrentBidLabel.setText(String.format("Your current bid: %.0f VNĐ (Leading)", serverCurrentPrice));
            return;
        }

        // Tính toán giá tiếp theo của mình
        double myNextBid = serverCurrentPrice + autoBidIncremental;

        if (myNextBid <= maxBidAmount) {
            long now = System.currentTimeMillis();
            if (now - lastAutoBidTime < AUTO_BID_DELAY) return; // chống spam
            lastAutoBidTime = now;
            network.sendBid(item.getId(), user.getId(), myNextBid, "");
            myLastBid = myNextBid;
            System.out.println("Auto Bid thực hiện đặt giá: " + myNextBid);
            userCurrentBidLabel.setText(String.format("Your current bid: %.0f VNĐ", myNextBid));
        } else {
            System.out.println("Giá đã vượt quá hạn mức Max Bid của bạn!");
            stopAutoBid();
        }
    }

    // Cập nhật hiển thị giao diện chuyển đổi trạng thái
    private void updateAutoBidUI(boolean active) {
        autoBidForm.setVisible(false);
        autoBidForm.setManaged(false);

        autoBidActiveStatus.setVisible(active);
        autoBidActiveStatus.setManaged(active);

        btnAutoBidToggle.setVisible(!active);
        btnAutoBidToggle.setManaged(!active);

        // Disable nút đặt giá tay khi đang chạy auto để tránh xung đột
        submitBid.setDisable(active || item.getSellerId().equals(user.getId()));
    }


}
