package com.auction.client.controller;

import com.auction.client.service.*;
import com.auction.client.service.AutoBidService.AutoBidDecision;
import com.auction.client.service.AutoBidService.ValidationResult;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.CountdownTimerUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.constant.ItemStatusConstants;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.util.ArrayList;
import java.util.Collections;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ItemPageController implements NetworkService.MessageListener {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final double IMAGE_WIDTH  = 700.0;
    private static final double IMAGE_HEIGHT = 450.0;
    private static final double THUMB_WIDTH  = 80.0;
    private static final double THUMB_HEIGHT = 60.0;
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        ClientImageUtil.makeResponsiveCover(itemImage, mainImageContainer, 16);
        countdownTimer = new CountdownTimerUtil(daysLabel, hoursLabel, minsLabel, secsLabel);

        // Cấu hình gắn các Control sang Service để ủy quyền quản lý
        statusUiService.attachUiControls(
                statusMessageLabel, bidControlsContainer, statusOverlay,
                btnAutoBidToggle, submitBid, countdownTimer, autoBidManager
        );
        if (bidPriceChart != null) {
            bidPriceSeries = new XYChart.Series<>();
            bidPriceChart.getData().add(bidPriceSeries);
            bidPriceChart.setAnimated(false);
        }
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
                        ToastService.showError(itemNameLabel.getScene(), "Failed to load item data");
                    });
                    return null;
                });
    }

    private void initData(Item loadedItem) {
        this.item = loadedItem;
        this.myLastBid = loadedItem.getMyLastBid();
        autoBidManager.setLastBidderId(loadedItem.getCurrentTopPLayerId());

        if (ItemStatusConstants.BANNED.equalsIgnoreCase(loadedItem.getStoredStatus())) {
            displayDataItem(loadedItem);
            statusUiService.applyBannedStateView(loadedItem);
            updateAutoBidUI(false);
            connectToRealTimeBidding();
            return;
        }

        displayDataItem(loadedItem);
        statusUiService.applyAuctionStatusView(loadedItem, user.getId());
        connectToRealTimeBidding();
        loadBidHistory();
    }

    private void connectToRealTimeBidding() {
        network.setListener(this);
        boolean connected = network.connectToAuctionRoom(item.getId(), user.getId());
        if (!connected) {
            System.err.println("Failed to connect to auction room: " + item.getId());
        }
    }

    // ─── Realtime Network Event Handlers (Background) ─────────────────────────

    @Override
    public void onMessageReceived(ResponseMessage response) {
        String event = response.getMessage();

        switch (event) {
            case SocketEventConstants.EVENT_NEW_BID -> {
                if (!SocketEventConstants.STATUS_SUCCESS_LOWER.equals(response.getStatus())) return;
                BidPayload payload = extractPayload(response.getData(), BidPayload.class);
                if (payload != null) Platform.runLater(() -> uiHandleNewBid(payload));
            }
            case SocketEventConstants.EVENT_AUCTION_EXTENDED -> {
                JsonObject data = extractPayload(response.getData(), JsonObject.class);
                if (data != null && data.has("endTime")) {
                    Platform.runLater(() -> uiHandleAuctionExtended(data.get("endTime").getAsString()));
                }
            }
            case SocketEventConstants.EVENT_ITEM_BANNED -> {
                JsonObject data = extractPayload(response.getData(), JsonObject.class);
                if (data != null && data.has("itemId")) {
                    if (data.get("itemId").getAsString().equals(this.itemId)) {
                        Platform.runLater(this::uiHandleItemBanned);
                    }
                }
            }
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
        statusUiService.applyAuctionStatusView(item, user.getId());
        updateMinimumBidLabel();
        handleAutoBidLogic(bidPayload.getBidPrice(), bidPayload.getUserId());
        loadBidHistory();
    }

    private void uiHandleAuctionExtended(String rawEndTime) {
        try {
            LocalDateTime newEndTime = LocalDateTime.parse(rawEndTime);
            item.setEndTime(newEndTime);
            endTimeLabel.setText(newEndTime.format(DISPLAY_DATE_FORMAT));

            // Gọi qua Status Service để khởi động lại Countdown đích mới
            statusUiService.startCountdown(item, () -> statusUiService.applyAuctionStatusView(item, user.getId()));
        } catch (Exception e) {
            System.err.println("[AUCTION_EXTENDED] Error parsing end time: " + e.getMessage());
        }
    }

    private void uiHandleItemBanned() {
        statusUiService.applyBannedStateView(item);
        updateAutoBidUI(false);
        ToastService.showError(itemNameLabel.getScene(), "Sản phẩm này đã bị kiểm duyệt bởi quản trị viên.");
    }

    private <T> T extractPayload(Object incomingData, Class<T> type) {
        try {
            JsonElement element = gson.toJsonTree(incomingData);
            return gson.fromJson(element, type);
        } catch (Exception e) {
            System.err.println("Failed mapping json tree payload data: " + e.getMessage());
            return null;
        }
    }

    // ─── Manual & Auto Bidding Interactions ───────────────────────────────────

    @FXML
    public void bidHandle() {
        String input = bidAmountField.getText().trim();
        if (input.isEmpty()) {
            ToastService.showInfo(bidAmountField.getScene(), "Please enter a bid amount.");
            return;
        }
        try {
            double amount = Double.parseDouble(input);
            BidValidationService.ValidationResult result = bidValidationService.validate(item, user, amount, statusUiService.isOngoing(item));
            if (!result.ok()) {
                ToastService.showError(bidAmountField.getScene(), result.errorMessage());
                return;
            }
            network.sendBid(item.getId(), user.getId(), amount, "");
            bidAmountField.clear();
        } catch (NumberFormatException e) {
            ToastService.showError(bidAmountField.getScene(), "Invalid price format.");
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
        boolean isFormVisible = autoBidForm.isVisible();
        autoBidForm.setVisible(!isFormVisible);
        autoBidForm.setManaged(!isFormVisible);
    }

    @FXML
    private void startAutoBid() {
        if (!statusUiService.isOngoing(item)) {
            ToastService.showError(maxBidField.getScene(), "Auction is not active.");
            return;
        }
        try {
            double max = Double.parseDouble(maxBidField.getText().trim());
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
            userCurrentBidLabel.setText(isLeading
                    ? String.format("Your current bid: $ %.0f (Leading)", item.getCurrentPrice())
                    : "Auto-bidding...");
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
                topBidderId, serverCurrentPrice, user.getId(), myLastBid, statusUiService.isOngoing(item));

        switch (decision.type()) {
            case AUCTION_ENDED -> stopAutoBid();
            case INACTIVE -> {}
            case LEADING -> userCurrentBidLabel.setText(decision.statusText());
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

    // ─── Bid History Render ───────────────────────────────────────────────────

    private void loadBidHistory() {
        bidHistoryService.getHistory(itemId)
                .thenAccept(bids -> Platform.runLater(() -> renderBidHistory(bids)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> ToastService.showError(historyBidContainer.getScene(), "Failed to load bid history"));
                    return null;
                });
    }

    private void renderBidHistory(List<BidHistoryItemDTO> bids) {
        historyBidContainer.getChildren().clear();
        renderBidPriceChart(bids);

        if (bids == null || bids.isEmpty()) {
            totalBidsLabel.setText("0 bids");
            toggleHistoryScroll(false);
            return;
        }

        toggleHistoryScroll(true);
        int totalCount = bids.size();
        totalBidsLabel.setText(totalCount + " bids");
        for (int i = 0; i < totalCount; i++) {
            historyBidContainer.getChildren().add(
                    BidHistoryService.createBidRow(totalCount - i, bids.get(i), user.getUsername())
            );
        }
    }

    private void renderBidPriceChart(List<BidHistoryItemDTO> bids) {
        if (bidPriceChart == null || bidPriceSeries == null) {
            return;
        }
        bidPriceSeries.getData().clear();
        if (bids == null || bids.isEmpty()) {
            return;}
        List<BidHistoryItemDTO> copy = new ArrayList<>(bids);
        Collections.reverse(copy);
        DateTimeFormatter chartFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (BidHistoryItemDTO bid : copy) {
            if (bid.bidTime == null)
                continue;
            String time = bid.bidTime.format(chartFormat);
            bidPriceSeries.getData().add(new XYChart.Data<>(time, bid.bidPrice));
        }
    }

    private void toggleHistoryScroll(boolean visible) {
        if (historyScrollPane != null) {
            historyScrollPane.setVisible(visible);
            historyScrollPane.setManaged(visible);
        }
    }

    // ─── Core Display & Timers ────────────────────────────────────────────────

    private void displayDataItem(Item currentItem) {
        ClientImageUtil.setupThumbnailGallery(
                thumbnailContainer, itemImage, currentItem.getImagesPath(),
                THUMB_WIDTH, THUMB_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT
        );

        setupWrappedDescriptionText();

        itemNameLabel.setText(currentItem.getName());
        sellerLabel.setText(currentItem.getSellerId());
        currentPriceLabel.setText(String.format("$ %.0f", currentItem.getCurrentPrice()));
        startPriceLabel.setText(String.format("$ %.0f", currentItem.getStartingPrice()));
        bidStepLabel.setText(String.format("$ %.0f", currentItem.getBidStep()));

        startTimeLabel.setText(currentItem.getStartTime() != null ? currentItem.getStartTime().format(DISPLAY_DATE_FORMAT) : "N/A");
        endTimeLabel.setText(currentItem.getEndTime() != null ? currentItem.getEndTime().format(DISPLAY_DATE_FORMAT) : "N/A");

        if (btnSuggestStep1 != null) btnSuggestStep1.setText(String.format("$ %.0f", currentItem.getBidStep()));
        if (btnSuggestStep2 != null) btnSuggestStep2.setText(String.format("$ %.0f", currentItem.getBidStep() * 2));

        // Kích hoạt Countdown đồng bộ thông qua Service ủy quyền
        statusUiService.startCountdown(item, () -> statusUiService.applyAuctionStatusView(item, user.getId()));
        updateMinimumBidLabel();
    }

    private void setupWrappedDescriptionText() {
        itemDesLabel.setMinHeight(100);
        itemDesLabel.setText(item.getDescription());
        itemDesLabel.setWrapText(true);
        itemDesLabel.setMaxWidth(Double.MAX_VALUE);
        itemDesLabel.prefWidthProperty().bind(itemDesLabel.getParent().layoutBoundsProperty().map(b -> b.getWidth()));
        itemDesLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        itemDesLabel.setMinHeight(Region.USE_PREF_SIZE);
    }

    private void updateMinimumBidLabel() {
        if (item == null || minimumBidLabel == null) return;
        minimumBidLabel.setText(String.format(
                "Your last bid: $ %.0f  (Min next: $ %.0f)",
                myLastBid, item.getCurrentPrice() + item.getBidStep()
        ));
    }
}