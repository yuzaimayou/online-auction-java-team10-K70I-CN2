package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.shared.constant.ItemStatusConstants;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.ConnectionException;
import com.auction.shared.exception.DataException;
import com.auction.shared.exception.ErrorCode;
import com.auction.shared.exception.InvalidBidException;
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
    private final WalletRepository walletRepository = new WalletRepository();
    private final WalletTransactionRepository txLogRepository = new WalletTransactionRepository();
    private static final ConcurrentMap<String, Object> ITEM_LOCKS = new ConcurrentHashMap<>();

    private boolean handleWalletMovement(Connection conn, String itemId, String bidderId, double bidPrice, String prevBidderId, double prevBidPrice) throws SQLException {
        try {
            // 1. Check balance of new bidder
            double[] bidderBalances = walletRepository.getBalances(conn, bidderId);
            if (bidderBalances == null || bidderBalances.length == 0) {
                throw DataException.of(ErrorCode.DATA_INTEGRITY_ERROR,
                        "Không thể lấy thông tin ví của người dùng: " + bidderId);
            }

            double bidderBalance = bidderBalances[0];

            if (bidderBalance < bidPrice) {
                throw InvalidBidException.of(ErrorCode.INSUFFICIENT_BALANCE,
                        String.format("Số dư không đủ: có %.2f đ, cần %.2f đ cho người dùng %s",
                                bidderBalance, bidPrice, bidderId));
            }

            // 2. Freeze money for the new bidder
            boolean frozeOk = walletRepository.freezeAmount(conn, bidderId, bidPrice);
            if (!frozeOk) {
                throw DataException.of(ErrorCode.WALLET_OPERATION_FAILED,
                        "Không thể khóa số dư của người dùng: " + bidderId);
            }

            // 3. Log FREEZE
            try {
                txLogRepository.logFreeze(conn, bidderId, bidPrice, bidderBalance, bidderBalance - bidPrice, itemId);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Cảnh báo: Không thể ghi log khóa ví", e);
                // Không throw exception, chỉ log cảnh báo vì log không ảnh hưởng đến logic chính
            }

            // 4. Unfreeze money for previous bidder
            if (prevBidderId != null && !prevBidderId.isBlank() && !prevBidderId.equals(bidderId)) {
                boolean unfrozeOk = walletRepository.unfreezeAmount(conn, prevBidderId, prevBidPrice);
                if (!unfrozeOk) {
                    throw DataException.of(ErrorCode.WALLET_OPERATION_FAILED,
                            "Không thể mở khóa số dư của người dùng trước: " + prevBidderId);
                }

                // Log UNFREEZE
                try {
                    double[] prevBal = walletRepository.getBalances(conn, prevBidderId);
                    if (prevBal == null || prevBal.length == 0) {
                        throw DataException.of(ErrorCode.DATA_INTEGRITY_ERROR,
                                "Không thể lấy thông tin ví của người dùng trước: " + prevBidderId);
                    }
                    txLogRepository.logUnfreeze(conn, prevBidderId, prevBidPrice, prevBal[0] - prevBidPrice, prevBal[0], itemId);
                } catch (DataException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Cảnh báo: Không thể ghi log mở khóa ví", e);
                    // Không throw exception, chỉ log cảnh báo
                }
            }

            return true;
        } catch (InvalidBidException | DataException e) {
            // Re-throw các exception tùy chỉnh
            throw new SQLException(e.getMessage(), e);
        } catch (Exception e) {
            // Wrap các exception khác
            throw new SQLException("Lỗi xử lý ví: " + e.getMessage(), e);
        }
    }

    public BidService() {
        this.bidRepository = new BidRepository();
        this.itemRepository = ItemRepository.getInstance();
        this.autoBidResolver = new AutoBidResolver(PRICE_EPSILON);
    }

    public boolean registerAutoBid(String itemId, String userId, double maxBid, double increment) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            LOGGER.warning("Auto-bid registration rejected: Invalid input - itemId or userId is empty");
            return false;
        }
        if (maxBid <= 0 || increment <= 0) {
            LOGGER.warning("Auto-bid registration rejected: Invalid parameters - maxBid or increment <= 0");
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: Item not found - " + itemId);
                    return false;
                }

                if (item.getSellerId().equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: Seller cannot auto-bid - " + itemId);
                    return false;
                }

                // Kiểm tra thời gian phiên đấu giá
                try {
                    validateAuctionTime(item);
                } catch (AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: " + e.getMessage());
                    return false;
                }

                // Kiểm tra maxBid hợp lệ
                double minimumPossible = item.getHighestCurrentPrice() + item.getBidStep();
                if (maxBid + PRICE_EPSILON < minimumPossible) {
                    conn.rollback();
                    LOGGER.info(String.format("Auto-bid registration rejected: maxBid %.2f < minimum %.2f", maxBid, minimumPossible));
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
                    LOGGER.warning("Auto-bid registration rejected: Failed to persist auto-bid config");
                    return false;
                }

                conn.commit();
                LOGGER.info("Auto-bid registered successfully");
                return true;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database connection error during auto-bid registration", e);
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during auto-bid registration", e);
                return false;
            }
        }
    }

    public boolean registerAutoBidAndMaybeBid(String itemId, String userId, double maxBid, double increment) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            LOGGER.warning("Auto-bid registration rejected: Invalid input - itemId or userId is empty");
            return false;
        }
        if (maxBid <= 0 || increment <= 0) {
            LOGGER.warning("Auto-bid registration rejected: Invalid parameters - maxBid or increment <= 0");
            return false;
        }

        FinalBid finalBid = null;
        boolean shouldBroadcast = false;

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: Item not found - " + itemId);
                    return false;
                }

                if (item.getSellerId().equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: Seller cannot auto-bid - " + itemId);
                    return false;
                }

                if (increment + PRICE_EPSILON < item.getBidStep()) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: Increment lower than bid step");
                    return false;
                }

                if (!isBiddingStatusAllowed(item)) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: Invalid item status");
                    return false;
                }

                // Kiểm tra thời gian phiên đấu giá
                try {
                    validateAuctionTime(item);
                } catch (AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Auto-bid registration rejected: " + e.getMessage());
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
                    LOGGER.warning("Auto-bid registration rejected: Failed to persist auto-bid config");
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
                    return true;
                }

                if (immediateBidPrice + PRICE_EPSILON <= currentPrice || immediateBidPrice > maxBid + PRICE_EPSILON) {
                    conn.rollback();
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION] time=%s itemId=%s registeringUserId=%s dbCurrentBidderId=%s decision=REJECT_MAX_TOO_LOW",
                            LocalDateTime.now(), itemId, userId, currentBidderId));
                    return false;
                }

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION] time=%s itemId=%s registeringUserId=%s dbCurrentBidderId=%s decision=PLACE_IMMEDIATE_AUTO_BID",
                        LocalDateTime.now(), itemId, userId, currentBidderId));

                // Handle wallet movement before updating DB
                try {
                    if (!handleWalletMovement(conn, itemId, userId, immediateBidPrice, currentBidderId, currentPrice)) {
                        conn.rollback();
                        return false;
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Auto-bid registration rejected: Wallet operation failed - " + e.getMessage());
                    return false;
                }

                String bidTime = LocalDateTime.now().toString();
                if (!bidRepository.createBid(conn, itemId, userId, immediateBidPrice, bidTime)) {
                    conn.rollback();
                    LOGGER.warning("Auto-bid registration rejected: Failed to create immediate bid record");
                    return false;
                }
                if (!itemRepository.updateCurrentBidder(conn, itemId, immediateBidPrice, userId)) {
                    conn.rollback();
                    LOGGER.warning("Auto-bid registration rejected: Failed to update current bidder");
                    return false;
                }
                LOGGER.info(String.format("[AUTO_BID_REGISTER][IMMEDIATE_BID] time=%s itemId=%s userId=%s currentPrice=%.2f bidStep=%.2f increment=%.2f bidPrice=%.2f maxBid=%.2f",
                        LocalDateTime.now(), itemId, userId, currentPrice, item.getBidStep(), increment, immediateBidPrice, maxBid));

                finalBid = runAutoBiddingRounds(conn, itemId, userId, immediateBidPrice, bidTime);
                conn.commit();
                shouldBroadcast = true;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database connection error during auto-bid registration", e);
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during auto-bid registration", e);
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
            LOGGER.warning("Bid rejected: Invalid input - itemId or userId is empty");
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Item not found - " + itemId);
                    return false;
                }

                if (item.getSellerId().equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Seller cannot bid - " + itemId);
                    return false;
                }

                if (!isBiddingStatusAllowed(item)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Invalid item status - " + itemId);
                    return false;
                }

                // Kiểm tra thời gian phiên đấu giá
                try {
                    validateAuctionTime(item);
                } catch (AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: " + e.getMessage());
                    return false;
                }

                String lastBidder = bidRepository.findLastBidder(conn, itemId);
                if (lastBidder != null && lastBidder.equals(userId)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Same user cannot bid consecutively - " + itemId);
                    return false;
                }

                double minAllowedPrice = item.getHighestCurrentPrice() + item.getBidStep();

                // Kiểm tra giá bid hợp lệ
                try {
                    validateBidPrice(bidPrice, minAllowedPrice);
                } catch (InvalidBidException e) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: " + e.getMessage());
                    return false;
                }

                // Handle wallet movement before updating DB
                try {
                    if (!handleWalletMovement(conn, itemId, userId, bidPrice, item.getCurrentTopPLayerId(), item.getHighestCurrentPrice())) {
                        conn.rollback();
                        return false;
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Bid rejected: Wallet operation failed - " + e.getMessage());
                    return false;
                }

                String resolvedBidTime = LocalDateTime.now().toString();
                LOGGER.info(String.format("[AUTO_BID_ROUND][MANUAL_BID] time=%s itemId=%s userId=%s itemCurrentPrice=%.2f bidStep=%.2f bidPrice=%.2f minAllowed=%.2f",
                        LocalDateTime.now(), itemId, userId, item.getHighestCurrentPrice(), item.getBidStep(), bidPrice, minAllowedPrice));

                boolean created = bidRepository.createBid(conn, itemId, userId, bidPrice, resolvedBidTime);
                if (!created) {
                    conn.rollback();
                    LOGGER.warning("Bid rejected: Failed to create bid record in database");
                    return false;
                }

                if (!itemRepository.updateCurrentBidder(conn, itemId, bidPrice, userId)) {
                    conn.rollback();
                    LOGGER.warning("Bid rejected: Failed to update current bidder in database");
                    return false;
                }

                boolean isExtended = false;
                LocalDateTime newEndTime = null;
                long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getEndTime());
                if (secondsRemaining < 60 && secondsRemaining >= 0) {
                    newEndTime = LocalDateTime.now().plusSeconds(60);
                    if (!itemRepository.extendEndTime(conn, itemId, newEndTime)) {
                        conn.rollback();
                        LOGGER.warning("Bid rejected: Failed to extend end time for anti-sniping");
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

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database connection error during placeBid", e);
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during placeBid", e);
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

            // 1. Check if candidate can afford it
            try {
                double[] candidateBalances = walletRepository.getBalances(conn, candidate.userId());
                if (candidateBalances[0] < candidate.bidPrice()) {
                    LOGGER.warning(String.format("[AUTO_BID_ROUND][SKIP] Insufficient funds for auto-bid user %s. Needs %.2f, has %.2f",
                            candidate.userId(), candidate.bidPrice(), candidateBalances[0]));
                    bidRepository.deactivateAutoBidIfPresent(conn, itemId, candidate.userId());
                    continue; // Skip this candidate, try others in the list
                }
            } catch (Exception e) {
                throw new SQLException("Failed to verify auto-bid candidate balance: " + e.getMessage(), e);
            }

            // 2. Perform wallet movement
            try {
                if (!handleWalletMovement(conn, itemId, candidate.userId(), candidate.bidPrice(), currentLeader, livePrice)) {
                    throw new SQLException("Failed to handle wallet movement for auto-bid candidate: " + candidate.userId());
                }
            } catch (Exception e) {
                throw new SQLException("Failed to execute wallet movement for auto-bid: " + e.getMessage(), e);
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

    /**
     * Kiểm tra xem phiên đấu giá có trong thời gian hợp lệ không
     * @param item Item cần kiểm tra
     * @throws AuctionClosedException nếu phiên đã kết thúc hoặc chưa bắt đầu
     */
    private void validateAuctionTime(Item item) throws AuctionClosedException {
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(item.getEndTime())) {
            throw AuctionClosedException.of(ErrorCode.AUCTION_ALREADY_CLOSED,
                    String.format("Phiên đấu giá đã kết thúc lúc %s", item.getEndTime()));
        }

        if (now.isBefore(item.getStartTime())) {
            throw AuctionClosedException.of(ErrorCode.AUCTION_NOT_STARTED,
                    String.format("Phiên đấu giá chưa bắt đầu, sẽ bắt đầu lúc %s", item.getStartTime()));
        }

    }

    /**
     * Kiểm tra xem giá bid có hợp lệ không
     * @param bidPrice Giá bid được đề xuất
     * @param minAllowedPrice Giá tối thiểu cho phép
     * @throws InvalidBidException nếu giá bid không hợp lệ
     */
    private void validateBidPrice(double bidPrice, double minAllowedPrice) throws InvalidBidException {
        if (bidPrice + PRICE_EPSILON < minAllowedPrice) {
            throw InvalidBidException.of(ErrorCode.BID_PRICE_TOO_LOW,
                    String.format("Giá bid %.2f thấp hơn giá tối thiểu %.2f", bidPrice, minAllowedPrice));
        }
    }

    public static Object getItemLock(String itemId) {
        return ITEM_LOCKS.computeIfAbsent(itemId, ignored -> new Object());
    }

}
