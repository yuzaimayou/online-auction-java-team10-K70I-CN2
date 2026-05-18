package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.BidPayload;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BidService {
    private static final Logger LOGGER = Logger.getLogger(BidService.class.getName());
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

                // [FIX] Kiểm tra xem phiên đấu giá đã hết thời gian chưa
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(item.getEndTime())) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: auction has ended at " + item.getEndTime());
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
                        LocalDateTime.now().toString()
                );

                if (!registered) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to register auto bid", e);
                return false;
            }
        }
    }

    public boolean placeBid(String itemId, String userId, double bidPrice) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: item not found or user is the seller for itemId " + itemId);
                    return false;
                }

                // [FIX] Kiểm tra xem phiên đấu giá đã hết thời gian chưa
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(item.getEndTime())) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: auction has ended at " + item.getEndTime() + ", current time: " + now);
                    return false;
                }
                if (now.isBefore(item.getStartTime())) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: auction has not started yet at " + item.getStartTime() + ", current time: " + now);
                    return false;
                }

                String lastBidder = bidRepository.findLastBidder(conn, itemId);
                if (lastBidder != null && lastBidder.equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: same user cannot bid consecutively");
                    return false;
                }

                double minAllowedPrice = item.getHighestCurrentPrice() + item.getBidStep();
                if (bidPrice + PRICE_EPSILON < minAllowedPrice) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: bid price " + bidPrice + " is less than minimum allowed " + minAllowedPrice);
                    return false;
                }

                String resolvedBidTime = now.toString();
                boolean created = bidRepository.createBid(conn, itemId, userId, bidPrice, resolvedBidTime);
                if (!created) {
                    conn.rollback();
                    LOGGER.warning("Bid rejected: failed to create bid record in database");
                    return false;
                }

                if (!itemRepository.updateCurrentPrice(conn, itemId, bidPrice)) {
                    conn.rollback();
                    LOGGER.warning("Bid rejected: failed to update current price in database");
                    return false;
                }

                boolean isExtended = false;
                LocalDateTime newEndTime = null;
                long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(now, item.getEndTime());
                if (secondsRemaining < 60 && secondsRemaining >= 0) {
                    newEndTime = now.plusSeconds(60);
                    if (!itemRepository.extendEndTime(conn, itemId, newEndTime)) {
                        conn.rollback();
                        LOGGER.warning("Bid rejected: failed to extend end time for anti-sniping");
                        return false;
                    }
                    isExtended = true;
                    LOGGER.info("Anti-sniping activated: end time extended to " + newEndTime);
                }

                runAutoBiddingRounds(conn, itemId, userId, bidPrice);
                conn.commit();
                try {
                    BidPayload newBidData = new BidPayload(itemId, userId, bidPrice, resolvedBidTime);

                    AuctionRoomManager.getInstance().broadcastToRoom(itemId, SocketEventConstants.EVENT_NEW_BID, newBidData);
                    LOGGER.fine("Broadcasted new bid for item " + itemId);

                    if (isExtended) {
                        com.auction.shared.model.payloads.AuctionExtendedPayload extendedPayload = 
                            new com.auction.shared.model.payloads.AuctionExtendedPayload(itemId, newEndTime.toString());
                        AuctionRoomManager.getInstance().broadcastToRoom(itemId, SocketEventConstants.EVENT_AUCTION_EXTENDED, extendedPayload);
                        LOGGER.fine("Broadcasted AUCTION_EXTENDED for item " + itemId);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to broadcast for item " + itemId, e);
                }
                return true;

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception occurred during placeBid", e);
                return false;
            }
        }
    }

    private void runAutoBiddingRounds(Connection conn, String itemId, String leadingUserId, double currentPrice) throws SQLException {
        String currentLeader = leadingUserId;
        double livePrice = currentPrice;

        for (int round = 0; round < AUTO_BID_MAX_ROUNDS; round++) {
            List<BidRepository.AutoBidConfig> autoBids = bidRepository.findActiveAutoBids(conn, itemId);
            if (autoBids.isEmpty()) {
                return;
            }

            AutoBidResolver.ResolvedAutoBid candidate = autoBidResolver.selectNextBid(autoBids, currentLeader, livePrice);
            if (candidate == null) {
                return;
            }

            String bidTime = LocalDateTime.now().toString();
            if (!bidRepository.createBid(conn, itemId, candidate.userId(), candidate.bidPrice(), bidTime)) {
                throw new SQLException("Failed to create auto bid in database");
            }
            if (!itemRepository.updateCurrentPrice(conn, itemId, candidate.bidPrice())) {
                throw new SQLException("Failed to update current price after auto bid in database");
            }

            livePrice = candidate.bidPrice();
            currentLeader = candidate.userId();
        }
    }

    private Object getItemLock(String itemId) {
        return itemLocks.computeIfAbsent(itemId, ignored -> new Object());
    }

}
