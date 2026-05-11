package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Service quan ly lich su bid va du lieu cho Price Curve
 * Requirement 3.2.5: Bid History Visualization - Realtime Price Curve
 */
public class BidHistoryService {
    private final BiddingService biddingService;

    public BidHistoryService(BiddingService biddingService) {
        this.biddingService = biddingService;
    }

    /**
     * Requirement 3.2.5: Lay du lieu cho Price Curve
     * Truc X: Thoi gian (timestamp)
     * Truc Y: Gia dau hien tai
     */
    public List<PriceCurvePoint> getPriceCurveData(String auctionId) throws Exception {
        Auction auction = biddingService.getAuction(auctionId);
        List<BidTransaction> bidHistory = new ArrayList<>(auction.getBidHistory());

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
     * Requirement 3.2.5: Lay lich su bid chi tiet
     */
    public List<BidHistoryDetail> getBidHistoryDetailed(String auctionId) throws Exception {
        Auction auction = biddingService.getAuction(auctionId);
        List<BidTransaction> bidHistory = new ArrayList<>(auction.getBidHistory());

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
     * Lay thong ke bid
     */
    public BidStatistics getBidStatistics(String auctionId) throws Exception {
        Auction auction = biddingService.getAuction(auctionId);
        List<BidTransaction> bidHistory = new ArrayList<>(auction.getBidHistory());

        if (bidHistory.isEmpty()) {
            return new BidStatistics(0, 0, 0, 0);
        }

        DoubleSummaryStatistics stats = bidHistory.stream()
                .mapToDouble(BidTransaction::getBidAmount)
                .summaryStatistics();

        return new BidStatistics(
                (int) stats.getCount(),
                stats.getMin(),
                stats.getMax(),
                stats.getAverage()
        );
    }

    /**
     * Model cho Price Curve data point
     */
    public static class PriceCurvePoint {
        private final LocalDateTime timestamp;
        private final double price;
        private final String bidderId;

        public PriceCurvePoint(LocalDateTime timestamp, double price, String bidderId) {
            this.timestamp = timestamp;
            this.price = price;
            this.bidderId = bidderId;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public double getPrice() {
            return price;
        }

        public String getBidderId() {
            return bidderId;
        }

        @Override
        public String toString() {
            return timestamp + " | Price: VND" + price + " | Bidder: " + bidderId;
        }
    }

    /**
     * Model cho bid history detail
     */
    public static class BidHistoryDetail {
        private final int bidNumber;
        private final String bidderId;
        private final double bidAmount;
        private final double priceIncrease;
        private final LocalDateTime bidTime;
        private final boolean autoBid;

        public BidHistoryDetail(int bidNumber, String bidderId, double bidAmount,
                                double priceIncrease, LocalDateTime bidTime, boolean autoBid) {
            this.bidNumber = bidNumber;
            this.bidderId = bidderId;
            this.bidAmount = bidAmount;
            this.priceIncrease = priceIncrease;
            this.bidTime = bidTime;
            this.autoBid = autoBid;
        }

        public int getBidNumber() {
            return bidNumber;
        }

        public String getBidderId() {
            return bidderId;
        }

        public double getBidAmount() {
            return bidAmount;
        }

        public double getPriceIncrease() {
            return priceIncrease;
        }

        public LocalDateTime getBidTime() {
            return bidTime;
        }

        public boolean isAutoBid() {
            return autoBid;
        }

        @Override
        public String toString() {
            return String.format("Bid #%d | Bidder: %s | Amount: $%.2f | +$%.2f | %s %s",
                    bidNumber, bidderId, bidAmount, priceIncrease, bidTime,
                    autoBid ? "(Auto)" : "");
        }
    }

    /**
     * Model cho bid statistics
     */
    public static class BidStatistics {
        private final int totalBids;
        private final double minBid;
        private final double maxBid;
        private final double avgBid;

        public BidStatistics(int totalBids, double minBid, double maxBid, double avgBid) {
            this.totalBids = totalBids;
            this.minBid = minBid;
            this.maxBid = maxBid;
            this.avgBid = avgBid;
        }

        public int getTotalBids() {
            return totalBids;
        }

        public double getMinBid() {
            return minBid;
        }

        public double getMaxBid() {
            return maxBid;
        }

        public double getAvgBid() {
            return avgBid;
        }

        @Override
        public String toString() {
            return String.format("Total Bids: %d | Min: $%.2f | Max: $%.2f | Avg: $%.2f",
                    totalBids, minBid, maxBid, avgBid);
        }
    }
}