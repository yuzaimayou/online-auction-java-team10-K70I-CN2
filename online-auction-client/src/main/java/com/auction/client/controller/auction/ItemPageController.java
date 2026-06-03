package com.auction.client.controller.auction;

import com.auction.client.network.AuctionRoomListener;
import com.auction.client.network.AuctionSocketClient;
import com.auction.client.network.BidHistoryApiClient;
import com.auction.client.service.AutoBidService;
import com.auction.client.service.ItemsService;
import com.auction.client.service.UserSession;
import com.auction.client.service.WalletService;
import com.auction.client.ui.auction.AutoBidPaneWrapper;
import com.auction.client.ui.auction.BidHistoryPanel;
import com.auction.client.ui.auction.BidPanelView;
import com.auction.client.ui.image.ImageCardFactory;
import com.auction.client.ui.item.ItemStatusRendered;
import com.auction.client.ui.util.ToastUtil;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.CountdownTimerUtil;
import com.auction.client.util.DateTimeUtil;
import com.auction.client.validation.AutoBidValidationService;
import com.auction.client.validation.BidValidationService;
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public class ItemPageController {

    private static final double IMAGE_WIDTH = 1400.0;
    private static final double IMAGE_HEIGHT = 900.0;
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
    @FXML
    private ScrollPane historyScrollPane;
    @FXML
    private VBox historyBidContainer;
    @FXML
    private Label totalBidsLabel;
    @FXML
    private AreaChart<String, Number> bidPriceChart;
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

    // Các Sub-Controller và thành phần giao diện đã được phân rã cấu trúc MVC
    private BidPanelController bidPanel;
    private AutoBidController autoBidHandler;
    private BidHistoryController historyController;
    private XYChart.Series<String, Number> bidPriceSeries;

    private String itemId;
    private Item item;
    private double myLastBid = 0.0;

    private final User user = UserSession.getInstance().getLoggedInUser();
    private final AuctionSocketClient network = AuctionSocketClient.getInstance();
    private final ItemsService itemsService = ItemsService.getInstance();
    private final BidHistoryApiClient bidHistoryApiClient = BidHistoryApiClient.getInstance();

    private final AutoBidService autoBidManager = new AutoBidService();
    private final BidValidationService bidValidationService = new BidValidationService();
    private final AutoBidValidationService autoBidValidationService = new AutoBidValidationService();
    private final ItemStatusRendered statusUiService = new ItemStatusRendered();
    private CountdownTimerUtil countdownTimer;

    @FXML
    public void initialize() {
        itemImage.setPreserveRatio(false);
        itemImage.setSmooth(true);
        itemImage.setCache(false);

        ClientImageUtil.makeResponsiveCover(itemImage, mainImageContainer, 16);
        countdownTimer = new CountdownTimerUtil(daysLabel, hoursLabel, minsLabel, secsLabel);

        // KHỐI 1: Khởi tạo cụm PANEL ĐẤU GIÁ
        BidPanelView bidPanelView = new BidPanelView(
                statusMessageLabel, bidControlsContainer, statusOverlay, btnAutoBidToggle, submitBid
        );
        bidPanel = new BidPanelController(countdownTimer, autoBidManager, bidPanelView);

        if (bidPriceChart != null) {
            bidPriceSeries = new XYChart.Series<>();
            bidPriceChart.getData().add(bidPriceSeries);
            bidPriceChart.setAnimated(false);
        }

        // KHỐI 2: Khởi tạo cụm TỰ ĐỘNG ĐẤU GIÁ
        AutoBidPaneWrapper autoBidPaneWrapper = new AutoBidPaneWrapper(
                autoBidForm, autoBidActiveStatus, maxBidField, autoBidStepField,
                userCurrentBidLabel, btnAutoBidToggle, submitBid
        );
        autoBidHandler = new AutoBidController(
                autoBidManager, autoBidValidationService, network, user, statusUiService,
                autoBidPaneWrapper, () -> item, () -> myLastBid
        );

        // KHỐI 3: Khởi tạo cụm LỊCH SỬ ĐẤU GIÁ
        BidHistoryPanel bidHistoryPanel = new BidHistoryPanel(
                historyBidContainer, historyScrollPane, totalBidsLabel, bidPriceSeries
        );
        historyController = new BidHistoryController(bidHistoryApiClient, user, bidHistoryPanel);

        itemImage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.F5) {
                        if (itemId != null) {
                            setItemId(itemId); // reload lại toàn bộ item data
                        }
                    }
                });
            }
        });
    }

    public void dispose() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        autoBidManager.deactivate();
        network.leaveRoom();
        System.out.println("[ItemPageController] Safe disposed.");
    }

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
        autoBidManager.setLastBidderId(loadedItem.getCurrentBidderId());

        // Đổ dữ liệu tĩnh lên màn hình trước
        displayDataItem(loadedItem);
        connectToRealTimeBidding();
        historyController.load(itemId);

        // 🎯 CHỈ CẦN 1 DÒNG NÀY: Ủy quyền toàn bộ việc kiểm tra trạng thái (kể cả BANNED)
        // và tự kích hoạt luồng đếm ngược khép kín cho BidPanelController tự xử lý.
        bidPanel.refreshAndSync(loadedItem, user.getId());
    }

    private void connectToRealTimeBidding() {
        network.setAuctionRoomListener(new AuctionRoomListener() {
            @Override
            public void onNewBid(BidPayload p) {
                Platform.runLater(() -> uiHandleNewBid(p));
            }

            @Override
            public void onAuctionExtended(LocalDateTime t) {
                System.out.println("[Auction Extended] New end time: " + t);
                Platform.runLater(() -> uiHandleAuctionExtended(t));
            }

            @Override
            public void onItemBanned(String id) {
                if (id.equals(itemId)) {
                    Platform.runLater(() -> uiHandleItemBanned());
                }
            }

            @Override
            public void onAutoBidState(AutoBidPayload data) {
                boolean isActive = data != null && Boolean.TRUE.equals(data.getIsActive());

                if (isActive) {
                    Platform.runLater(() -> {
                        autoBidManager.activate(data.getMaxBid(), data.getIncrement());
                        autoBidHandler.updateUi(true);
                        autoBidHandler.updateUIAutoBid(data.getMaxBid(), data.getIncrement());
                    });
                } else {
                    Platform.runLater(() -> {
                        autoBidManager.deactivate();
                        autoBidHandler.updateUi(false);
                    });
                }
            }
        });

        boolean connected = network.connectToAuctionRoom(item.getId(), user.getId());
        if (!connected) {
            System.err.println("Failed to connect to auction room: " + item.getId());
        }
    }

    @FXML
    public void bidHandle() {
        String input = bidAmountField.getText().trim();
        if (input.isEmpty()) {
            ToastUtil.showInfo(bidAmountField.getScene(), "Please enter a bid amount.");
            return;
        }
        try {
            double amount = Double.parseDouble(input);
            BidValidationService.ValidationResult result = bidValidationService.validate(
                    item, user, amount, statusUiService.isOngoing(item));

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
        if (item != null) {
            bidAmountField.setText(String.format("%.0f", item.getCurrentPrice() + item.getBidStep()));
        }
    }

    @FXML
    private void handleSuggestStep2() {
        if (item != null) {
            bidAmountField.setText(String.format("%.0f", item.getCurrentPrice() + item.getBidStep() * 2));
        }
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

    private void uiHandleNewBid(BidPayload bidPayload) {
        autoBidManager.setLastBidderId(bidPayload.getUserId());
        item.setCurrentBidderId(bidPayload.getUserId());
        item.setCurrentPrice(bidPayload.getBidPrice());

        if (bidPayload.getUserId().equals(user.getId())) {
            this.myLastBid = bidPayload.getBidPrice();
        }

        currentPriceLabel.setText(statusUiService.formatPrice(item.getCurrentPrice()));

        //  ĐỒNG BỘ: Gọi refreshAndSync để làm mới giao diện đấu giá và tính toán lại luồng đếm
        bidPanel.refreshAndSync(item, user.getId());

        autoBidHandler.handleDecision(bidPayload.getBidPrice(), bidPayload.getUserId());
        updateMinimumBidLabel();
        historyController.load(itemId);

        WalletService.getInstance().fetchAndSync();
    }

    private void uiHandleAuctionExtended(LocalDateTime newEndTime) {
        item.setEndTime(newEndTime);
        endTimeLabel.setText(DateTimeUtil.format(newEndTime));

        // ĐỒNG BỘ: Gọi refreshAndSync khi có sự mở rộng gia hạn thời gian
        bidPanel.refreshAndSync(item, user.getId());
    }

    private void uiHandleItemBanned() {
        network.setAuctionRoomListener(null);
        autoBidHandler.updateUi(false);

        // ĐỒNG BỘ: Đẩy thẳng lệnh khóa phiên đấu giá sang cho Sub-Controller xử lý dứt điểm
        bidPanel.applyBannedStateView(item);
    }

    private void displayDataItem(Item currentItem) {
        ImageCardFactory.setupThumbnailGallery(
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

        if (btnSuggestStep1 != null) {
            btnSuggestStep1.setText(String.format("$ %.0f", currentItem.getBidStep()));
        }
        if (btnSuggestStep2 != null) {
            btnSuggestStep2.setText(String.format("$ %.0f", currentItem.getBidStep() * 2));
        }

        updateMinimumBidLabel();
    }

    private void updateMinimumBidLabel() {
        if (item == null || minimumBidLabel == null) {
            return;
        }
        minimumBidLabel.setText(String.format(
                "Your last bid: $ %.0f  (Min next: $ %.0f)",
                myLastBid, item.getCurrentPrice() + item.getBidStep()
        ));
    }
}