package com.auction.client.controller.auction;

import com.auction.client.network.BidHistoryApiClient;
import com.auction.client.ui.auction.BidHistoryPanel;
import com.auction.shared.model.account.User;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import javafx.application.Platform;

import java.util.List;

/**
 * 🎮 SUB-CONTROLLER COMPONENT
 * Chịu trách nhiệm nhận tín hiệu từ luồng điều phối, gọi API mạng và điều hướng tầng View hiển thị.
 */
public class BidHistoryController {

    private final BidHistoryApiClient bidHistoryApiClient;
    private final User user;
    private final BidHistoryPanel view; // Giao tiếp lỏng qua giao diện View đã đóng gói

    public BidHistoryController(
            BidHistoryApiClient bidHistoryApiClient,
            User user,
            BidHistoryPanel view
    ) {
        this.bidHistoryApiClient = bidHistoryApiClient;
        this.user               = user;
        this.view               = view;
    }

    /**
     * Tải dữ liệu lịch sử đấu giá bất đồng bộ từ Server
     */
    public void load(String itemId) {
        bidHistoryApiClient.getHistory(itemId)
                .thenAccept(bids -> Platform.runLater(() -> render(bids)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> view.showLoadErrorMessage("Failed to load bid history"));
                    return null;
                });
    }

    private void render(List<BidHistoryItemDTO> bids) {
        // Đẩy dữ liệu sạch sang cho View tự kết xuất đồ họa
        view.render(bids, user.getUsername());
    }
}