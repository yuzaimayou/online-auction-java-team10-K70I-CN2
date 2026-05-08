package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.product.Item;
import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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

    @FXML
    private Label minimumBidLabel;

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

    //Các Label điều khiển đồng hồ
    @FXML
    private Label timeStatusLabel;
    @FXML
    private Label daysLabel;
    @FXML
    private Label hoursLabel;
    @FXML
    private Label minsLabel;
    @FXML
    private Label secsLabel;

    private static final Map<String, Double> userLastBidSession = new ConcurrentHashMap<>();
    private Timeline timeline;

    private static final double IMAGE_WIDTH = 700.0;
    private static final double IMAGE_HEIGHT = 450.0;
    private static final double IMAGE_ARC = 20.0;
    private static final double THUMB_WIDTH = 80.0;
    private static final double THUMB_HEIGHT = 60.0;
    private double myLastBid = 0;
    private final long AUTO_BID_DELAY = 50;
    private Item item;
    private final User user = UserSession.getInstance().getLoggedInUser();
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new Gson();

    // Biến điều khiển logic Auto Bid
    private boolean isAutoBidActive = false;
    private double maxBidAmount = 0;
    private double autoBidIncremental = 0;
    private long lastAutoBidTime = 0;


    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
        clip.setArcWidth(IMAGE_ARC);
        clip.setArcHeight(IMAGE_ARC);
        productImage.setClip(clip);

        productImage.setFitWidth(IMAGE_WIDTH);
        productImage.setFitHeight(IMAGE_HEIGHT);
        productImage.setPreserveRatio(false);

        productImage.imageProperty().addListener((obs, oldImg, newImg) -> {
            if (newImg != null) {
                applyObjectFitCover(newImg);
            }
        });
    }

    public void initData(Item item) {
        this.item = item;
        this.myLastBid = userLastBidSession.getOrDefault(item.getId(), 0.0);
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
                    currentPriceLabel.setText(String.format("$ " + item.getCurrentPrice()));

                    // Nếu người vừa đặt giá thành công là chính user này, cập nhật myLastBid
                    if (user.getId().equals(bidPayload.getUserId())) {
                        myLastBid = bidPayload.getBidPrice();
                        userLastBidSession.put(item.getId(), myLastBid);
                    }
                    // Cập nhật lại UI hiển thị (Minimum chạy theo Current Price, Your Bid giữ nguyên nếu người khác bid)
                    updateMinimumBidLabel();

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

    // Hàm kiểm tra trạng thái sản phẩm và hiển thị thông báo
    private boolean checkBiddableStatus() {
        // Giả sử model Item của bạn có phương thức getStatus() trả về String hoặc Enum
        // Nếu tên hàm getStatus() của bạn khác, hãy đổi lại cho đúng (VD: getState())
        String status = item.getStatus().toString().toUpperCase();

        if (status.equals("UPCOMING")) {
            showAlert(Alert.AlertType.WARNING, "Không thể đấu giá", "Sản phẩm này chưa tới thời gian đấu giá (Upcoming).");
            return false;
        } else if (status.equals("ENDED")) {
            showAlert(Alert.AlertType.WARNING, "Không thể đấu giá", "Cuộc đấu giá cho sản phẩm này đã kết thúc (Ended).");
            return false;
        }
        return true;
    }

    // Hàm tiện ích để hiển thị Alert
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void bidHandle() {

        if (!checkBiddableStatus()) {
            return;
        }

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
        applyObjectFitCoverToImageView(productImage, img, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private void applyObjectFitCoverToImageView(ImageView imageView, Image img, double targetW, double targetH) {
        if (img == null || imageView == null) return;

        if (img.getProgress() < 1.0) {
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0) {
                    Platform.runLater(() -> applyObjectFitCoverToImageView(imageView, img, targetW, targetH));
                }
            });
            return;
        }

        Platform.runLater(() -> {
            double imgW = img.getWidth();
            double imgH = img.getHeight();

            // Tránh lỗi chia cho 0 trong trường hợp ảnh bị lỗi hoặc không load được
            if (imgW == 0 || imgH == 0) return;

            double targetRatio = targetW / targetH;
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

            imageView.setViewport(new javafx.geometry.Rectangle2D(viewX, viewY, viewW, viewH));
        });
    }

    private void displayDataProduct(Item item) {
        thumbnailContainer.getChildren().clear();

        List<String> images = item.getImagesPath();

        if (images != null && !images.isEmpty()) {
            String mainImageUrl = images.get(0);
            ClientImageUtil.displayImage(mainImageUrl, "images", productImage, 200, 200);

            boolean isFirst = true;

            for (String imgPath : images) {
                if (imgPath == null || imgPath.trim().isEmpty()) continue;

                StackPane thumbPane = new StackPane();
                thumbPane.getStyleClass().add("thumbnail-container");
                thumbPane.setMinWidth(80);
                thumbPane.setMinHeight(60);
                thumbPane.setMaxWidth(80);
                thumbPane.setMaxHeight(60);

                if (isFirst) {
                    thumbPane.getStyleClass().add("active-thumb");
                    isFirst = false;
                }

                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(80);
                thumbView.setFitHeight(60);
                thumbView.setPreserveRatio(true);

                thumbView.setPreserveRatio(false);

                thumbView.imageProperty().addListener((obs, oldImg, newImg) -> {
                    if (newImg != null) {
                        applyObjectFitCoverToImageView(thumbView, newImg, THUMB_WIDTH, THUMB_HEIGHT);
                    }
                });

                ClientImageUtil.displayImage(imgPath, "images", thumbView, 200, 200);
                thumbPane.getChildren().add(thumbView);

                thumbPane.setOnMouseClicked(e -> {
                    Image clickedImage = thumbView.getImage();
                    if (clickedImage != null) {
                        productImage.setImage(clickedImage);
                    } else {
                        ClientImageUtil.displayImage(imgPath, "images", productImage, 200, 200);
                    }
                    thumbnailContainer.getChildren().forEach(node -> {
                        node.getStyleClass().remove("active-thumb");
                    });

                    // 2. Thêm class 'active-thumb' vào thumbnail vừa được click
                    thumbPane.getStyleClass().add("active-thumb");
                });

                thumbnailContainer.getChildren().add(thumbPane);
            }
        }

        double minHeight = 100;
        productDesLabel.setMinHeight(minHeight);
        productDesLabel.setText(item.getDescription());
        productDesLabel.setWrapText(true);
        productDesLabel.setMaxWidth(Double.MAX_VALUE);
        productDesLabel.prefWidthProperty().bind(
                productDesLabel.getParent().layoutBoundsProperty().map(b -> b.getWidth())
        );

        productDesLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        productDesLabel.setMinHeight(Region.USE_PREF_SIZE);
        productNameLabel.setText(item.getName());
        sellerLabel.setText(item.getSellerId());
        currentPriceLabel.setText(String.format("$ " + item.getCurrentPrice()));
        startPriceLabel.setText(String.valueOf(item.getStartingPrice()));
        bidStepLabel.setText(String.valueOf(item.getBidStep()));
        startTimeLabel.setText(String.valueOf(item.getStartTime()));
        endTimeLabel.setText(String.valueOf(item.getEndTime()));

        startCountdown();

        updateMinimumBidLabel();
    }

    private void updateMinimumBidLabel() {
        if (item != null && minimumBidLabel != null) {
            double minimumNextBid = item.getCurrentPrice() + item.getBidStep();
            // Cập nhật text: Hiển thị giá bid cuối cùng của User và Giá tối thiểu yêu cầu cho lượt tới
            minimumBidLabel.setText(String.format("Your last bid: $ %.0f ( min next: $ %.0f ) ", myLastBid, minimumNextBid));
        }
    }

    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }
        updateTimeDisplay();

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateTimeDisplay())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.playFromStart();
    }

    private void updateTimeDisplay() {
        if (item == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = null;
        String statusText = "Time Left:";

        // upcoming
        if (item.getStartTime() != null && now.isBefore(item.getStartTime())) {
            targetTime = item.getStartTime();
            statusText = "Starts in:";
        }
        // ongoing
        else if (item.getEndTime() != null && now.isBefore(item.getEndTime())) {
            targetTime = item.getEndTime();
            statusText = "Time Left:";
        }

        if (timeStatusLabel != null) {
            timeStatusLabel.setText(statusText);
        }
        // ended
        if (targetTime == null || !now.isBefore(targetTime)) {
            updateTimerLabels(0, 0, 0, 0);
            if (timeStatusLabel != null) {
                timeStatusLabel.setText("Ended");
            }
            if (timeline != null) timeline.stop();
        } else {
            long days = ChronoUnit.DAYS.between(now, targetTime);
            long hours = ChronoUnit.HOURS.between(now, targetTime) % 24;
            long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
            long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

            updateTimerLabels(days, hours, minutes, seconds);
        }
    }

    private void updateTimerLabels(long d, long h, long m, long s) {
        if (daysLabel != null) daysLabel.setText(String.format("%02d", d));
        if (hoursLabel != null) hoursLabel.setText(String.format("%02d", h));
        if (minsLabel != null) minsLabel.setText(String.format("%02d", m));
        if (secsLabel != null) secsLabel.setText(String.format("%02d", s));
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

        if (!checkBiddableStatus()) {
            return;
        }

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
            userLastBidSession.put(item.getId(), myLastBid);
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
