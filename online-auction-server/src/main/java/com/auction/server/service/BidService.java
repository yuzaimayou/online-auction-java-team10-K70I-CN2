package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.constant.ItemStatusConstants;
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
    private static final ConcurrentMap<String, Object> ITEM_LOCKS = new ConcurrentHashMap<>();

    public BidService() {
        this.bidRepository = new BidRepository();
        this.itemRepository = ItemRepository.getInstance();
        this.autoBidResolver = new AutoBidResolver(PRICE_EPSILON);
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

    public boolean registerAutoBidAndMaybeBid(String itemId, String userId, double maxBid, double increment) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        if (maxBid <= 0 || increment <= 0) {
            return false;
        }

        FinalBid finalBid = null;
        boolean shouldBroadcast = false;

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: item not found or user is the seller for itemId " + itemId);
                    return false;
                }
                if (!isBiddingStatusAllowed(item)) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: item status does not allow bidding for itemId " + itemId
                            + ", storedStatus=" + item.getStoredStatus()
                            + ", computedStatus=" + item.getStatus());
                    return false;
                }

                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(item.getEndTime())) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: auction has ended at " + item.getEndTime()
                            + ", current time: " + now);
                    return false;
                }
                if (now.isBefore(item.getStartTime())) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: auction has not started yet at " + item.getStartTime()
                            + ", current time: " + now);
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
                    LOGGER.warning("Auto-bid registration rejected: failed to persist auto-bid config");
                    return false;
                }

                String currentBidderId = itemRepository.getCurrentBidderId(conn, itemId);
                double currentPrice = item.getHighestCurrentPrice();
                double immediateBidPrice = computeImmediateAutoBidPrice(currentPrice, item.getBidStep(), increment);
                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION_INPUT] time=%s itemId=%s registeringUserId=%s dbCurrentBidderId=%s currentPrice=%.2f nextLegalBid=%.2f maxBid=%.2f bidStep=%.2f increment=%.2f",
                        LocalDateTime.now(), itemId, userId, currentBidderId, currentPrice, immediateBidPrice, maxBid, item.getBidStep(), increment));
                if (userId.equals(currentBidderId)) {
                    conn.commit();
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION] time=%s itemId=%s registeringUserId=%s dbCurrentBidderId=%s decision=REGISTER_ONLY",
                            LocalDateTime.now(), itemId, userId, currentBidderId));
                    LOGGER.info("Auto-bid registered without immediate bid because user is already current bidder for itemId " + itemId);
                    return true;
                }

                if (immediateBidPrice + PRICE_EPSILON <= currentPrice || immediateBidPrice > maxBid + PRICE_EPSILON) {
                    conn.rollback();
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION] time=%s itemId=%s registeringUserId=%s dbCurrentBidderId=%s decision=REJECT_MAX_TOO_LOW",
                            LocalDateTime.now(), itemId, userId, currentBidderId));
                    LOGGER.info(String.format(
                            "Auto-bid registration rejected: maxBid %.2f cannot beat current price %.2f with bidStep %.2f and increment %.2f",
                            maxBid, currentPrice, item.getBidStep(), increment));
                    return false;
                }
                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION] time=%s itemId=%s registeringUserId=%s dbCurrentBidderId=%s decision=PLACE_IMMEDIATE_AUTO_BID",
                        LocalDateTime.now(), itemId, userId, currentBidderId));

                String bidTime = LocalDateTime.now().toString();
                if (!bidRepository.createBid(conn, itemId, userId, immediateBidPrice, bidTime)) {
                    conn.rollback();
                    LOGGER.warning("Auto-bid registration rejected: failed to create immediate auto-bid record");
                    return false;
                }
                if (!itemRepository.updateCurrentBidder(conn, itemId, immediateBidPrice, userId)) {
                    conn.rollback();
                    LOGGER.warning("Auto-bid registration rejected: failed to update immediate auto-bid current bidder");
                    return false;
                }
                LOGGER.info(String.format("[AUTO_BID_REGISTER][IMMEDIATE_BID] time=%s itemId=%s userId=%s currentPrice=%.2f bidStep=%.2f increment=%.2f bidPrice=%.2f maxBid=%.2f",
                        LocalDateTime.now(), itemId, userId, currentPrice, item.getBidStep(), increment, immediateBidPrice, maxBid));

                finalBid = runAutoBiddingRounds(conn, itemId, userId, immediateBidPrice, bidTime);
                conn.commit();
                shouldBroadcast = true;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to register auto bid and place immediate bid", e);
                return false;
            }
        }

        if (shouldBroadcast && finalBid != null) {
            broadcastNewBid(itemId, finalBid);
        }
        return true;
    }

    public com.auction.shared.model.payloads.AutoBidPayload getAutoBidStatus(String itemId, String userId) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return null;
        }

        com.auction.shared.model.payloads.AutoBidPayload activeConfig =
                bidRepository.findActiveAutoBid(itemId, userId);
        if (activeConfig != null) {
            return activeConfig;
        }

        return new com.auction.shared.model.payloads.AutoBidPayload(itemId, userId, null, null, false);
    }

    public boolean cancelAutoBid(String itemId, String userId) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(bidRepository.deactivateAutoBidIfPresent(itemId, userId));
    }

    public boolean placeBid(String itemId, String userId, double bidPrice) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: item not found or user is the seller for itemId " + itemId);
                    return false;
                }
                if (!isBiddingStatusAllowed(item)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: item status does not allow bidding for itemId " + itemId
                            + ", storedStatus=" + item.getStoredStatus()
                            + ", computedStatus=" + item.getStatus());
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
                LOGGER.info(String.format("[AUTO_BID_ROUND][MANUAL_BID] time=%s itemId=%s userId=%s itemCurrentPrice=%.2f bidStep=%.2f bidPrice=%.2f minAllowed=%.2f",
                        LocalDateTime.now(), itemId, userId, item.getHighestCurrentPrice(), item.getBidStep(), bidPrice, minAllowedPrice));
                boolean created = bidRepository.createBid(conn, itemId, userId, bidPrice, resolvedBidTime);
                if (!created) {
                    conn.rollback();
                    LOGGER.warning("Bid rejected: failed to create bid record in database");
                    return false;
                }

                if (!itemRepository.updateCurrentBidder(conn, itemId, bidPrice, userId)) {
                    conn.rollback();
                    LOGGER.warning("Bid rejected: failed to update current price and bidder in database");
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

                FinalBid finalBid = runAutoBiddingRounds(conn, itemId, userId, bidPrice, resolvedBidTime);
                conn.commit();
                try {
                    broadcastNewBid(itemId, finalBid);
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

    private FinalBid runAutoBiddingRounds(Connection conn,
                                          String itemId,
                                          String leadingUserId,
                                          double currentPrice,
                                          String currentBidTime) throws SQLException {
        String currentLeader = leadingUserId;
        double livePrice = currentPrice;
        String liveBidTime = currentBidTime;

        for (int round = 0; round < AUTO_BID_MAX_ROUNDS; round++) {
            LOGGER.info(String.format("[AUTO_BID_ROUND][START] time=%s itemId=%s round=%d currentLeader=%s currentPrice=%.2f",
                    LocalDateTime.now(), itemId, round, currentLeader, livePrice));
            List<BidRepository.AutoBidConfig> autoBids = bidRepository.findActiveAutoBids(conn, itemId);
            if (autoBids.isEmpty()) {
                LOGGER.info(String.format("[AUTO_BID_ROUND][STOP] time=%s itemId=%s reason=no_active_configs currentLeader=%s currentPrice=%.2f",
                        LocalDateTime.now(), itemId, currentLeader, livePrice));
                return new FinalBid(currentLeader, livePrice, liveBidTime);
            }

            AutoBidResolver.ResolvedAutoBid candidate = autoBidResolver.selectNextBid(autoBids, currentLeader, livePrice);
            if (candidate == null) {
                LOGGER.info(String.format("[AUTO_BID_ROUND][STOP] time=%s itemId=%s reason=no_valid_candidate currentLeader=%s currentPrice=%.2f",
                        LocalDateTime.now(), itemId, currentLeader, livePrice));
                return new FinalBid(currentLeader, livePrice, liveBidTime);
            }

            String bidTime = LocalDateTime.now().toString();
            if (!bidRepository.createBid(conn, itemId, candidate.userId(), candidate.bidPrice(), bidTime)) {
                throw new SQLException("Failed to create auto bid in database");
            }
            if (!itemRepository.updateCurrentBidder(conn, itemId, candidate.bidPrice(), candidate.userId())) {
                throw new SQLException("Failed to update current price and bidder after auto bid in database");
            }
            LOGGER.info(String.format("[AUTO_BID_ROUND][ACCEPT] time=%s itemId=%s userId=%s bidPrice=%.2f",
                    LocalDateTime.now(), itemId, candidate.userId(), candidate.bidPrice()));

            livePrice = candidate.bidPrice();
            currentLeader = candidate.userId();
            liveBidTime = bidTime;
        }

        return new FinalBid(currentLeader, livePrice, liveBidTime);
    }

    private record FinalBid(String userId, double bidPrice, String bidTime) {
    }

    private double computeImmediateAutoBidPrice(double currentPrice, double bidStep, double increment) {
        double requiredIncrement = Math.max(bidStep, increment);
        return currentPrice + requiredIncrement;
    }

    private void broadcastNewBid(String itemId, FinalBid finalBid) {
        BidPayload newBidData = new BidPayload(
                itemId,
                finalBid.userId(),
                finalBid.bidPrice(),
                finalBid.bidTime()
        );

        AuctionRoomManager.getInstance().broadcastToRoom(itemId, SocketEventConstants.EVENT_NEW_BID, newBidData);
        LOGGER.fine("Broadcasted new bid for item " + itemId);
    }

    private boolean isBiddingStatusAllowed(Item item) {
        String storedStatus = normalizeStatus(item.getStoredStatus());
        if (ItemStatusConstants.BANNED.equals(storedStatus) || ItemStatusConstants.ENDED.equals(storedStatus)) {
            return false;
        }

        String computedStatus = normalizeStatus(item.getStatus());
        return ItemStatusConstants.ONGOING.equals(computedStatus);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private Object getItemLock(String itemId) {
        return ITEM_LOCKS.computeIfAbsent(itemId, ignored -> new Object());
    }

}
