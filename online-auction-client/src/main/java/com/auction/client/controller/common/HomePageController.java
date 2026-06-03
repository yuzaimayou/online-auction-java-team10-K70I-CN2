package com.auction.client.controller.common;

import com.auction.client.network.AuctionSocketClient;
import com.auction.client.service.ItemsService;
import com.auction.client.ui.item.ItemCardFactory;
import com.auction.client.util.NavigationUtil;
import com.auction.client.ui.util.ToastUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HomePageController {

    /** Kết nối mạng duy nhất thông qua cơ chế Socket Client. */
    private final AuctionSocketClient network = AuctionSocketClient.getInstance();

    /** Quản lý thông tin sản phẩm đấu giá  */
    private final ItemsService itemsService = ItemsService.getInstance();

    private String currentCategory  = "ALL";

    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private FlowPane ongoingAuctionsContainer;
    @FXML
    private FlowPane upcomingAuctionsContainer;
    @FXML
    private FlowPane endedAuctionsContainer;
    @FXML
    private VBox ongoingSection;
    @FXML
    private VBox upcomingSection;
    @FXML
    private VBox endedSection;
    @FXML
    private NavBarController navBarController;


    public HomePageController() {}
    private final PauseTransition debounce = new PauseTransition(Duration.millis(100));


    @FXML
    public void initialize() {
        network.leaveRoom();

        SearchStoreController.searchQueryProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(event -> fetchItemsFromServer());
            debounce.playFromStart();
        });

        mainScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            double containerWidth = newVal.doubleValue() - 40;
            ongoingAuctionsContainer.setPrefWidth(containerWidth);
            upcomingAuctionsContainer.setPrefWidth(containerWidth);
            endedAuctionsContainer.setPrefWidth(containerWidth);
        });

        // Lấy danh sách dữ liệu từ mạng
        fetchItemsFromServer();
    }

    /**
     * Gọi nghiệp vụ từ Service xử lý dữ liệu ngầm để lấy Map sản phẩm đã phân nhóm
     */
    private void fetchItemsFromServer() {
        String search = SearchStoreController.getSearchQuery();

        itemsService.getFilteredAndGroupedItems(search, currentCategory)
                .thenAccept(groupedItems -> {
                    if (search.equals(SearchStoreController.getSearchQuery())) {
                        processFetchResponse(groupedItems);
                    }
                })
                .exceptionally(this::processFetchException);
    }
    /**
     * Tiếp nhận kết quả Map dữ liệu và điều phối về Luồng giao diện.
     * @param groupedItems Bản đồ lưu trữ danh sách sản phẩm đã được phân nhóm theo trạng thái
     */
    private void processFetchResponse(Map<AuctionStatus, List<ItemSummary>> groupedItems) {
        Platform.runLater(() -> loadItemsToUI(groupedItems));
    }

    private Void processFetchException(Throwable e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            if (mainScrollPane.getScene() != null) {
                ToastUtil.showError(mainScrollPane.getScene(), "Cannot load auction items.");
            }
        });
        return null;
    }

    public void loadItemsToUI(Map<AuctionStatus, List<ItemSummary>> groupedItems) {
        clearAllContainers();

        if (groupedItems == null || groupedItems.isEmpty()) {
            updateSectionVisibility(0, 0, 0);
            return;
        }

        List<ItemSummary> ongoingList = groupedItems.get(AuctionStatus.ONGOING);
        List<ItemSummary> upcomingList = groupedItems.get(AuctionStatus.UPCOMING);
        List<ItemSummary> endedList = groupedItems.get(AuctionStatus.ENDED);

        // Dữ liệu sản phẩm Ongoing
        if (ongoingList != null) {
            ongoingList.forEach(item -> {
                try { ongoingAuctionsContainer.getChildren().add(ItemCardFactory.createCard(item)); }
                catch (IOException e) { System.err.println("Failed to load ongoing card: " + item.getName()); }
            });
        }

        // Dữ liệu sản phẩm Upcoming
        if (upcomingList != null) {
            upcomingList.forEach(item -> {
                try { upcomingAuctionsContainer.getChildren().add(ItemCardFactory.createCard(item)); }
                catch (IOException e) { System.err.println("Failed to load upcoming card: " + item.getName()); }
            });
        }

        // Dữ liệu sản phẩm Ended
        if (endedList != null) {
            endedList.forEach(item -> {
                try { endedAuctionsContainer.getChildren().add(ItemCardFactory.createCard(item)); }
                catch (IOException e) { System.err.println("Failed to load ended card: " + item.getName()); }
            });
        }

        int ongoingCount = ongoingList != null ? ongoingList.size() : 0;
        int upcomingCount = upcomingList != null ? upcomingList.size() : 0;
        int endedCount = endedList != null ? endedList.size() : 0;
        updateSectionVisibility(ongoingCount, upcomingCount, endedCount);
    }

    /**
     * Tiếp nhận và xử lý sự kiện click chuột chọn danh mục sản phẩm từ người dùng.
     */
    @FXML
    private void handleCategoryClick(MouseEvent event) {
        VBox clickedBox = (VBox) event.getSource();
        String rawId = clickedBox.getId();
        if (rawId == null || rawId.isBlank()) return;

        String targetCategory = rawId.substring(0, 1).toUpperCase() + rawId.substring(1).toLowerCase();
        currentCategory = targetCategory.equalsIgnoreCase(currentCategory) ? "ALL" : targetCategory;

        ItemCardFactory.updateCategoryUIStyle(clickedBox, currentCategory);
        fetchItemsFromServer();
    }

    /**
     * Tiếp nhận sự kiện yêu cầu chuyển hướng người dùng sang giao diện Tạo sản phẩm đấu giá mới.
     */
    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        NavigationUtil.handleSwitchToAuctionFormPage(event);
    }

    /**
     * Làm mới lại danh sách sản phẩm bằng cách tải lại từ mạng.
     */
    public void refreshItems() {
        itemsService.clearCache();
        fetchItemsFromServer();
    }

    /**
     * Ra lệnh cho khối NavBarController con nạp lại thông tin số dư / tài khoản người dùng hiện tại.
     */
    public void refreshNavBarInfo() {
        if (navBarController != null) {
            navBarController.refreshUserInfo();
        }
    }

    /**
     * Dọn sạch tất cả các Node đồ họa cũ ra khỏi 3 ngăn chứa màn hình chính.
     */
    private void clearAllContainers() {
        ongoingAuctionsContainer.getChildren().clear();
        upcomingAuctionsContainer.getChildren().clear();
        endedAuctionsContainer.getChildren().clear();
    }

    private void updateSectionVisibility(int ongoing, int upcoming, int ended) {
        setSectionVisible(ongoingSection,  ongoing  > 0);
        setSectionVisible(upcomingSection, upcoming > 0);
        setSectionVisible(endedSection,    ended    > 0);
    }

    private void setSectionVisible(VBox section, boolean visible) {
        section.setVisible(visible);
        section.setManaged(visible);
    }
}