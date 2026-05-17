package com.auction.client.controller;

import com.auction.client.service.ItemsService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.ToastService;
import com.auction.client.util.AppConfig;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.auction.BidTransaction;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public class ItemPageController implements NetworkService.MessageListener {
    private static final double IMAGE_WIDTH = 700.0;
    private static final double IMAGE_HEIGHT = 450.0;
    private static final double IMAGE_ARC = 20.0;
    private static final double THUMB_WIDTH = 80.0;
    private static final double THUMB_HEIGHT = 60.0;
    private final long AUTO_BID_DELAY = 50;


    // item information
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

    // BID CONTROL
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button submitBid;
    @FXML
    private Label minimumBidLabel;
    @FXML
    private Button btnSuggestStep1;
    @FXML
    private Button btnSuggestStep2;

    // BID HISTORY
    @FXML
    private VBox historyBidContainer;
    @FXML
    private Label totalBidsLabel;
    @FXML
    private Hyperlink viewAllBidsLink;


    // IMAGE THUMBNAIL
    @FXML
    private HBox thumbnailContainer;

    // AUTO BID
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

    // COUNTDOWN TIMER
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

    //STATUS OVERLAY
    @FXML
    private VBox bidControlsContainer;
    @FXML
    private StackPane statusOverlay;
    @FXML
    private Label statusMessageLabel;


    private String itemId;
    private final ItemsService itemsService = ItemsService.getInstance();
    private Item item;
    private Timeline timeline;
    private double myLastBid = 0.0;

    private final User user = UserSession.getInstance().getLoggedInUser();
    private NetworkService network = NetworkService.getInstance();
    private Gson gson = GsonUtil.getInstance();

    // Auto Bid
    private boolean isAutoBidActive = false;
    private double maxBidAmount = 0;
    private double autoBidIncremental = 0;
    private long lastAutoBidTime = 0;
    private String lastBidderId = "";


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

        String currentUserId = user.getId();
        itemsService.getItemById(id, currentUserId)
                .thenAccept(item -> {
                    Platform.runLater(() -> initData(item));})
                .exceptionally(ex -> {
                    Platform.runLater(() -> {System.err.println("Failed to load item");
                        ex.printStackTrace();
                        ToastService.showError(
                                itemNameLabel.getScene(), "Failed to load item data"
                        );
                    });

                    return null;
                });
    }
    public void initData(Item item) {
        this.item = item;
        this.myLastBid = item.getMyLastBid();
        this.lastBidderId = item.getCurrentTopPLayerId();

        item.setStatus(resolveRealtimeStatus());
        displayDataItem(item);

        // cập nhật UI NGAY lập tức
        updateUIByStatus();
        connectToRealTimeBidding();
        loadBidHistory();

        if (item.getSellerId().equals(user.getId())) {
            bidControlsContainer.setDisable(true);
            btnAutoBidToggle.setDisable(true);
            statusOverlay.setVisible(true);
            statusOverlay.setManaged(true);
            statusMessageLabel.setText("You are the owner of this item");
        }
    }
    // UI STATE MANAGEMENT
    private void updateUIByStatus() {
        if (item == null) return;
        String status = resolveRealtimeStatus();
        item.setStatus(status);
        boolean isOwner =
                item.getSellerId().equals(user.getId());

        bidControlsContainer.setVisible(false);
        bidControlsContainer.setManaged(false);

        statusOverlay.setVisible(true);
        statusOverlay.setManaged(true);

        statusMessageLabel.getStyleClass().removeAll("status-ended", "status-upcoming");
        if (isOwner) {
            statusMessageLabel.setText("👤 You are the owner of this item"
            );
            return;
        }

        switch (status.toUpperCase()) {
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

    // REALTIME BIDDING
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
                JsonElement jsonElement = gson.toJsonTree(response.getData());
                BidPayload bidPayload = gson.fromJson(jsonElement, BidPayload.class);
                if (bidPayload != null) {
                    System.out.println("Received new bid update: " + jsonElement);
                    this.lastBidderId = bidPayload.getUserId();
                    item.setCurrentTopPLayerId(bidPayload.getUserId());
                    item.setCurrentPrice(bidPayload.getBidPrice());

                    currentPriceLabel.setText(String.format("$ %.0f", item.getCurrentPrice())
                    );
                    updateUIByStatus();

                    if (user.getId().equals(bidPayload.getUserId())) {
                        myLastBid = bidPayload.getBidPrice();
                    }
                    updateMinimumBidLabel();
                    handleAutoBidLogic(bidPayload.getBidPrice(), bidPayload.getUserId());
                    loadBidHistory();
                } else {
                    System.err.println("Failed to parse updated item from response: " + jsonElement);
                }
            } else {
                System.out.println("Received message: " + response.getMessage());
            }
        });
    }

    public void bidHandle() {
        String status = resolveRealtimeStatus();

        if (!"ONGOING".equalsIgnoreCase(status)) {
            ToastService.showError(
                    bidAmountField.getScene(),
                    "Auction is not active."
            );
            return;
        }
        if (user.getId().equals(item.getCurrentTopPLayerId())) {
            ToastService.showInfo(
                    bidAmountField.getScene(),
                    "You are already the highest bidder."
            );
            return;
        }

        String inputAmount = bidAmountField.getText().trim();
        if (inputAmount.isEmpty()) {
            ToastService.showInfo(bidAmountField.getScene(), "Please enter a bid amount.");
            return;
        }
        try {
            double bidAmount = Double.parseDouble(inputAmount);
            double minRequired = item.getCurrentPrice() + item.getBidStep();

            // Kiểm tra logic giá trước khi gửi lên server để giảm tải
            if (bidAmount < minRequired) {
                ToastService.showError(bidAmountField.getScene(),
                        String.format("Bid must be at least $ %.1f", minRequired));
                return;
            }
            network.sendBid(item.getId(), user.getId(), bidAmount, "");
            bidAmountField.clear();
        } catch (NumberFormatException e) {
            ToastService.showError(bidAmountField.getScene(), "Invalid price format.");
        }
    }

    @FXML
    private void handleSuggestStep1() {
        if (item == null) return;
        double suggestedPrice = item.getCurrentPrice() + item.getBidStep();
        bidAmountField.setText(String.format("%.0f", suggestedPrice));
    }

    @FXML
    private void handleSuggestStep2() {
        if (item == null) return;
        double suggestedPrice = item.getCurrentPrice() + (item.getBidStep() * 2);
        bidAmountField.setText(String.format("%.0f", suggestedPrice));
    }

    // AUTO BID
    @FXML
    private void toggleAutoBidForm() {
        boolean isVisible = autoBidForm.isVisible();
        autoBidForm.setVisible(!isVisible);
        autoBidForm.setManaged(!isVisible);
    }

    @FXML
    private void startAutoBid() {
        String status = resolveRealtimeStatus();

        if (!"ONGOING".equalsIgnoreCase(status)) {
            ToastService.showError(maxBidField.getScene(), "Auction is not active.");
            return;
        }
        try {
            maxBidAmount = Double.parseDouble(maxBidField.getText().trim());
            autoBidIncremental = Double.parseDouble(autoBidStepField.getText().trim());

            if (maxBidAmount <= item.getCurrentPrice()) {
                ToastService.showInfo(maxBidField.getScene(), "Max bid must be greater than current price.");
                return;
            }
            if (autoBidIncremental < item.getBidStep()) {
                ToastService.showError(maxBidField.getScene(),
                        "Your step must be at least " + item.getBidStep());
                return;
            }

            if (autoBidIncremental >= maxBidAmount) {
                ToastService.showError(maxBidField.getScene(),
                        String.format("Increment ($ %.0f) must be less than Max bid ($ %.0f).",
                                autoBidIncremental, maxBidAmount));
                return;
            }

            if (item.getCurrentPrice() + autoBidIncremental > maxBidAmount) {
                ToastService.showError(maxBidField.getScene(),
                        String.format("First auto-bid would be $ %.0f, which exceeds your Max bid ($ %.0f). Please raise Max bid or lower your increment.",
                                item.getCurrentPrice() + autoBidIncremental, maxBidAmount));
                return;
            }

            network.sendAutoBidRegister(item.getId(), user.getId(), maxBidAmount, autoBidIncremental);
            isAutoBidActive = true;
            updateAutoBidUI(true);
            ToastService.showSuccess(maxBidField.getScene(), "Auto-Bid activated!");

            boolean isLeading = user.getId().equals(lastBidderId);

            if (isLeading) {
                // User đang dẫn đầu → chỉ hiển thị giá hiện tại của họ, không cần bid thêm
                userCurrentBidLabel.setText(
                        String.format("Your current bid: $ %.0f (Leading)", item.getCurrentPrice()));
            } else {
                // User KHÔNG phải người cao nhất → tự động đặt giá ngay lập tức
                double firstAutoBidPrice = item.getCurrentPrice() + autoBidIncremental;

                if (firstAutoBidPrice <= maxBidAmount) {
                    // Giá auto-bid đầu tiên chưa vượt giới hạn → gửi bid
                    network.sendBid(item.getId(), user.getId(), firstAutoBidPrice, "");
                    userCurrentBidLabel.setText(
                            String.format("Your current bid: $ %.0f (Auto-bidding...)", firstAutoBidPrice));
                } else {
                    // firstAutoBidPrice vượt maxBidAmount → không thể bid, dừng luôn
                    stopAutoBid();
                    ToastService.showError(maxBidField.getScene(),
                            String.format("Max bid ($ %.0f) is too low to place even one bid. Min required: $ %.0f",
                                    maxBidAmount, firstAutoBidPrice));
                }
            }

        } catch (NumberFormatException e) {
            ToastService.showError(maxBidField.getScene(), "Please enter valid numbers.");
        }
    }

    @FXML
    private void stopAutoBid() {
        isAutoBidActive = false;
        updateAutoBidUI(false);

    }

    private void handleAutoBidLogic(double serverCurrentPrice, String lastBidderId) {
        if (!"ONGOING".equalsIgnoreCase(resolveRealtimeStatus())) {
            stopAutoBid();
            return;
        }
        if (!isAutoBidActive) return;
        if (user.getId().equals(lastBidderId)) {
            userCurrentBidLabel.setText(
                    String.format("Your current bid: $ %.0f (Leading)", serverCurrentPrice));

        } else if (serverCurrentPrice >= maxBidAmount) {
            stopAutoBid();
            Platform.runLater(() ->
                    ToastService.showInfo(userCurrentBidLabel.getScene(),
                            "Auto-bid stopped: Max limit reached!")
            );
        } else {
            // User khác vừa bid cao hơn → server sẽ trigger auto-bid
            if (myLastBid > 0) {
                userCurrentBidLabel.setText(
                        String.format("Your current bid: $ %.0f (Outbid — auto-bidding...)", myLastBid));
            } else {
                userCurrentBidLabel.setText("Auto-bidding...");
            }
        }
    }
    private void updateAutoBidUI(boolean active) {
        autoBidForm.setVisible(false);
        autoBidForm.setManaged(false);

        autoBidActiveStatus.setVisible(active);
        autoBidActiveStatus.setManaged(active);

        btnAutoBidToggle.setVisible(!active);
        btnAutoBidToggle.setManaged(!active);
        submitBid.setDisable(active || item.getSellerId().equals(user.getId()));
    }

    // BID HISTORY
    private void loadBidHistory() {
        itemsService.getBidHistory(itemId)
                .thenAccept(bids -> {
                    Platform.runLater(() -> {
                        renderBidHistory(bids);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ex.printStackTrace();
                        ToastService.showError(historyBidContainer.getScene(), "Failed to load bid history");
                    });
                    return null;
                });
    }

    private void renderBidHistory(List<BidTransaction> bids) {
        historyBidContainer.getChildren().clear();
        if (bids == null || bids.isEmpty()) {
            totalBidsLabel.setText("0 bids");
            return;
        }

        int totalCount = bids.size();
        totalBidsLabel.setText(totalCount + " bids");

        int displayLimit = Math.min(totalCount, 8);
        for (int i = 0; i < displayLimit; i++) {
            historyBidContainer.getChildren().add(createBidRow(i + 1, bids.get(i)));
        }
        boolean hasMore = totalCount > 8;
        viewAllBidsLink.setVisible(hasMore);
        viewAllBidsLink.setManaged(hasMore);
        if (hasMore) viewAllBidsLink.setText("View all bids (" + totalCount + ") →");
    }

    private HBox createBidRow(int index, BidTransaction bid) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");

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
    }


    // DISPLAY & IMAGE HANDLING
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
        currentPriceLabel.setText(
                String.format("$ %.0f", item.getCurrentPrice())
        );

        startPriceLabel.setText(
                String.format("$ %.0f", item.getStartingPrice())
        );

        bidStepLabel.setText(
                String.format("$ %.0f", item.getBidStep())
        );
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
        if (btnSuggestStep1 != null) btnSuggestStep1.setText(String.format("$ %.0f", item.getBidStep()));
        if (btnSuggestStep2 != null) btnSuggestStep2.setText(String.format("$ %.0f", item.getBidStep() * 2));
        startCountdown();
        updateMinimumBidLabel();
    }

    private void updateMinimumBidLabel() {
        if (item != null && minimumBidLabel != null) {
            double minimumNextBid = item.getCurrentPrice() + item.getBidStep();

            minimumBidLabel.setText(
                    String.format("Your last bid: $ %.0f (Min next: $ %.0f)",
                            myLastBid,
                            minimumNextBid)
            );
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

    // TIMER $ COUNTDOWN
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

        String status = resolveRealtimeStatus();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = null;

        if ("UPCOMING".equalsIgnoreCase(status)) {
            targetTime = item.getStartTime();
        } else if ("ONGOING".equalsIgnoreCase(status)) {
            targetTime = item.getEndTime();

        }
        if (targetTime == null || now.isAfter(targetTime)) {
            String realtimeStatus = resolveRealtimeStatus();
            item.setStatus(realtimeStatus);
            updateUIByStatus();
            updateTimerLabels(0,0,0,0);
            if (timeline != null) {
                timeline.stop();
            }
            return;
        }

        long days = ChronoUnit.DAYS.between(now, targetTime);
        long hours = ChronoUnit.HOURS.between(now, targetTime) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        updateTimerLabels(days, hours, minutes, seconds);
    }

    private void updateTimerLabels(long d, long h, long m, long s) {
        if (daysLabel != null) daysLabel.setText(String.format("%02d", d));
        if (hoursLabel != null) hoursLabel.setText(String.format("%02d", h));
        if (minsLabel != null) minsLabel.setText(String.format("%02d", m));
        if (secsLabel != null) secsLabel.setText(String.format("%02d", s));
    }
    private String resolveRealtimeStatus() {
        if (item == null) {
            return "ENDED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            return "UPCOMING";
        }
        if (!now.isBefore(item.getEndTime())) {
            return "ENDED";
        }
        return "ONGOING";
    }
}