package com.auction.client.controller.common;

import com.auction.client.network.AuctionSocketClient;
import com.auction.client.service.ItemsService;
import com.auction.client.ui.item.ItemCardFactory;
import com.auction.client.util.NavigationUtil;
import com.auction.client.ui.util.ToastUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

/**
 * Trách nhiệm:
 * 1. Lắng nghe thay đổi (Search, Category, Resize).
 * 2. Gọi ItemsService để lấy dữ liệu.
 * 3. Yêu cầu ItemCardFactory tạo giao diện và gắn vào vùng chứa.
 */
public class HomePageController {

    private final AuctionSocketClient network = AuctionSocketClient.getInstance();
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        network.leaveRoom();

        SearchStoreController.searchQueryProperty().addListener(
                (obs, oldVal, newVal) -> fetchItemsFromServer()
        );

        // Bind chiều rộng linh hoạt cho các vùng chứa
        mainScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            double containerWidth = newVal.doubleValue() - 40;
            ongoingAuctionsContainer.setPrefWidth(containerWidth);
            upcomingAuctionsContainer.setPrefWidth(containerWidth);
            endedAuctionsContainer.setPrefWidth(containerWidth);
        });

        fetchItemsFromServer();
    }

    // ── Data Fetching ─────────────────────────────────────────────────────────
    private void fetchItemsFromServer() {
        String search = SearchStoreController.getSearchQuery();
        itemsService.getItems(search, currentCategory)
                .thenAccept(this::processFetchResponse)
                .exceptionally(this::processFetchException);
    }

    private void processFetchResponse(List<ItemSummary> items) {
        Platform.runLater(() -> loadItemsToUI(items));
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

    // ── UI Rendering ──────────────────────────────────────────────────────────
    public void loadItemsToUI(List<ItemSummary> itemsFromServer) {
        clearAllContainers();

        if (itemsFromServer == null || itemsFromServer.isEmpty()) {
            updateSectionVisibility(0, 0, 0);
            return;
        }

        int ongoingCount = 0, upcomingCount = 0, endedCount = 0;

        for (ItemSummary item : itemsFromServer) {
            if (item.getStatus() == AuctionStatus.BANNED) continue;

            try {
                VBox cardBox = ItemCardFactory.createCard(item);
                AuctionStatus status = AuctionStatus.compute(item.getStartTime(), item.getEndTime());

                switch (status) {
                    case ONGOING  -> { ongoingAuctionsContainer.getChildren().add(cardBox);  ongoingCount++;  }
                    case UPCOMING -> { upcomingAuctionsContainer.getChildren().add(cardBox); upcomingCount++; }
                    case ENDED    -> { endedAuctionsContainer.getChildren().add(cardBox);    endedCount++;    }
                }
            } catch (IOException e) {
                System.err.println("Failed to load item card: " + item.getName());
                e.printStackTrace();
            }
        }

        updateSectionVisibility(ongoingCount, upcomingCount, endedCount);
    }

    // ── Event Handlers ────────────────────────────────────────────────────────
    @FXML
    private void handleCategoryClick(MouseEvent event) {
        VBox clickedBox = (VBox) event.getSource();
        String rawId = clickedBox.getId();
        if (rawId == null || rawId.isBlank()) return;

        String targetCategory = rawId.substring(0, 1).toUpperCase() + rawId.substring(1).toLowerCase();

        currentCategory = targetCategory.equalsIgnoreCase(currentCategory) ? "ALL" : targetCategory;

        updateCategoryUIStyle(clickedBox);
        fetchItemsFromServer();
    }

    @FXML
    public void handleSwitchToAuctionFormPage(ActionEvent event) {
        NavigationUtil.handleSwitchToAuctionFormPage(event);
    }

    public void refreshItems() {
        fetchItemsFromServer();
    }

    public void refreshNavBarInfo() {
        if (navBarController != null) {
            navBarController.refreshUserInfo();
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
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

    private void updateCategoryUIStyle(VBox clickedBox) {
        for (Node node : clickedBox.getParent().getChildrenUnmodifiable()) {
            if (node instanceof VBox) {
                node.getStyleClass().remove("active-category");
            }
        }
        if (!"ALL".equals(currentCategory)) {
            clickedBox.getStyleClass().add("active-category");
        }
    }
}