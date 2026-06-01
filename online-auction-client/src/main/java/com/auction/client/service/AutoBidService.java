package com.auction.client.service;


/**
 * Trách nhiệm: Quản lý trạng thái vận hành và đưa ra quyết định nâng giá tự động.
 * Đã bóc tách toàn bộ logic xác thực dữ liệu đầu vào.
 */
public class AutoBidService {

    // State
    private boolean isActive = false;
    private double maxBidAmount  = 0;
    private double autoBidIncremental = 0;
    private String lastBidderId  = "";

    public enum DecisionType {
        LEADING,
        OUTBID_AND_REBID,
        MAX_REACHED,
        INACTIVE,
        AUCTION_ENDED
    }

    public record AutoBidDecision(
            DecisionType type,
            double nextBidPrice
    ) {}

    public void activate(double max, double step) {
        this.maxBidAmount       = max;
        this.autoBidIncremental = step;
        this.isActive           = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Ra quyết định Auto-bid dựa trên luồng dữ liệu mới từ Server
     */
    public AutoBidDecision decideBid(String topBidderId, double serverPrice, String currentUserId, boolean isAuctionOngoing) {
        this.lastBidderId = topBidderId;

        if (!isAuctionOngoing) {
            deactivate();
            return new AutoBidDecision(DecisionType.AUCTION_ENDED, 0);
        }

        if (!isActive) {
            return new AutoBidDecision(DecisionType.INACTIVE, 0);
        }

        // Nếu mình đang là người dẫn đầu
        if (currentUserId.equals(topBidderId)) {
            return new AutoBidDecision(DecisionType.LEADING, 0);
        }

        // Nếu giá hiện tại đã chạm hoặc vượt ngưỡng Max của mình
        if (serverPrice >= maxBidAmount) {
            deactivate();
            return new AutoBidDecision(DecisionType.MAX_REACHED, 0);
        }

        // Bị vượt mặt và vẫn trong giới hạn Max -> Tính toán giá bid tiếp theo
        double nextBid = serverPrice + autoBidIncremental;
        return new AutoBidDecision(DecisionType.OUTBID_AND_REBID, nextBid);
    }

    // Getters & Setters
    public boolean isActive() { return isActive; }
    public double getMaxBidAmount() { return maxBidAmount;}
    public double getAutoBidIncremental(){ return autoBidIncremental; }
    public String getLastBidderId() { return lastBidderId; }
    public void setLastBidderId(String id) { this.lastBidderId = id; }
}