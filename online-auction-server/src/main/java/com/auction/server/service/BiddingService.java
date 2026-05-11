package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.account.User;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý chức năng đấu giá
 * Requirement 3.1.3: Tham gia đấu giá
 * Requirement 3.1.5: Xử lý lỗi & ngoại lệ
 */
public class BiddingService {
    private Map<String, Auction> auctionMap;

    public BiddingService() {
        this.auctionMap = new HashMap<>();
    }

    /**
     * Tạo một phiên đấu giá mới
     */
    public Auction createAuction(String auctionId, Item item) throws Exception {
        // [ERROR HANDLING] Kiểm tra auction không tồn tại
        if (auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction with ID " + auctionId + " already exists!");
        }

        Auction auction = new Auction(auctionId, item);
        auctionMap.put(auctionId, auction);
        System.out.println("Auction created: " + auctionId);
        return auction;
    }

    /**
     * Requirement 3.1.3: Người dùng đặt giá cao hơn giá hiện tại
     * Requirement 3.1.3: Kiểm tra tính hợp lệ của giá đấu
     * Requirement 3.1.5: Xử lý lỗi & ngoại lệ
     */
    public BidTransaction placeBid(String auctionId, User bidder, double bidAmount) throws Exception {
        // [ERROR HANDLING] 3.1.5: Lỗi dữ liệu - Kiểm tra auction tồn tại
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction " + auctionId + " not found!");
        }

        Auction auction = auctionMap.get(auctionId);

        // [ERROR HANDLING] 3.1.5: Kiểm tra bidder không null
        if (bidder == null) {
            throw new IllegalArgumentException("Error: Bidder cannot be null!");
        }

        // [ERROR HANDLING] 3.1.5: Đấu giá khi phiên đã đóng
        if (!auction.isActive()) {
            throw new Exception("Error: Auction is not active! Status: " + auction.getStatus());
        }

        // [ERROR HANDLING] 3.1.5: Đặt giá thấp hơn giá hiện tại
        if (bidAmount <= 0) {
            throw new Exception("Error: Bid amount must be positive!");
        }

        // [ERROR HANDLING] 3.1.3: Kiểm tra tính hợp lệ - bid phải cao hơn giá hiện tại
        if (bidAmount <= auction.getItem().getHighestCurrentPrice()) {
            throw new Exception("Error: Bid (" + bidAmount +
                    ") must be greater than current price (" +
                    auction.getItem().getHighestCurrentPrice() + ")!");
        }

        // [ERROR HANDLING] 3.1.5: Kiểm tra shill bidding (bid lên item của mình)
        if (auction.getItem().isOwner(bidder.getId())) {
            throw new Exception("Error: Cannot bid on your own item!");
        }

        // [ERROR HANDLING] 3.1.5: Kiểm tra balance đủ
        if (bidder.getBalance() < bidAmount) {
            throw new Exception("Error: Insufficient balance! Required: " + bidAmount +
                    ", Available: " + bidder.getBalance());
        }

        // Tạo transaction bid
        String transactionId = UUID.randomUUID().toString();
        BidTransaction bid = new BidTransaction(transactionId, auctionId, bidder.getId(),
                bidAmount, LocalDateTime.now());

        // Requirement 3.1.3: Thêm bid vào phiên
        auction.addBid(bid);

        System.out.println("Bid placed: " + bidder.getUsername() + " | Amount: " + bidAmount);
        return bid;
    }

    /**
     * Bắt đầu phiên đấu giá
     */
    public void startAuction(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }
        auctionMap.get(auctionId).startAuction();
    }

    /**
     * Requirement 3.1.4: Tự động đóng phiên khi hết thời gian
     * Requirement 3.1.4: Xác định người thắng cuộc
     */
    public void endAuction(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }
        auctionMap.get(auctionId).endAuction();
    }

    /**
     * Requirement 3.1.4: Chuyển trạng thái FINISHED → PAID
     */
    public void markAuctionAsPaid(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }
        auctionMap.get(auctionId).markAsPaid();
    }

    /**
     * Requirement 3.1.3: Theo dõi diễn biến của phiên đấu giá theo thời gian thực
     */
    public Auction getAuction(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }
        return auctionMap.get(auctionId);
    }

    /**
     * Lấy lịch sử bid (Requirement 3.1.3: Theo dõi diễn biến)
     */
    public List<BidTransaction> getBidHistory(String auctionId) throws Exception {
        if (!auctionMap.containsKey(auctionId)) {
            throw new Exception("Error: Auction not found!");
        }
        return auctionMap.get(auctionId).getBidHistory();
    }

    /**
     * Lấy tất cả auctions
     */
    public Collection<Auction> getAllAuctions() {
        return new ArrayList<>(auctionMap.values());
    }

    /**
     * Lấy tất cả auctions đang hoạt động
     */
    public List<Auction> getActiveAuctions() {
        List<Auction> active = new ArrayList<>();
        for (Auction auction : auctionMap.values()) {
            if (auction.isActive()) {
                active.add(auction);
            }
        }
        return active;
    }

    public void broadcastNewBid(String auctionId, BidTransaction bid) {
        System.out.println("Broadcasting new bid: " + bid.getBidAmount());
    }
}