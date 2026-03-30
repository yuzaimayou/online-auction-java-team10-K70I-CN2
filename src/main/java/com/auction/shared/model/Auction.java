package com.auction.shared.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Quản lý một phiên đấu giá
 * Requirement 3.1.3: Tham gia đấu giá
 * Requirement 3.1.4: Kết thúc phiên đấu giá
 * Requirement 3.1.5: Xử lý lỗi & ngoại lệ
 */
public class Auction {
    private String auctionId;
    private Item item;
    private AuctionStatus status;
    private String winnerId;
    private double finalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<BidTransaction> bidHistory;

    public Auction(String auctionId, Item item) {
        // [ERROR HANDLING] Validate constructor parameters
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Auction ID cannot be empty!");
        }
        if (item == null) {
            throw new IllegalArgumentException("Error: Item cannot be null!");
        }

        this.auctionId = auctionId;
        this.item = item;
        this.status = AuctionStatus.OPEN;
        this.winnerId = null;
        this.finalPrice = item.getStartingPrice();
        this.createdAt = LocalDateTime.now();
        this.bidHistory = new ArrayList<>();
    }

    // Getters
    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public AuctionStatus getStatus() { return status; }
    public String getWinnerId() { return winnerId; }
    public double getFinalPrice() { return finalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public List<BidTransaction> getBidHistory() { return new ArrayList<>(bidHistory); }
    public int getBidCount() { return bidHistory.size(); }

    /**
     * Requirement 3.1.3: Kiểm tra tính hợp lệ của giá đấu
     * Kiểm tra xem phiên đấu giá có đang hoạt động không
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == AuctionStatus.RUNNING &&
                now.isAfter(item.getStartTime()) &&
                now.isBefore(item.getEndTime());
    }

    /**
     * Bắt đầu phiên đấu giá
     * Requirement 3.1.4: OPEN → RUNNING
     */
    public void startAuction() throws Exception {
        // [ERROR HANDLING] Kiểm tra trạng thái hiện tại
        if (status != AuctionStatus.OPEN) {
            throw new Exception("Error: Auction is not in OPEN status! Current: " + status);
        }

        // [ERROR HANDLING] Kiểm tra thời gian bắt đầu
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            throw new Exception("Error: Auction start time has not been reached! " +
                    "Scheduled: " + item.getStartTime());
        }

        this.status = AuctionStatus.RUNNING;
        this.startedAt = now;
        System.out.println("Auction " + auctionId + " started for item: " + item.getName());
    }

    /**
     * Requirement 3.1.3: Người dùng đặt giá cao hơn giá hiện tại
     * Requirement 3.1.3: Cập nhật người dẫn đầu phiên đấu giá
     * Requirement 3.1.5: Xử lý lỗi & ngoại lệ
     */
    public void addBid(BidTransaction bid) throws Exception {
        // [ERROR HANDLING] Kiểm tra bid không null
        if (bid == null) {
            throw new IllegalArgumentException("Error: Bid cannot be null!");
        }

        // [ERROR HANDLING] 3.1.5: Đấu giá khi phiên đã đóng
        if (!isActive()) {
            throw new Exception("Error: Auction is not active for bidding! Status: " + status);
        }

        // [ERROR HANDLING] 3.1.5: Đặt giá thấp hơn giá hiện tại
        if (bid.getBidAmount() <= item.getHighestCurrentPrice()) {
            throw new Exception("Error: Bid amount (" + bid.getBidAmount() +
                    ") must be greater than current price (" +
                    item.getHighestCurrentPrice() + ")!");
        }

        // Requirement 3.1.3: Cập nhật giá cao nhất và người dẫn đầu
        bidHistory.add(bid);
        item.setHighestCurrentPrice(bid.getBidAmount());

        System.out.println("New bid accepted: " + bid.getBidderId() +
                " | Amount: " + bid.getBidAmount());
    }

    /**
     * Requirement 3.1.4: Xác định người thắng cuộc
     * Kết thúc phiên đấu giá
     */
    public void endAuction() throws Exception {
        // [ERROR HANDLING] Kiểm tra trạng thái
        if (status != AuctionStatus.RUNNING) {
            throw new Exception("Error: Auction is not running! Current status: " + status);
        }

        // [ERROR HANDLING] 3.1.4: Tự động đóng phiên khi hết thời gian
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getEndTime())) {
            throw new Exception("Error: Auction end time has not been reached! " +
                    "Scheduled: " + item.getEndTime());
        }

        // Requirement 3.1.4: Xác định người thắng cuộc
        if (!bidHistory.isEmpty()) {
            // Người thắng = người có bid cao nhất
            BidTransaction winnerBid = bidHistory.stream()
                    .max(Comparator.comparingDouble(BidTransaction::getBidAmount))
                    .orElse(null);

            if (winnerBid != null) {
                this.winnerId = winnerBid.getBidderId();
                this.finalPrice = winnerBid.getBidAmount();
                System.out.println("Winner: " + winnerId + " | Final Price: " + finalPrice);
            }
        } else {
            this.winnerId = null;
            this.finalPrice = item.getStartingPrice();
            System.out.println("No bids received - No winner");
        }

        // Requirement 3.1.4: Chuyển trạng thái RUNNING → FINISHED
        this.status = AuctionStatus.FINISHED;
        this.endedAt = now;
        System.out.println("Auction " + auctionId + " has finished");
    }

    /**
     * Requirement 3.1.4: Chuyển trạng thái FINISHED → PAID
     */
    public void markAsPaid() throws Exception {
        if (status != AuctionStatus.FINISHED) {
            throw new Exception("Error: Auction must be FINISHED to mark as PAID! Current: " + status);
        }

        this.status = AuctionStatus.PAID;
        System.out.println("Auction " + auctionId + " marked as PAID");
    }

    /**
     * Hủy phiên đấu giá
     * Requirement 3.1.4: Chuyển trạng thái → CANCELED
     */
    public void cancelAuction(String reason) throws Exception {
        // [ERROR HANDLING] Kiểm tra lý do không trống
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: Cancellation reason cannot be empty!");
        }

        this.status = AuctionStatus.CANCELED;
        System.out.println("Auction " + auctionId + " canceled. Reason: " + reason);
    }

    /**
     * Requirement 3.1.3: Theo dõi diễn biến của phiên đấu giá
     */
    public BidTransaction getLatestBid() {
        if (bidHistory.isEmpty()) {
            return null;
        }
        return bidHistory.get(bidHistory.size() - 1);
    }

    public BidTransaction getHighestBid() {
        if (bidHistory.isEmpty()) {
            return null;
        }
        return bidHistory.stream()
                .max(Comparator.comparingDouble(BidTransaction::getBidAmount))
                .orElse(null);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + auctionId + '\'' +
                ", item='" + item.getName() + '\'' +
                ", status=" + status +
                ", bids=" + bidHistory.size() +
                ", currentPrice=" + item.getHighestCurrentPrice() +
                ", winner='" + winnerId + '\'' +
                '}';
    }
}