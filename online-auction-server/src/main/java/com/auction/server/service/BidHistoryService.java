package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service quản lý lịch sử bid và dữ liệu cho Price Curve
 * Requirement 3.2.5: Bid History Visualization - Realtime Price Curve
 */
public class BidHistoryService {
    private BiddingService biddingService;

    public BidHistoryService(BiddingService biddingService) {
        this.biddingService = biddingService;
    }

    /**
     * Requirement 3.2.5: Lấy dữ liệu cho Price Curve
     * Trục X: Thời gian (timestamp)
     * Trục Y: Giá đấu hiện tại
     */
    public List<PriceCurvePoint> getPriceCurveData(String auctionId) throws Exception {
        Auction auction = biddingService.getAuction(auctionId);
        List<BidTransaction> bidHistory = auction.getBidHistory();

        List<PriceCurvePoint> curveData = new ArrayList<>();

        curveData.add(new PriceCurvePoint(
                auction.getItem().getStartTime(),
                auction.getItem().getStartingPrice(),
                "Start"
        ));

        for (BidTransaction bid : bidHistory) {
            curveData.add(new PriceCurvePoint(
                    bid.getBidTime(),
                    bid.getBidAmount(),
                    bid.getBidderId()
            ));
        }

        return curveData;
    }

    /**
     * Model cho Price Curve data point
     */
    public static class PriceCurvePoint {
        public LocalDateTime timestamp;
        public double price;
        public String bidderId;

        public PriceCurvePoint(LocalDateTime timestamp, double price, String bidderId) {
            this.timestamp = timestamp;
            this.price = price;
            this.bidderId = bidderId;
        }

        @Override
        public String toString() {
            return timestamp + " | Price: VND" + price + " | Bidder: " + bidderId;
        }
    }

    /**
     * Requirement 3.2.5: Lấy lịch sử bid chi tiết
     */
    public List<BidHistoryDetail> getBidHistoryDetailed(String auctionId) throws Exception {
        Auction auction = biddingService.getAuction(auctionId);
        List<BidTransaction> bidHistory = auction.getBidHistory();

        List<BidHistoryDetail> details = new ArrayList<>();

        for (int i = 0; i < bidHistory.size(); i++) {
            BidTransaction bid = bidHistory.get(i);
            double previousPrice = (i == 0) ?
                    auction.getItem().getStartingPrice() :
                    bidHistory.get(i - 1).getBidAmount();

            details.add(new BidHistoryDetail(
                    i + 1,
                    bid.getBidderId(),
                    bid.getBidAmount(),
                    bid.getBidAmount() - previousPrice,
                    bid.getBidTime(),
                    bid.isAutoBid()
            ));
        }

        return details;
    }

    /**
     * Model cho bid history detail
     */
    public static class BidHistoryDetail {
        public int bidNumber;
        public String bidderId;
        public double bidAmount;
        public double priceIncrease;
        public LocalDateTime bidTime;
        public boolean isAutoBid;

        public BidHistoryDetail(int bidNumber, String bidderId, double bidAmount,
                                double priceIncrease, LocalDateTime bidTime, boolean isAutoBid) {
            this.bidNumber = bidNumber;
            this.bidderId = bidderId;
            this.bidAmount = bidAmount;
            this.priceIncrease = priceIncrease;
            this.bidTime = bidTime;
            this.isAutoBid = isAutoBid;
        }

        @Override
        public String toString() {
            return String.format("Bid #%d | Bidder: %s | Amount: $%.2f | +$%.2f | %s %s",
                    bidNumber, bidderId, bidAmount, priceIncrease, bidTime,
                    isAutoBid ? "(Auto)" : "");
        }
    }

    /**
     * Lấy thống kê bid
     */
    public BidStatistics getBidStatistics(String auctionId) throws Exception {
        Auction auction = biddingService.getAuction(auctionId);
        List<BidTransaction> bidHistory = auction.getBidHistory();

        if (bidHistory.isEmpty()) {
            return new BidStatistics(0, 0, 0, 0);
        }

        int totalBids = bidHistory.size();
        double minBid = bidHistory.stream()
                .mapToDouble(BidTransaction::getBidAmount)
                .min()
                .orElse(0);
        double maxBid = bidHistory.stream()
                .mapToDouble(BidTransaction::getBidAmount)
                .max()
                .orElse(0);
        double avgBid = bidHistory.stream()
                .mapToDouble(BidTransaction::getBidAmount)
                .average()
                .orElse(0);

        return new BidStatistics(totalBids, minBid, maxBid, avgBid);
    }

    /**
     * Model cho bid statistics
     */
    public static class BidStatistics {
        public int totalBids;
        public double minBid;
        public double maxBid;
        public double avgBid;

        public BidStatistics(int totalBids, double minBid, double maxBid, double avgBid) {
            this.totalBids = totalBids;
            this.minBid = minBid;
            this.maxBid = maxBid;
            this.avgBid = avgBid;
        }

        @Override
        public String toString() {
            return String.format("Total Bids: %d | Min: $%.2f | Max: $%.2f | Avg: $%.2f",
                    totalBids, minBid, maxBid, avgBid);
        }
    }
}