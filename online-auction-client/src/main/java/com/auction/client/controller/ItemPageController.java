package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.util.GsonUtil;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.shared.model.auction.BidTransaction;
import javafx.scene.control.Hyperlink;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.Comparator;


public class ItemPageController implements NetworkService.MessageListener {
    private static final double IMAGE_WIDTH = 700.0;
    private static final double IMAGE_HEIGHT = 450.0;
    private static final double IMAGE_ARC = 20.0;
    private static final double THUMB_WIDTH = 80.0;
    private static final double THUMB_HEIGHT = 60.0;
    @FXML
    private Label itemNameLabel;
    @FXML
    private Label itemDesLabel;
    @FXML
    private ImageView itemImage;
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
    @FXML
    private VBox historyBidContainer;
    @FXML
    private Label totalBidsLabel;
    @FXML
    private Hyperlink viewAllBidsLink;

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

    // Time
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

    @FXML
    private VBox bidControlsContainer;
    @FXML
    private StackPane statusOverlay;
    @FXML
    private Label statusMessageLabel;

    private String itemId;
    private Item item;
    private Timeline timeline;
    private double myLastBid = 0.0;
    private final long AUTO_BID_DELAY = 50;

    private final User user = UserSession.getInstance().getLoggedInUser();
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = GsonUtil.getInstance();

    // Auto Bid
    private boolean isAutoBidActive = false;
    private double maxBidAmount = 0;
    private double autoBidIncremental = 0;
    private long lastAutoBidTime = 0;


    @FXML
    public void initialize() {
        try {
            Rectangle clip = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
            clip.setArcWidth(IMAGE_ARC);
            clip.setArcHeight(IMAGE_ARC);
            itemImage.setClip(clip);

            itemImage.setFitWidth(IMAGE_WIDTH);
            itemImage.setFitHeight(IMAGE_HEIGHT);
            itemImage.setPreserveRatio(false);

            itemImage.imageProperty().addListener((obs, oldImg, newImg) -> {
                if (newImg != null) {
                    applyObjectFitCover(newImg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing ItemPageController: " + e.getMessage());
        }

    }

    public void setItemId(String id) {
        this.itemId = id;
        String currentUserId = UserSession.getInstance().getLoggedInUser().getId();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s?userId=%s", AppConfig.getHttpUrl(), id, currentUserId)))
                .GET()
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    ResponseMessage response = gson.fromJson(responseBody, ResponseMessage.class);
                    if ("success".equals(response.getStatus()) && response.getData() != null) {
                        Item item = gson.fromJson(response.getData(), Item.class);
                        Platform.runLater(() -> initData(item));
                        System.out.println("Item data loaded successfully for item ID: " + id);
                    } else {
                        System.err.println("Failed to load item data: " + response.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error fetching item data: " + ex.getMessage());
                    return null;
                });
    }
    public void initData(Item item) {
        this.item = item;
        this.myLastBid = item.getMyLastBid();

        displayDataItem(item);
        connectToRealTimeBidding();
        updateUIByStatus();
        loadBidHistory();

        if (item.getSellerId().equals(user.getId())) {
            bidControlsContainer.setDisable(true);
            btnAutoBidToggle.setDisable(true);
            statusOverlay.setVisible(true);
            statusOverlay.setManaged(true);
            statusMessageLabel.setText("You are the owner of this item");
        }
    }

    private void updateUIByStatus() {
        if (item == null) return;

        String status = item.getStatus().toString().toUpperCase();
        boolean isOwner = item.getSellerId().equals(user.getId());

        bidControlsContainer.setVisible(false);
        bidControlsContainer.setManaged(false);
        statusOverlay.setVisible(true);
        statusOverlay.setManaged(true);

        statusMessageLabel.getStyleClass().removeAll("status-ended", "status-upcoming");

        if (isOwner) {
            statusMessageLabel.setText("👤 You are the owner of this item");
            return;
        }
        switch (status) {
            case "ONGOING":

                bidControlsContainer.setVisible(true);
                bidControlsContainer.setManaged(true);
                statusOverlay.setVisible(false);
                statusOverlay.setManaged(false);

                break;

            case "UPCOMING":
                statusMessageLabel.setText("⏳ This auction hasn't started yet");
                statusMessageLabel.getStyleClass().add("status-upcoming");
                break;

            case "ENDED":
                statusMessageLabel.setText("🚫 This auction has ended");
                statusMessageLabel.getStyleClass().add("status-ended");
                break;
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
                    }
                    updateMinimumBidLabel();

                    // Kích hoạt logic Auto Bid nếu đang bật
                    handleAutoBidLogic(bidPayload.getBidPrice(), bidPayload.getUserId());
                    loadBidHistory();
                } else {
                    System.err.println("Failed to parse updated item from response: " + jsonPayload);
                }
            } else {
                System.out.println("Received message: " + response.getMessage());
            }
        });
    }

    private void loadBidHistory() {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getHttpUrl() + "/api/bids/history/" + itemId))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    ResponseMessage res = gson.fromJson(body, ResponseMessage.class);
                    if ("success".equals(res.getStatus())) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<BidTransaction>>(){}.getType();
                        List<BidTransaction> bids = gson.fromJson(res.getData(), listType);
                        Platform.runLater(() -> renderBidHistory(bids));
                    }
                });
    }

    // --- CHỈNH SỬA 4: Logic Render (Tối đa 8 người) ---
    private void renderBidHistory(List<BidTransaction> bids) {
        historyBidContainer.getChildren().clear();
        if (bids == null || bids.isEmpty()) {
            totalBidsLabel.setText("0 bids");
            return;
        }

        // Sắp xếp giá cao nhất lên đầu
        bids.sort( Comparator.comparing(BidTransaction::getBidTime) .reversed() );

        int totalCount = bids.size();
        totalBidsLabel.setText(totalCount + " bids");

        // Chỉ lấy tối đa 8 người
        int displayLimit = Math.min(totalCount, 8);
        for (int i = 0; i < displayLimit; i++) {
            historyBidContainer.getChildren().add(createBidRow(i + 1, bids.get(i)));
        }

        // Hiện nút "View all" nếu nhiều hơn 8
        boolean hasMore = totalCount > 8;
        viewAllBidsLink.setVisible(hasMore);
        viewAllBidsLink.setManaged(hasMore);
        if (hasMore) viewAllBidsLink.setText("View all bids (" + totalCount + ") →");
    }

    // Hàm tạo UI cho từng dòng bid (giống ảnh mẫu)
    private HBox createBidRow(int index, BidTransaction bid) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row"); // Bạn cần định nghĩa class này trong CSS

        Label lblIndex = new Label(String.valueOf(index));
        lblIndex.getStyleClass().add("history-index");

        Circle avatar = new Circle(15, Color.web("#e0e0e0"));
        if (bid.getBidderId().equals(user.getId())) avatar.setFill(Color.web("#2D55FF"));

        VBox info = new VBox(2);
        String name = bid.getBidderId().equals(user.getId()) ? bid.getBidderId() + " (You)" : bid.getBidderId();
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold;");

        Label lblTime = new Label(bid.getBidTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        info.getChildren().addAll(lblName, lblTime);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label lblPrice = new Label(String.format("$ %.1f", bid.getBidAmount()));
        lblPrice.setStyle("-fx-text-fill: #4A835D; -fx-font-weight: bold;");

        row.getChildren().addAll(lblIndex, avatar, info, lblPrice);
        return row;
    }

    @FXML
    private void handleViewAllBids() {
        System.out.println("Redirecting to full history for item: " + itemId);
        // Chuyển hướng sang trang Full History tại đây
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

    private boolean checkBiddableStatus() {
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


    // Auto bid logic
    @FXML
    private void toggleAutoBidForm() {
        boolean isVisible = autoBidForm.isVisible();
        autoBidForm.setVisible(!isVisible);
        autoBidForm.setManaged(!isVisible);
    }

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

            handleAutoBidLogic(item.getCurrentPrice(), null);
        } catch (NumberFormatException e) {
            System.out.println("Vui lòng nhập số hợp lệ cho Auto Bid");
        }
    }
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

    // display utils
    private void displayDataItem(Item item) {
        thumbnailContainer.getChildren().clear();
        List<String> images = item.getImagesPath();

        if (images != null && !images.isEmpty()) {
            String mainImageUrl = images.get(0);
            ClientImageUtil.displayImage(mainImageUrl, "images", itemImage, IMAGE_WIDTH * 2, IMAGE_HEIGHT * 2);

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
                ClientImageUtil.displayImage(imgPath, "images", thumbView, IMAGE_WIDTH, IMAGE_HEIGHT);
                thumbPane.getChildren().add(thumbView);

                thumbPane.setOnMouseClicked(e -> {
                    Image clickedImage = thumbView.getImage();
                    if (clickedImage != null) {
                        itemImage.setImage(clickedImage);
                    } else {
                        ClientImageUtil.displayImage(imgPath, "images", itemImage, THUMB_WIDTH, THUMB_HEIGHT);
                    }
                    thumbnailContainer.getChildren().forEach(node -> {
                        node.getStyleClass().remove("active-thumb");
                    });
                    thumbPane.getStyleClass().add("active-thumb");
                });
                thumbnailContainer.getChildren().add(thumbPane);
            }
        }
        double minHeight = 100;
        itemDesLabel.setMinHeight(minHeight);
        itemDesLabel.setText(item.getDescription());
        itemDesLabel.setWrapText(true);
        itemDesLabel.setMaxWidth(Double.MAX_VALUE);
        itemDesLabel.prefWidthProperty().bind(
                itemDesLabel.getParent().layoutBoundsProperty().map(b -> b.getWidth())
        );

        itemDesLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        itemDesLabel.setMinHeight(Region.USE_PREF_SIZE);
        itemNameLabel.setText(item.getName());
        sellerLabel.setText(item.getSellerId());
        currentPriceLabel.setText(String.format("$ " + item.getCurrentPrice()));
        startPriceLabel.setText(String.valueOf(item.getStartingPrice()));
        bidStepLabel.setText(String.valueOf(item.getBidStep()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (item.getStartTime() != null) {
            startTimeLabel.setText(item.getStartTime().format(formatter));
        } else {
            startTimeLabel.setText("N/A");
        }
        if (item.getEndTime() != null) {
            endTimeLabel.setText(item.getEndTime().format(formatter));
        } else {
            endTimeLabel.setText("N/A");
        }
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
    private void applyObjectFitCover(Image img) {
        applyObjectFitCoverToImageView(itemImage, img, IMAGE_WIDTH, IMAGE_HEIGHT);
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

    // timer & countdown
    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }
        updateTimeDisplay();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimeDisplay()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.playFromStart();
    }
    private void updateTimeDisplay() {
        if (item == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = null;

        // upcoming
        if (item.getStartTime() != null && now.isBefore(item.getStartTime())) {
            targetTime = item.getStartTime();
        } else if (item.getEndTime() != null && now.isBefore(item.getEndTime())) {
            targetTime = item.getEndTime();
        }
        if (targetTime == null || now.isAfter(targetTime)) {
            updateTimerLabels(0, 0, 0, 0);
            updateUIByStatus(); // Gọi lại để ẩn/hiện bảng bid
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
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
