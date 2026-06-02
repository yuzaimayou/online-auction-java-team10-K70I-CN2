package com.auction.client.controller.auction;

import com.auction.client.network.AuctionSocketClient;
import com.auction.client.service.AutoBidService;
import com.auction.client.service.AutoBidService.AutoBidDecision;
import com.auction.client.ui.auction.AutoBidPaneWrapper;
import com.auction.client.ui.item.ItemStatusRendered;
import com.auction.client.validation.AutoBidValidationService;
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;

import java.util.function.Supplier;

/**
 * 🎮 CONTROLLER COMPONENT (Sub-Controller)
 * Chịu trách nhiệm tiếp nhận tương tác người dùng từ View,
 * ra lệnh cập nhật trạng thái cho Model, và điều hướng View hiển thị lại.
 */
public class AutoBidController {

    // Liên kết tới tầng Model
    private final AutoBidService autoBidManager;
    private final AutoBidValidationService autoBidValidator;
    private final AuctionSocketClient network;
    private final User user;
    private final ItemStatusRendered statusService;

    // Đồng bộ trạng thái dữ liệu động từ Controller tổng
    private final Supplier<Item> itemSupplier;
    private final Supplier<Double> myLastBidSupplier;

    // Liên kết tới tầng View nguyên bản (Đã bóc tách)
    private final AutoBidPaneWrapper view;

    public AutoBidController(
            AutoBidService autoBidManager,
            AutoBidValidationService autoBidValidator,
            AuctionSocketClient network,
            User user,
            ItemStatusRendered statusService,
            AutoBidPaneWrapper view, // Nhận đối tượng View đã gom nhóm
            Supplier<Item> itemSupplier,
            Supplier<Double> myLastBidSupplier
    ) {
        this.autoBidManager = autoBidManager;
        this.autoBidValidator = autoBidValidator;
        this.network = network;
        this.user = user;
        this.statusService = statusService;
        this.view = view;
        this.itemSupplier = itemSupplier;
        this.myLastBidSupplier = myLastBidSupplier;
    }

    public void toggleForm() {
        view.toggleFormVisibility();
    }

    public void start() {
        Item item = itemSupplier.get();
        if (!statusService.isOngoing(item)) {
            view.showErrorMessage("Auction is not active.");
            return;
        }
        try {
            // [C] Lấy dữ liệu thô từ View thông qua giao tiếp phương thức công khai
            double max = Double.parseDouble(view.getMaxBidInput());
            double step = Double.parseDouble(view.getAutoBidStepInput());

            // [M] Yêu cầu nghiệp vụ Model xác thực luật đấu giá
            AutoBidValidationService.ValidationResult result = autoBidValidator.validate(item, max, step);
            if (!result.ok()) {
                view.showErrorMessage(result.errorMessage());
                return;
            }

            // [M] Thay đổi trạng thái dữ liệu trong Model & Phát tín hiệu qua tầng mạng
            autoBidManager.activate(max, step);
            network.sendAutoBidRegister(item.getId(), user.getId(), max, step);

            // [V] Điều khiển View kết xuất đồ họa trạng thái mới
            boolean isSeller = item.getSellerId().equals(user.getId());
            view.renderActiveState(true, isSeller);
            view.showSuccessMessage("Auto-Bid activated!");

            boolean isLeading = user.getId().equals(autoBidManager.getLastBidderId());
            view.updateStatusText(isLeading
                    ? String.format("Your current bid: $ %.0f (Leading)", item.getCurrentPrice())
                    : "Auto-bidding...");
        } catch (NumberFormatException e) {
            view.showErrorMessage("Please enter valid numbers.");
        }
    }

    public void stop() {
        autoBidManager.deactivate();
        Item item = itemSupplier.get();
        if (item != null) {
            network.sendCancelAutoBid(item.getId(), user.getId());
        }
        boolean isSeller = item != null && item.getSellerId().equals(user.getId());
        view.renderActiveState(false, isSeller);
    }

    public void handleDecision(double serverPrice, String topBidderId) {
        Item item = itemSupplier.get();

        // [M] Thực thi xử lý thuật toán cốt lõi bên trong nghiệp vụ của Model
        AutoBidDecision decision = autoBidManager.decideBid(
                topBidderId, serverPrice, user.getId(), statusService.isOngoing(item));

        // [C] Nhận tín hiệu quyết định từ Model và điều hướng View hiển thị tương ứng
        switch (decision.type()) {
            case AUCTION_ENDED -> stop();
            case INACTIVE -> { }
            case LEADING -> {
                if (autoBidManager.isActive()) {
                    view.updateStatusText(String.format("Your current bid: $ %.0f", serverPrice));
                }
            }
            case MAX_REACHED -> {
                network.sendCancelAutoBid(item.getId(), user.getId());
                boolean isSeller = item != null && item.getSellerId().equals(user.getId());
                view.renderActiveState(false, isSeller);
                view.showInfoMessage("Auto-bid stopped: Max limit reached!");
            }
            case OUTBID_AND_REBID -> {
                double myLastBid = myLastBidSupplier.get();
                view.updateStatusText(myLastBid > 0
                        ? String.format("Your current bid: $ %.0f", myLastBid)
                        : "Your current bid: $ 0");
                network.sendBid(item.getId(), user.getId(), decision.nextBidPrice(), "");
            }
        }
    }

    public void updateUi(boolean active) {
        Item item = itemSupplier.get();
        boolean isSeller = item != null && item.getSellerId().equals(user.getId());
        view.renderActiveState(active, isSeller);
    }

    public void updateUIAutoBid(double maxBid, double step) {
        Item item = itemSupplier.get();
        double currentPrice = item != null ? item.getCurrentPrice() : 0;
        view.updateStatusText(String.format("Your current bid: $ %.0f", currentPrice));
    }
}