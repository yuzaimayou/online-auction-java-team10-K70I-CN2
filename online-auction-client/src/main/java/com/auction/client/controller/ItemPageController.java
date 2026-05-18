package com.auction.client.controller;

import com.auction.client.service.AutoBidService;
import com.auction.client.service.AutoBidService.AutoBidDecision;
import com.auction.client.service.AutoBidService.ValidationResult;
import com.auction.client.service.BidHistoryService;
import com.auction.client.service.ItemsService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.ToastService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.CountdownTimerUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ItemPageController implements NetworkService.MessageListener {

    // Constants
    private static final double IMAGE_WIDTH  = 700.0;
    private static final double IMAGE_HEIGHT = 450.0;
    private static final double IMAGE_ARC    = 20.0;
    private static final double THUMB_WIDTH  = 80.0;
    private static final double THUMB_HEIGHT = 60.0;

    // FXML: item info
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
    private HBox thumbnailContainer;

    // FXML: bid controls
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

    // FXML: bid history (Đã chỉnh sửa: Thêm ScrollPane để quản lý việc co giãn hiển thị)
    @FXML
    private ScrollPane historyScrollPane;
    @FXML
    private VBox historyBidContainer;
    @FXML
    private Label totalBidsLabel;

    // FXML: auto-bid
    @FXML
    private VBox autoBidForm;
    @FXML
    private VBox  autoBidActiveStatus;
    @FXML
    private TextField maxBidField;
    @FXML
    private TextField autoBidStepField;
    @FXML
    private Label userCurrentBidLabel;
    @FXML
    private Button btnAutoBidToggle;

    // FXML: countdown timer
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

    //  FXML: status overlay
    @FXML private VBox bidControlsContainer;
    @FXML private StackPane statusOverlay;
    @FXML private Label statusMessageLabel;

    // State
    private String itemId;
    private Item item;
    private double myLastBid = 0.0;

    //  Dependencies
    private final User user = UserSession.getInstance().getLoggedInUser();
    private final NetworkService network  = NetworkService.getInstance();
    private final Gson gson = GsonUtil.getInstance();
    private final ItemsService itemsService = ItemsService.getInstance();
    private final BidHistoryService bidHistoryService = BidHistoryService.getInstance();

    private final AutoBidService autoBidManager = new AutoBidService();
    private CountdownTimerUtil countdownTimer;

    // Lifecycle
    @FXML
    public void initialize() {
        initImageClip();
        countdownTimer = new CountdownTimerUtil(daysLabel, hoursLabel, minsLabel, secsLabel);
    }

    private void initImageClip() {
        Rectangle clip = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
        clip.setArcWidth(IMAGE_ARC);
        clip.setArcHeight(IMAGE_ARC);
        itemImage.setClip(clip);
        itemImage.setFitWidth(IMAGE_WIDTH);
        itemImage.setFitHeight(IMAGE_HEIGHT);
        itemImage.setPreserveRatio(false);
        itemImage.imageProperty().addListener((obs, old, newImg) -> {
            if (newImg != null) {
                ClientImageUtil.applyObjectFitCoverToImageView(itemImage, newImg, IMAGE_WIDTH, IMAGE_HEIGHT);
            }
        });
    }

    // Data loading
    public void setItemId(String id) {
        this.itemId = id;
        itemsService.getItemById(id, user.getId())
                .thenAccept(item -> Platform.runLater(() -> initData(item)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ex.printStackTrace();
                        ToastService.showError(itemNameLabel.getScene(), "Failed to load item data");
                    });
                    return null;
                });
    }

    public void initData(Item item) {
        this.item         = item;
        this.myLastBid    = item.getMyLastBid();
        autoBidManager.setLastBidderId(item.getCurrentTopPLayerId());

        AuctionStatus status = currentStatus();
        item.setStatus(status);

        displayDataItem(item);
        updateUIByStatus(status);
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

    private void updateUIByStatus(AuctionStatus status) {
        if (item == null) return;

        item.setStatus(status);
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
            case ONGOING -> {
                bidControlsContainer.setVisible(true);
                bidControlsContainer.setManaged(true);
                statusOverlay.setVisible(false);
                statusOverlay.setManaged(false);
            }
            case UPCOMING -> {
                statusMessageLabel.setText("⏳ This auction hasn't started yet");
                statusMessageLabel.getStyleClass().add("status-upcoming");
            }
            case ENDED, PAID, CANCELED -> {
                statusMessageLabel.setText("🚫 This auction has ended");
                statusMessageLabel.getStyleClass().add("status-ended");
            }
        }
    }

    // Realtime bidding
    @Override
    public void onMessageReceived(ResponseMessage response) {
        System.out.println(response);
        Platform.runLater(() -> {
            if (!"success".equals(response.getStatus()) || !"NEW_BID".equals(response.getMessage())) return;

            JsonElement jsonElement = gson.toJsonTree(response.getData());
            BidPayload  bidPayload  = gson.fromJson(jsonElement, BidPayload.class);

            if (bidPayload == null) {
                System.err.println("Failed to parse BidPayload: " + jsonElement);
                return;
            }

            autoBidManager.setLastBidderId(bidPayload.getUserId());
            item.setCurrentTopPLayerId(bidPayload.getUserId());
            item.setCurrentPrice(bidPayload.getBidPrice());

            if (user.getId().equals(bidPayload.getUserId())) {
                myLastBid = bidPayload.getBidPrice();
            }

            currentPriceLabel.setText(String.format("$ %.0f", item.getCurrentPrice()));
            updateUIByStatus(currentStatus());
            updateMinimumBidLabel();

            handleAutoBidLogic(bidPayload.getBidPrice(), bidPayload.getUserId());
            loadBidHistory();
        });
    }

    private void connectToRealTimeBidding() {
        network.setListener(this);
        boolean connected = network.connectToAuctionRoom(item.getId(), user.getId());
        if (!connected) System.err.println("Failed to connect to auction room: " + item.getId());
    }

    @FXML
    public void bidHandle() {
        if (!isOngoing()) {
            ToastService.showError(bidAmountField.getScene(), "Auction is not active.");
            return;
        }
        if (user.getId().equals(item.getCurrentTopPLayerId())) {
            ToastService.showInfo(bidAmountField.getScene(), "You are already the highest bidder.");
            return;
        }

        String inputAmount = bidAmountField.getText().trim();
        if (inputAmount.isEmpty()) {
            ToastService.showInfo(bidAmountField.getScene(), "Please enter a bid amount.");
            return;
        }
        try {
            double bidAmount   = Double.parseDouble(inputAmount);
            double minRequired = item.getCurrentPrice() + item.getBidStep();

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
        bidAmountField.setText(String.format("%.0f", item.getCurrentPrice() + item.getBidStep()));
    }

    @FXML
    private void handleSuggestStep2() {
        if (item == null) return;
        bidAmountField.setText(String.format("%.0f", item.getCurrentPrice() + item.getBidStep() * 2));
    }

    @FXML
    private void toggleAutoBidForm() {
        boolean visible = autoBidForm.isVisible();
        autoBidForm.setVisible(!visible);
        autoBidForm.setManaged(!visible);
    }

    @FXML
    private void startAutoBid() {
        if (!isOngoing()) {
            ToastService.showError(maxBidField.getScene(), "Auction is not active.");
            return;
        }
        try {
            double max  = Double.parseDouble(maxBidField.getText().trim());
            double step = Double.parseDouble(autoBidStepField.getText().trim());

            ValidationResult result = autoBidManager.validate(item, max, step);
            if (!result.ok()) {
                ToastService.showError(maxBidField.getScene(), result.errorMessage());
                return;
            }

            autoBidManager.activate(max, step);
            network.sendAutoBidRegister(item.getId(), user.getId(), max, step);
            updateAutoBidUI(true);
            ToastService.showSuccess(maxBidField.getScene(), "Auto-Bid activated!");

            boolean isLeading = user.getId().equals(autoBidManager.getLastBidderId());
            if (isLeading) {
                userCurrentBidLabel.setText(
                        String.format("Your current bid: $ %.0f (Leading)", item.getCurrentPrice()));
            } else {
                double firstBid = item.getCurrentPrice() + step;
                network.sendBid(item.getId(), user.getId(), firstBid, "");
                userCurrentBidLabel.setText(
                        String.format("Your current bid: $ %.0f (Auto-bidding...)", firstBid));
            }
        } catch (NumberFormatException e) {
            ToastService.showError(maxBidField.getScene(), "Please enter valid numbers.");
        }
    }

    @FXML
    private void stopAutoBid() {
        autoBidManager.deactivate();
        updateAutoBidUI(false);
    }

    private void handleAutoBidLogic(double serverCurrentPrice, String topBidderId) {
        AutoBidDecision decision = autoBidManager.decideBid(
                topBidderId, serverCurrentPrice, user.getId(), myLastBid, isOngoing());

        switch (decision.type()) {
            case AUCTION_ENDED -> stopAutoBid();
            case INACTIVE      -> {}
            case LEADING       -> userCurrentBidLabel.setText(decision.statusText());
            case MAX_REACHED -> {
                updateAutoBidUI(false);
                ToastService.showInfo(userCurrentBidLabel.getScene(), decision.statusText());
            }
            case OUTBID_AND_REBID -> {
                userCurrentBidLabel.setText(decision.statusText());
                network.sendBid(item.getId(), user.getId(), decision.nextBidPrice(), "");
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

    // Bid history
    private void loadBidHistory() {
        bidHistoryService.getHistory(itemId)
                .thenAccept(bids -> Platform.runLater(() -> renderBidHistory(bids)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            ToastService.showError(historyBidContainer.getScene(), "Failed to load bid history"));
                    return null;
                });
    }

    // (Đã chỉnh sửa: Thay đổi trạng thái layout động giống display:none / display:block)
    private void renderBidHistory(List<BidHistoryItemDTO> bids) {
        historyBidContainer.getChildren().clear();

        if (bids == null || bids.isEmpty()) {
            totalBidsLabel.setText("0 bids");
            // Nếu có liên kết ScrollPane từ FXML, thu gọn layout để tránh chiếm không gian trống vô ích
            if (historyScrollPane != null) {
                historyScrollPane.setVisible(false);
                historyScrollPane.setManaged(false);
            }
            return;
        }

        // Có dữ liệu -> Kích hoạt hiển thị phân cấp linh hoạt tự giãn
        if (historyScrollPane != null) {
            historyScrollPane.setVisible(true);
            historyScrollPane.setManaged(true);
        }

        int totalCount = bids.size();
        totalBidsLabel.setText(totalCount + " bids");
        for (int i = 0; i < totalCount; i++) {
            historyBidContainer.getChildren().add(createBidRow(totalCount - i, bids.get(i)));
        }
    }

    private HBox createBidRow(int index, BidHistoryItemDTO bid) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");

        Label lblIndex = new Label(String.valueOf(index));
        lblIndex.getStyleClass().add("history-index");
        lblIndex.setStyle("-fx-text-fill: #000000;");

        VBox info = new VBox(2);
        String name = bid.userName.equals(user.getUsername()) ? bid.userName + " (You)" : bid.userName;
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label lblTime = new Label(bid.bidTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        info.getChildren().addAll(lblName, lblTime);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label lblPrice = new Label(String.format("$ %.1f", bid.bidPrice));
        lblPrice.setStyle("-fx-text-fill: #4A835D; -fx-font-weight: bold; -fx-font-size: 16px;");

        row.getChildren().addAll(lblIndex, info, lblPrice);
        return row;
    }

    //  Display & image
    private void displayDataItem(Item item) {
        thumbnailContainer.getChildren().clear();
        List<String> images = item.getImagesPath();

        if (images != null && !images.isEmpty()) {
            ClientImageUtil.displayImage(images.get(0), "images", itemImage,
                    IMAGE_WIDTH * 2, IMAGE_HEIGHT * 2);

            boolean isFirst = true;
            for (String imgPath : images) {
                if (imgPath == null || imgPath.isBlank()) continue;

                StackPane thumbPane = new StackPane();
                thumbPane.getStyleClass().add("thumbnail-container");
                thumbPane.setMinWidth(THUMB_WIDTH);  thumbPane.setMaxWidth(THUMB_WIDTH);
                thumbPane.setMinHeight(THUMB_HEIGHT); thumbPane.setMaxHeight(THUMB_HEIGHT);
                if (isFirst) { thumbPane.getStyleClass().add("active-thumb"); isFirst = false; }

                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(THUMB_WIDTH);
                thumbView.setFitHeight(THUMB_HEIGHT);
                thumbView.setPreserveRatio(false);
                thumbView.imageProperty().addListener((obs, oldImg, newImg) -> {
                    if (newImg != null)
                        ClientImageUtil.applyObjectFitCoverToImageView(thumbView, newImg, THUMB_WIDTH, THUMB_HEIGHT);
                });

                ClientImageUtil.displayImage(imgPath, "images", thumbView, IMAGE_WIDTH, IMAGE_HEIGHT);
                thumbPane.getChildren().add(thumbView);

                thumbPane.setOnMouseClicked(e -> {
                    Image clicked = thumbView.getImage();
                    if (clicked != null) itemImage.setImage(clicked);
                    else ClientImageUtil.displayImage(imgPath, "images", itemImage, THUMB_WIDTH, THUMB_HEIGHT);
                    thumbnailContainer.getChildren().forEach(n -> n.getStyleClass().remove("active-thumb"));
                    thumbPane.getStyleClass().add("active-thumb");
                });
                thumbnailContainer.getChildren().add(thumbPane);
            }
        }

        itemDesLabel.setMinHeight(100);
        itemDesLabel.setText(item.getDescription());
        itemDesLabel.setWrapText(true);
        itemDesLabel.setMaxWidth(Double.MAX_VALUE);
        itemDesLabel.prefWidthProperty().bind(
                itemDesLabel.getParent().layoutBoundsProperty().map(b -> b.getWidth()));
        itemDesLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        itemDesLabel.setMinHeight(Region.USE_PREF_SIZE);

        itemNameLabel.setText(item.getName());
        sellerLabel.setText(item.getSellerId());
        currentPriceLabel.setText(String.format("$ %.0f", item.getCurrentPrice()));
        startPriceLabel.setText(String.format("$ %.0f", item.getStartingPrice()));
        bidStepLabel.setText(String.format("$ %.0f", item.getBidStep()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        startTimeLabel.setText(item.getStartTime() != null ? item.getStartTime().format(fmt) : "N/A");
        endTimeLabel.setText(item.getEndTime()     != null ? item.getEndTime().format(fmt)   : "N/A");

        if (btnSuggestStep1 != null) btnSuggestStep1.setText(String.format("$ %.0f", item.getBidStep()));
        if (btnSuggestStep2 != null) btnSuggestStep2.setText(String.format("$ %.0f", item.getBidStep() * 2));

        startCountdown();
        updateMinimumBidLabel();
    }

    private void updateMinimumBidLabel() {
        if (item == null || minimumBidLabel == null) return;
        minimumBidLabel.setText(String.format(
                "Your last bid: $ %.0f  (Min next: $ %.0f)",
                myLastBid,
                item.getCurrentPrice() + item.getBidStep()));
    }

    private void startCountdown() {
        AuctionStatus status = currentStatus();
        LocalDateTime targetTime = null;

        if (status == AuctionStatus.UPCOMING) {
            targetTime = item.getStartTime();
        } else if (status == AuctionStatus.ONGOING) {
            targetTime = item.getEndTime();
        }

        countdownTimer.startFor(targetTime, () -> {
            AuctionStatus newStatus = currentStatus();
            item.setStatus(newStatus);
            Platform.runLater(() -> updateUIByStatus(newStatus));
        });
    }

    private AuctionStatus currentStatus() {
        if (item == null) return AuctionStatus.ENDED;
        return AuctionStatus.compute(item.getStartTime(), item.getEndTime());
    }

    private boolean isOngoing()  { return currentStatus() == AuctionStatus.ONGOING; }
}