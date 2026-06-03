package com.auction.client.controller.auction;

import com.auction.client.service.AutoBidService;
import com.auction.client.ui.auction.BidPanelView;
import com.auction.client.ui.item.ItemStatusRendered;
import com.auction.client.util.CountdownTimerUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import javafx.application.Platform;

import java.time.LocalDateTime;

public class BidPanelController {

    private final CountdownTimerUtil countdownTimer;
    private final AutoBidService autoBidManager;
    private final ItemStatusRendered statusService;
    private final BidPanelView view;

    public BidPanelController(
            CountdownTimerUtil countdownTimer,
            AutoBidService autoBidManager,
            BidPanelView view
    ) {
        this.countdownTimer = countdownTimer;
        this.autoBidManager = autoBidManager;
        this.view           = view;
        this.statusService  = new ItemStatusRendered();
    }

    /**
     * Hàm hạt nhân: Vừa cập nhật giao diện trạng thái, vừa tự động kích hoạt đếm ngược.
     * Giải quyết triệt để lỗi hàm không được gọi.
     */
    public void refreshAndSync(Item item, String currentUserId) {
        if (item == null) return;

        boolean isOwner = item.getSellerId().equals(currentUserId);
        AuctionStatus status = statusService.resolveStatus(item);

        if (isOwner) {
            view.showOwnerRestrictedState(item.getSellerId());
        } else {
            switch (status) {
                case ONGOING -> view.showOngoingState(autoBidManager.isActive());
                case UPCOMING -> view.showUpcomingState();
                case ENDED -> view.showEndedState();
                case BANNED -> applyBannedStateView(item);
            }
        }


        if (status == AuctionStatus.UPCOMING || status == AuctionStatus.ONGOING) {
            LocalDateTime target = (status == AuctionStatus.UPCOMING)
                    ? item.getStartTime()
                    : item.getEndTime();

            countdownTimer.startFor(target, () -> Platform.runLater(() -> refreshAndSync(item, currentUserId)));
        } else {
            countdownTimer.stop();
        }
    }
    public void applyBannedStateView(Item item) {
        countdownTimer.stop();
        autoBidManager.deactivate();
        item.setStatus(AuctionStatus.BANNED);
        view.showBannedState();
    }
}