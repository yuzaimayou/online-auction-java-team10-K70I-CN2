package com.auction.client.controller;

import com.auction.client.network.AuctionRoomListener;
import com.auction.client.network.NetworkService;
import com.auction.client.service.*;
import com.auction.client.ui.*;
import com.auction.client.util.*;
import com.auction.client.validation.BidValidationService;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public class ItemPageController  {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final double IMAGE_WIDTH = 1400.0;
    private static final double IMAGE_HEIGHT = 900.0;
    private static final double THUMB_WIDTH  = 80.0;
    private static final double THUMB_HEIGHT = 60.0;

    // ─── FXML Bindings ────────────────────────────────────────────────────────
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
    @FXML
    private StackPane mainImageContainer;

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

    @FXML
    private ScrollPane historyScrollPane;
    @FXML
    private VBox historyBidContainer;
    @FXML
    private Label totalBidsLabel;

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
    // ─── Bid Price Chart ─────────────────────────────────────

    @FXML
    private AreaChart<String, Number> bidPriceChart;
    @FXML
    private CategoryAxis bidTimeAxis;
    @FXML
    private NumberAxis bidPriceAxis;
    private BidPanelController bidPanel;
    private AutoBidUiHandler autoBidHandler;
    private BidHistoryPanel historyPanel;


    private XYChart.Series<String, Number> bidPriceSeries;

    // ─── Core State ───────────────────────────────────────────────────────────
    private String itemId;
    private Item item;
    private double myLastBid = 0.0;

    // ─── Infrastructure Dependencies ──────────────────────────────────────────
    private final User user = UserSession.getInstance().getLoggedInUser();
    private final NetworkService network = NetworkService.getInstance();
    private final Gson gson = GsonUtil.getInstance();
    private final ItemsService itemsService = ItemsService.getInstance();
    private final BidHistoryService bidHistoryService = BidHistoryService.getInstance();
    private final AutoBidService autoBidManager = new AutoBidService();
    private final BidValidationService bidValidationService = new BidValidationService();

    // Tách Service xử lý Status
    private final ItemStatusService statusUiService = new ItemStatusService();
    private CountdownTimerUtil countdownTimer;


    // ─── Lifecycle & Cleanup ──────────────────────────────────────────────────

    @FXML
    public void initialize() {
        itemImage.setPreserveRatio(false);
        itemImage.setSmooth(true);
        itemImage.setCache(false);

        ClientImageUtil.makeResponsiveCover(itemImage, mainImageContainer, 16);
        countdownTimer = new CountdownTimerUtil(daysLabel, hoursLabel, minsLabel, secsLabel);
        bidPanel = new BidPanelController(
                statusMessageLabel, bidControlsContainer, statusOverlay,
                btnAutoBidToggle, submitBid, countdownTimer, autoBidManager
        );

        if (bidPriceChart != null) {
            bidPriceSeries = new XYChart.Series<>();
            bidPriceChart.getData().add(bidPriceSeries);
            bidPriceChart.setAnimated(false);
        }
        autoBidHandler = new AutoBidUiHandler(
                autoBidManager, network, user, statusUiService,
                autoBidForm, autoBidActiveStatus,
                maxBidField, autoBidStepField, userCurrentBidLabel,
                btnAutoBidToggle, submitBid,
                () -> item,        // Supplier<Item>
                () -> myLastBid    // Supplier<Double>
        );

        historyPanel = new BidHistoryPanel(
                bidHistoryService, user,
                historyBidContainer, historyScrollPane,
                totalBidsLabel, bidPriceSeries
        );
    }

    public void dispose() {
        if (countdownTimer != null) countdownTimer.stop();
        autoBidManager.deactivate();
        network.leaveRoom();
        System.out.println("[ItemPageController] Safe disposed.");
    }

    // ─── Data Initialization ──────────────────────────────────────────────────

    public void setItemId(String id) {
        this.itemId = id;
        itemsService.getItemById(id, user.getId())
                .thenAccept(loadedItem -> Platform.runLater(() -> initData(loadedItem)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ex.printStackTrace();
                        ToastUtil.showError(itemNameLabel.getScene(), "Failed to load item data");
                    });
                    return null;
                });
    }

    private void initData(Item loadedItem) {
        this.item = loadedItem;
        this.myLastBid = loadedItem.getMyLastBid();
        autoBidManager.setLastBidderId(loadedItem.getCurrentTopPLayerId());

        if (loadedItem.getStoredStatus() == AuctionStatus.BANNED) {
            displayDataItem(loadedItem);
            bidPanel.applyBannedStateView(loadedItem); // hiện overlay "Auction Suspended"
            autoBidHandler.updateUi(false);
            connectToRealTimeBidding();
            historyPanel.load(itemId);
            return;
        }

        displayDataItem(loadedItem);
        bidPanel.applyAuctionStatusView(loadedItem, user.getId());
        connectToRealTimeBidding();
        historyPanel.load(itemId);
    }

    private void connectToRealTimeBidding() {
        network.setAuctionRoomListener(new AuctionRoomListener() {
            public void onNewBid(BidPayload p) { Platform.runLater(() -> uiHandleNewBid(p)); }
            public void onAuctionExtended(LocalDateTime t) { Platform.runLater(() -> uiHandleAuctionExtended(t)); }
            public void onItemBanned(String id) { if (id.equals(itemId)) Platform.runLater(() -> uiHandleItemBanned()); }
        });

        boolean connected = network.connectToAuctionRoom(item.getId(), user.getId());
        if (!connected) {
            System.err.println("Failed to connect to auction room: " + item.getId());
        }
    }



    // ─── UI Mutators ──────────────────────────────────────────────────────────

    private void uiHandleNewBid(BidPayload bidPayload) {
        autoBidManager.setLastBidderId(bidPayload.getUserId());
        item.setCurrentTopPLayerId(bidPayload.getUserId());
        item.setCurrentPrice(bidPayload.getBidPrice());

        if (bidPayload.getUserId().equals(user.getId())) {
            this.myLastBid = bidPayload.getBidPrice();
        }

        currentPriceLabel.setText(String.format("$ %.0f", item.getCurrentPrice()));
        bidPanel.applyAuctionStatusView(item, user.getId());
        updateMinimumBidLabel();
        autoBidHandler.handleDecision(bidPayload.getBidPrice(), bidPayload.getUserId());
        historyPanel.load(itemId);

        WalletService.getInstance().fetchAndSync();
    }

    private void uiHandleAuctionExtended(LocalDateTime newEndTime) {
        item.setEndTime(newEndTime);
        endTimeLabel.setText(DateTimeUtil.format(newEndTime));
        bidPanel.startCountdown(item,
                () -> bidPanel.applyAuctionStatusView(item, user.getId()));
    }

    private void uiHandleItemBanned() {
        network.setListener(null);
        autoBidHandler.updateUi(false);
        bidPanel.applyBannedStateView(item);
    }

    // ─── Manual & Auto Bidding Interactions ───────────────────────────────────

    @FXML
    public void bidHandle() {
        String input = bidAmountField.getText().trim();
        if (input.isEmpty()) {
            ToastUtil.showInfo(bidAmountField.getScene(), "Please enter a bid amount.");
            return;
        }
        try {
            double amount = Double.parseDouble(input);
            BidValidationService.ValidationResult result = bidValidationService.validate(item, user, amount, statusUiService.isOngoing(item));
            if (!result.ok()) {
                ToastUtil.showError(bidAmountField.getScene(), result.errorMessage());
                return;
            }
            network.sendBid(item.getId(), user.getId(), amount, "");
            bidAmountField.clear();
        } catch (NumberFormatException e) {
            ToastUtil.showError(bidAmountField.getScene(), "Invalid price format.");
        }
    }

    @FXML
    private void handleSuggestStep1() {
        if (item != null) bidAmountField.setText(String.format("%.0f", item.getCurrentPrice() + item.getBidStep()));
    }

    @FXML
    private void handleSuggestStep2() {
        if (item != null) bidAmountField.setText(String.format("%.0f", item.getCurrentPrice() + item.getBidStep() * 2));
    }

    @FXML
    private void toggleAutoBidForm() {
        autoBidHandler.toggleForm();
    }
    @FXML
    private void startAutoBid() {
        autoBidHandler.start();
    }

    @FXML
    private void stopAutoBid() {
        autoBidHandler.stop();
    }

    // ─── Core Display & Timers ────────────────────────────────────────────────

    private void displayDataItem(Item currentItem) {
        ClientImageUtil.setupThumbnailGallery(
                thumbnailContainer, itemImage, currentItem.getImagesPath(),
                THUMB_WIDTH, THUMB_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT
        );

        itemDesLabel.setText(currentItem.getDescription());
        itemDesLabel.setWrapText(true);
        itemDesLabel.setMinHeight(100);
        itemDesLabel.setMaxWidth(Double.MAX_VALUE);
        itemDesLabel.prefWidthProperty().bind(
                itemDesLabel.getParent().layoutBoundsProperty().map(b -> b.getWidth()));
        itemDesLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        itemDesLabel.setMinHeight(Region.USE_PREF_SIZE);

        itemNameLabel.setText(currentItem.getName());
        sellerLabel.setText(currentItem.getSellerId());
        currentPriceLabel.setText(statusUiService.formatPrice(currentItem.getCurrentPrice()));
        startPriceLabel.setText(statusUiService.formatPrice(currentItem.getStartingPrice()));
        bidStepLabel.setText(statusUiService.formatPrice(currentItem.getBidStep()));

        startTimeLabel.setText(DateTimeUtil.format(currentItem.getStartTime()));
        endTimeLabel.setText(DateTimeUtil.format(currentItem.getEndTime()));

        if (btnSuggestStep1 != null) btnSuggestStep1.setText(String.format("$ %.0f", currentItem.getBidStep()));
        if (btnSuggestStep2 != null) btnSuggestStep2.setText(String.format("$ %.0f", currentItem.getBidStep() * 2));

        // Kích hoạt Countdown đồng bộ thông qua Service ủy quyền
        AuctionStatus status = currentItem.getStoredStatus();
        if (status != AuctionStatus.BANNED) {
            bidPanel.startCountdown(item,
                    () -> bidPanel.applyAuctionStatusView(item, user.getId()));
        }
        updateMinimumBidLabel();
    }

    private void updateMinimumBidLabel() {
        if (item == null || minimumBidLabel == null) return;
        minimumBidLabel.setText(String.format(
                "Your last bid: $ %.0f  (Min next: $ %.0f)",
                myLastBid, item.getCurrentPrice() + item.getBidStep()
        ));
    }
}