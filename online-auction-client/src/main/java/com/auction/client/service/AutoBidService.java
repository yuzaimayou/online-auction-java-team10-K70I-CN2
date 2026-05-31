package com.auction.client.service;

import com.auction.shared.model.item.Item;

public class AutoBidService {

    // State
    private boolean isActive = false;
    private double maxBidAmount  = 0;
    private double autoBidIncremental = 0;
    private String lastBidderId  = "";
    private long lastAutoBidTime = 0;

    public record ValidationResult(boolean ok, String errorMessage) {
        public static ValidationResult success()             { return new ValidationResult(true,  null); }
        public static ValidationResult fail(String msg)     { return new ValidationResult(false, msg);  }
    }

    // Auto-bid decision trả về cho controller
    public enum DecisionType {
        LEADING,
        OUTBID_AND_REBID,
        MAX_REACHED,
        INACTIVE,
        AUCTION_ENDED }
    public record AutoBidDecision(
            DecisionType type,
            double nextBidPrice,
            String statusText
    ) {}

    // Validate trước khi activate
    public ValidationResult validate(Item item, double max, double step) {
        if (step <= 0) {
            return ValidationResult.fail("Increment amount must be greater than 0.");
        }
        if (step < item.getBidStep()) {
            return ValidationResult.fail(
                    String.format("Your increment step must be at least the minimum allowed ($ %.0f).", item.getBidStep()));
        }
        if (max <= item.getCurrentPrice()) {
            return ValidationResult.fail(
                    String.format("Max Bid ($ %.0f) must be higher than the Current Price ($ %.0f).", max, item.getCurrentPrice()));
        }
        if (step >= max) {
            return ValidationResult.fail(
                    String.format("Increment step ($ %.0f) cannot be equal or greater than your Max Bid ($ %.0f).", step, max));
        }
        double firstAutoBidPrice = item.getCurrentPrice() + step;
        if (firstAutoBidPrice > max) {
            return ValidationResult.fail(
                    String.format("The first auto-bid will be $ %.0f, which exceeds your Max Bid ($ %.0f). Please raise Max Bid or lower increment.",
                            firstAutoBidPrice, max));
        }
        return ValidationResult.success();
    }

    // ctivate / Deactivate
    public void activate(double max, double step) {
        this.maxBidAmount       = max;
        this.autoBidIncremental = step;
        this.isActive           = true;
    }
    public void deactivate() {
        this.isActive = false;
    }

    // Reaction to a new bid
    /**
     *
     * @param topBidderId     userId đang dẫn đầu theo server
     * @param serverPrice     giá hiện tại theo server
     * @param currentUserId   userId của người dùng hiện tại
     * @param myLastBid       giá bid cuối của người dùng (để tạo status text)
     * @param isAuctionOngoing trạng thái phiên đấu giá
     */
    public AutoBidDecision decideBid(String topBidderId, double serverPrice, String currentUserId, double myLastBid, boolean isAuctionOngoing) {
        this.lastBidderId = topBidderId;

        if (!isAuctionOngoing) {
            deactivate();
            return new AutoBidDecision(DecisionType.AUCTION_ENDED, 0, null);
        }
        if (!isActive) {
            return new AutoBidDecision(DecisionType.INACTIVE, 0, null);
        }
        if (currentUserId.equals(topBidderId)) {
            String text = String.format("Your current bid: $ %.0f (Leading)", serverPrice);
            return new AutoBidDecision(DecisionType.LEADING, 0, text);
        }
        if (serverPrice >= maxBidAmount) {
            deactivate();
            return new AutoBidDecision(DecisionType.MAX_REACHED, 0,
                    "Auto-bid stopped: Max limit reached!");
        }

        double nextBid = serverPrice + autoBidIncremental;
        String text = myLastBid > 0
                ? String.format("Your current bid: $ %.0f (Outbid — auto-bidding...)", myLastBid)
                : "Auto-bidding...";
        this.lastAutoBidTime = System.currentTimeMillis();
        return new AutoBidDecision(DecisionType.OUTBID_AND_REBID, nextBid, text);
    }

    // Getters
    public boolean isActive() { return isActive; }
    public double getMaxBidAmount() { return maxBidAmount;}
    public double getAutoBidIncremental(){ return autoBidIncremental; }
    public String getLastBidderId() { return lastBidderId; }
    public void setLastBidderId(String id) { this.lastBidderId = id; }
}