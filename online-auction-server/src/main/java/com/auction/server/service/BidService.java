package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;  // [SỬA #2] thêm import
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BidService {
    private static final double PRICE_EPSILON = 0.000001d;
    private static final int AUTO_BID_MAX_ROUNDS = 200;

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final AutoBidResolver autoBidResolver;
    private final ConcurrentMap<String, Object> itemLocks;

    public BidService() {
        this.bidRepository = new BidRepository();
        this.itemRepository = ItemRepository.getInstance();
        this.autoBidResolver = new AutoBidResolver(PRICE_EPSILON);
        this.itemLocks = new ConcurrentHashMap<>();
    }

    public boolean registerAutoBid(String itemId, String userId, double maxBid, double increment) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        if (maxBid <= 0 || increment <= 0) {
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    return false;
                }
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(item.getEndTime())) {
                    conn.rollback();
                    System.out.println("Auto-bid registration rejected: auction has ended at " + item.getEndTime());
                    return false;
                }

                double minimumPossible = item.getHighestCurrentPrice() + item.getBidStep();
                if (maxBid + PRICE_EPSILON < minimumPossible) {
                    conn.rollback();
                    return false;
                }

                boolean registered = bidRepository.upsertAutoBid(
                        conn,
                        itemId,
                        userId,
                        maxBid,
                        increment,
                        now.toString()
                );

                if (!registered) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public boolean placeBid(String itemId, String userId, double bidPrice, String bidTime) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    System.out.println("Bid rejected: item not found or user is the seller");
                    return false;
                }

                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(item.getEndTime())) {
                    conn.rollback();
                    System.out.println("Bid rejected: auction has ended at " + item.getEndTime() + ", current time: " + now);
                    return false;
                }

                String lastBidder = bidRepository.findLastBidder(conn, itemId);
                if (lastBidder != null && lastBidder.equals(userId)) {
                    conn.rollback();
                    System.out.println("Bid rejected: same user cannot bid consecutively");
                    return false;
                }

                double minAllowedPrice = item.getHighestCurrentPrice() + item.getBidStep();
                if (bidPrice + PRICE_EPSILON < minAllowedPrice) {
                    conn.rollback();
                    System.out.println("Bid rejected: bid price " + bidPrice + " is less than minimum allowed " + minAllowedPrice);
                    return false;
                }

                String resolvedBidTime = (bidTime == null || bidTime.isBlank())
                        ? now.toString()
                        : bidTime;

                boolean created = bidRepository.createBid(conn, itemId, userId, bidPrice, resolvedBidTime);
                if (!created) {
                    conn.rollback();
                    System.out.println("Bid rejected: failed to create bid record in database");
                    return false;
                }

                if (!itemRepository.updateCurrentPrice(conn, itemId, bidPrice)) {
                    conn.rollback();
                    System.out.println("Bid rejected: failed to update current price in database");
                    return false;
                }

                List<BidPayload> autoBidResults = runAutoBiddingRounds(conn, itemId, userId, bidPrice);

                conn.commit();

                try {
                    BidPayload newBidData = new BidPayload(itemId, userId, bidPrice, resolvedBidTime);
                    AuctionRoomManager.getInstance().broadcastToRoom(itemId, "NEW_BID", newBidData);
                    System.out.println("Broadcasted new bid for item " + itemId + ": " + newBidData);
                } catch (Exception e) {
                    System.err.println("Failed to broadcast new bid for item " + itemId + ": " + e.getMessage());
                }

                for (BidPayload autoBid : autoBidResults) {
                    try {
                        AuctionRoomManager.getInstance().broadcastToRoom(itemId, "NEW_BID", autoBid);
                        System.out.println("Broadcasted auto-bid for item " + itemId + ": " + autoBid);
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast auto-bid for item " + itemId + ": " + e.getMessage());
                    }
                }

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    private List<BidPayload> runAutoBiddingRounds(Connection conn, String itemId,
                                                  String leadingUserId, double currentPrice) throws Exception {
        List<BidPayload> results = new ArrayList<>();
        String currentLeader = leadingUserId;
        double livePrice = currentPrice;

        for (int round = 0; round < AUTO_BID_MAX_ROUNDS; round++) {
            List<BidRepository.AutoBidConfig> autoBids = bidRepository.findActiveAutoBids(conn, itemId);
            if (autoBids.isEmpty()) {
                break;
            }

            AutoBidResolver.ResolvedAutoBid candidate = autoBidResolver.selectNextBid(autoBids, currentLeader, livePrice);
            if (candidate == null) {
                break;
            }

            String bidTime = LocalDateTime.now().toString();
            if (!bidRepository.createBid(conn, itemId, candidate.userId(), candidate.bidPrice(), bidTime)) {
                throw new Exception("Failed to create auto bid");
            }
            if (!itemRepository.updateCurrentPrice(conn, itemId, candidate.bidPrice())) {
                throw new Exception("Failed to update current price after auto bid");
            }

            results.add(new BidPayload(itemId, candidate.userId(), candidate.bidPrice(), bidTime));

            livePrice = candidate.bidPrice();
            currentLeader = candidate.userId();
        }

        return results;
    }

    private Object getItemLock(String itemId) {
        return itemLocks.computeIfAbsent(itemId, ignored -> new Object());
    }
}