package com.auction.server.service.bid;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.*;
import com.auction.server.service.auction.AuctionLockManager;
import com.auction.server.socket.room.AuctionRoomManager;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.DataException;
import com.auction.shared.exception.ErrorCode;
import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.AuctionExtendedPayload;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BidService — xử lý toàn bộ nghiệp vụ đặt giá (manual bid và auto-bid).
 * <p>
 * Nguyên tắc thiết kế:
 * 1. Mỗi thao tác ghi DB chạy trong một transaction duy nhất
 * (conn.setAutoCommit(false)).
 * 2. Mọi nhánh thất bại đều rollback TRƯỚC khi return false.
 * 3. Validate nghiệp vụ (item, user, giá, thời gian) được tập trung vào
 * BidValidator — BidService chỉ điều phối, không tự validate.
 * 4. UserRepository được inject (không new() inline) để tái sử dụng connection
 * pool.
 * 5. Anti-sniping áp dụng nhất quán cho cả manual bid lẫn auto-bid.
 * 6. broadcastNewBid() luôn được gọi SAU khi commit, ngoài synchronized block.
 */
public class BidService {

    private static final Logger LOGGER = Logger.getLogger(BidService.class.getName());
    private static final double PRICE_EPSILON = 0.000001d;
    private static final long ANTI_SNIPE_SECONDS = 60L;

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AutoBidResolver autoBidResolver;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txLogRepository;
    private final BidValidator bidValidator;

    public BidService() {
        this.bidRepository = new BidRepository();
        this.itemRepository = ItemRepository.getInstance();
        this.userRepository = new UserRepository();
        this.autoBidResolver = new AutoBidResolver(PRICE_EPSILON);
        this.walletRepository = new WalletRepository();
        this.txLogRepository = new WalletTransactionRepository();
        this.bidValidator = new BidValidator(PRICE_EPSILON);
    }

    // Public API

    /**
     * Đặt bid thủ công.
     * <p>
     * Flow:
     * 1. Validate đầu vào (item, user, status, thời gian, giá).
     * 2. Xử lý ví (freeze mới / unfreeze cũ) trong transaction.
     * 3. Ghi bid + cập nhật current bidder.
     * 4. Anti-snipe nếu còn < 60s.
     * 5. Chạy auto-bid rounds cho các user đã đăng ký.
     * 6. Commit → broadcast.
     */
    public boolean placeBid(String itemId, String userId, double bidPrice) {
        if (isBlank(itemId) || isBlank(userId)) {
            LOGGER.warning("Bid rejected: itemId hoặc userId rỗng");
            return false;
        }

        List<BidEvent> bidEvents = new ArrayList<>();
        boolean isExtended = false;
        LocalDateTime newEndTime = null;

        synchronized (AuctionLockManager.getItemLock(itemId)) {
            Connection conn = null;

            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);
                Item item = itemRepository.findById(conn, itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Item không tồn tại - " + itemId);
                    return false;
                }

                if (userId.equals(item.getSellerId())) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Seller không được bid item của chính mình - " + itemId);
                    return false;
                }

                User user = userRepository.findById(conn, userId);

                try {
                    bidValidator.validateForManualBid(item, user, userId, bidPrice);
                } catch (InvalidBidException | AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: " + e.getMessage());
                    return false;
                }

                String lastBidder = bidRepository.findLastBidder(conn, itemId);
                if (userId.equals(lastBidder)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Cùng user không được bid liên tiếp - " + itemId);
                    return false;
                }

                String currentBidderId = itemRepository.getCurrentBidderId(conn, itemId);
                double currentPrice = item.getHighestCurrentPrice();

                handleWalletMovement(conn, itemId, userId, bidPrice, currentBidderId, currentPrice);

                String bidTime = LocalDateTime.now().toString();
                LOGGER.info(String.format(
                        "[MANUAL_BID] time=%s itemId=%s userId=%s currentPrice=%.2f bidPrice=%.2f",
                        bidTime, itemId, userId, currentPrice, bidPrice));

                if (!bidRepository.createBid(conn, itemId, userId, bidPrice, bidTime)) {
                    throw new SQLException("Failed to create bid for item: " + itemId);
                }
                if (!itemRepository.updateCurrentBidder(conn, itemId, bidPrice, userId)) {
                    throw new SQLException("Failed to update current bidder for item: " + itemId);
                }

                newEndTime = antiSnipe(item.getEndTime(), conn, itemId);
                isExtended = newEndTime != null;

                bidEvents.add(new BidEvent(userId, bidPrice, bidTime));
                bidEvents.addAll(runAutoBiddingRounds(conn, itemId, userId, bidPrice, bidTime));

                conn.commit();

            } catch (SQLException e) {
                rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "DB error trong placeBid - " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "Unexpected error trong placeBid - " + e.getMessage(), e);
                return false;
            } finally {
                closeQuietly(conn);
            }
        }

        broadcastAllBidEvents(itemId, bidEvents, isExtended, newEndTime);
        return true;
    }

    /**
     * Đăng ký auto-bid VÀ đặt bid ngay nếu điều kiện cho phép.
     * <p>
     * Flow:
     * 1. Validate đầu vào.
     * 2. Validate nghiệp vụ (item, user, status, thời gian, increment).
     * 3. Upsert auto-bid config.
     * 4. Quyết định: REGISTER_ONLY nếu đang dẫn đầu, PLACE_BID nếu chưa.
     * 5. Nếu PLACE_BID: xử lý ví → ghi bid → anti-snipe → auto-bid rounds.
     * 6. Commit → broadcast.
     */
    public boolean registerAutoBidAndMaybeBid(String itemId, String userId, double maxBid, double increment) {
        if (isBlank(itemId) || isBlank(userId)) {
            LOGGER.warning("Auto-bid rejected: itemId hoặc userId rỗng");
            return false;
        }
        if (maxBid <= 0 || increment <= 0) {
            LOGGER.warning("Auto-bid rejected: maxBid hoặc increment <= 0");
            return false;
        }

        List<BidEvent> bidEvents = new ArrayList<>();
        boolean shouldBroadcast = false;
        boolean isExtended = false;
        LocalDateTime newEndTime = null;

        synchronized (AuctionLockManager.getItemLock(itemId)) {
            Connection conn = null;

            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                // 1. Load dữ liệu
                Item item = itemRepository.findById(conn, itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Auto-bid rejected: Item không tồn tại - " + itemId);
                    return false;
                }

                User user = userRepository.findById(conn, userId);

                try {
                    bidValidator.validateForAutoBid(item, user, userId, maxBid, increment);
                } catch (InvalidBidException | AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Auto-bid rejected: " + e.getMessage());
                    return false;
                }

                bidRepository.upsertAutoBid(conn, itemId, userId, maxBid, increment,
                        LocalDateTime.now().toString());

                String currentBidderId = itemRepository.getCurrentBidderId(conn, itemId);
                double currentPrice = item.getHighestCurrentPrice();
                double immediateBidPrice = computeImmediateAutoBidPrice(
                        currentPrice, item.getBidStep(), increment);

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION_INPUT] time=%s itemId=%s userId=%s " +
                                "currentBidder=%s currentPrice=%.2f nextBid=%.2f maxBid=%.2f",
                        LocalDateTime.now(), itemId, userId,
                        currentBidderId, currentPrice, immediateBidPrice, maxBid));

                if (userId.equals(currentBidderId)) {
                    conn.commit();
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION=REGISTER_ONLY] itemId=%s userId=%s",
                            itemId, userId));
                    return true;
                }

                if (immediateBidPrice + PRICE_EPSILON <= currentPrice
                        || immediateBidPrice > maxBid + PRICE_EPSILON) {
                    conn.rollback();
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION=REJECT_MAX_TOO_LOW] itemId=%s userId=%s " +
                                    "immediateBid=%.2f maxBid=%.2f",
                            itemId, userId, immediateBidPrice, maxBid));
                    return false;
                }

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION=PLACE_IMMEDIATE_BID] itemId=%s userId=%s bidPrice=%.2f",
                        itemId, userId, immediateBidPrice));

                handleWalletMovement(conn, itemId, userId, immediateBidPrice,
                        currentBidderId, currentPrice);

                String bidTime = LocalDateTime.now().toString();
                if (!bidRepository.createBid(conn, itemId, userId, immediateBidPrice, bidTime)) {
                    throw new SQLException("Failed to create immediate auto-bid for item: " + itemId);
                }
                if (!itemRepository.updateCurrentBidder(conn, itemId, immediateBidPrice, userId)) {
                    throw new SQLException("Failed to update current bidder for item: " + itemId);
                }

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][IMMEDIATE_BID] time=%s itemId=%s userId=%s bidPrice=%.2f",
                        bidTime, itemId, userId, immediateBidPrice));

                newEndTime = antiSnipe(item.getEndTime(), conn, itemId);
                isExtended = newEndTime != null;

                bidEvents.add(new BidEvent(userId, immediateBidPrice, bidTime));
                bidEvents.addAll(runAutoBiddingRounds(conn, itemId, userId, immediateBidPrice, bidTime));

                conn.commit();
                shouldBroadcast = true;

            } catch (SQLException e) {
                rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "DB error trong registerAutoBidAndMaybeBid - " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "Unexpected error trong registerAutoBidAndMaybeBid - " + e.getMessage(), e);
                return false;
            } finally {
                closeQuietly(conn);
            }
        }

        if (shouldBroadcast) {
            broadcastAllBidEvents(itemId, bidEvents, isExtended, newEndTime);
        }
        return true;
    }

    /**
     * Lấy trạng thái auto-bid hiện tại của user cho một item.
     */
    public AutoBidPayload getAutoBidStatus(String itemId, String userId) {
        if (isBlank(itemId) || isBlank(userId)) {
            return null;
        }

        AutoBidPayload config = bidRepository.findActiveAutoBid(itemId, userId);
        return config != null ? config : new AutoBidPayload(itemId, userId, null, null, false);
    }

    /**
     * Hủy auto-bid.
     */
    public boolean cancelAutoBid(String itemId, String userId) {
        if (isBlank(itemId) || isBlank(userId)) {
            return false;
        }

        synchronized (AuctionLockManager.getItemLock(itemId)) {
            Connection conn = null;
            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);
                boolean deactivated = bidRepository.deactivateAutoBidIfPresent(conn, itemId, userId);
                conn.commit();
                return deactivated;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "DB error trong cancelAutoBid - " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "Unexpected error trong cancelAutoBid - " + e.getMessage(), e);
                return false;
            } finally {
                closeQuietly(conn);
            }
        }
    }

    // Private — wallet

    /**
     * Freeze tiền người bid mới, unfreeze tiền người bid cũ.
     * <p>
     * Method ném Exception ra ngoài để caller rollback đúng transaction.
     * Không tự rollback ở đây vì không sở hữu transaction.
     */
    private void handleWalletMovement(Connection conn, String itemId,
            String bidderId, double bidPrice,
            String prevBidderId, double prevBidPrice) throws Exception {

        double[] balances = walletRepository.getBalances(conn, bidderId);
        if (balances == null || balances.length == 0) {
            throw DataException.of(ErrorCode.DATA_INTEGRITY_ERROR,
                    "Không lấy được thông tin ví: " + bidderId);
        }

        double balance = balances[0];
        if (balance < bidPrice) {
            throw InvalidBidException.of(ErrorCode.INSUFFICIENT_BALANCE,
                    String.format("Số dư không đủ: có %.2f, cần %.2f (user=%s)",
                            balance, bidPrice, bidderId));
        }

        if (!walletRepository.freezeAmount(conn, bidderId, bidPrice)) {
            throw DataException.of(ErrorCode.WALLET_OPERATION_FAILED,
                    "Không thể khóa số dư: " + bidderId);
        }

        try {
            txLogRepository.logFreeze(conn, bidderId, bidPrice, balance, balance - bidPrice, itemId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không ghi được log FREEZE", e);
        }

        if (prevBidderId != null && !prevBidderId.isBlank() && !prevBidderId.equals(bidderId)) {
            if (!walletRepository.unfreezeAmount(conn, prevBidderId, prevBidPrice)) {
                throw DataException.of(ErrorCode.WALLET_OPERATION_FAILED,
                        "Không thể mở khóa số dư người cũ: " + prevBidderId);
            }

            try {
                double[] prevBal = walletRepository.getBalances(conn, prevBidderId);
                if (prevBal != null && prevBal.length > 0) {
                    txLogRepository.logUnfreeze(conn, prevBidderId, prevBidPrice,
                            prevBal[0] - prevBidPrice, prevBal[0], itemId);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Không ghi được log UNFREEZE", e);
            }
        }
    }

    // Private — auto-bid rounds

    /**
     * Chạy vòng lặp đối kháng giữa các auto-bid config.
     * Mỗi vòng: chọn candidate → kiểm tra ví → xử lý ví → ghi bid.
     * Dừng khi không còn candidate hợp lệ (selectNextBid trả về null).
     * <p>
     * Không có giới hạn số vòng cứng — vòng lặp kết thúc khi thị trường tự hội tụ.
     * Ném Exception ra ngoài để caller rollback toàn bộ transaction.
     */
    private List<BidEvent> runAutoBiddingRounds(Connection conn, String itemId,
            String leadingUserId, double currentPrice,
            String currentBidTime) throws Exception {
        String currentLeader = leadingUserId;
        double livePrice = currentPrice;
        int round = 0;
        List<BidEvent> events = new ArrayList<>();

        while (true) {
            LOGGER.info(String.format(
                    "[AUTO_BID_ROUND][START] time=%s itemId=%s round=%d leader=%s price=%.2f",
                    LocalDateTime.now(), itemId, round, currentLeader, livePrice));

            List<BidRepository.AutoBidConfig> autoBids = bidRepository.findActiveAutoBids(conn, itemId);
            if (autoBids.isEmpty()) {
                LOGGER.info(String.format(
                        "[AUTO_BID_ROUND][STOP] reason=no_active_configs leader=%s price=%.2f",
                        currentLeader, livePrice));
                break;
            }

            AutoBidResolver.ResolvedAutoBid candidate = autoBidResolver.selectNextBid(autoBids, currentLeader,
                    livePrice);
            if (candidate == null) {
                LOGGER.info(String.format(
                        "[AUTO_BID_ROUND][STOP] reason=no_valid_candidate leader=%s price=%.2f",
                        currentLeader, livePrice));
                break;
            }

            if (candidate.bidPrice() <= livePrice + PRICE_EPSILON) {
                LOGGER.warning(String.format(
                        "[AUTO_BID_ROUND][STOP] reason=candidate_price_not_above_live " +
                                "candidate=%s candidateBid=%.2f livePrice=%.2f — market converged.",
                        candidate.userId(), candidate.bidPrice(), livePrice));
                break;
            }

            double[] candidateBalances = walletRepository.getBalances(conn, candidate.userId());
            if (candidateBalances == null || candidateBalances.length == 0
                    || candidateBalances[0] < candidate.bidPrice()) {
                LOGGER.warning(String.format(
                        "[AUTO_BID_ROUND][SKIP] Không đủ tiền: userId=%s needs=%.2f has=%.2f",
                        candidate.userId(),
                        candidate.bidPrice(),
                        candidateBalances != null && candidateBalances.length > 0 ? candidateBalances[0] : 0d));
                bidRepository.deactivateAutoBidIfPresent(conn, itemId, candidate.userId());
                round++;
                continue;
            }

            handleWalletMovement(conn, itemId,
                    candidate.userId(), candidate.bidPrice(),
                    currentLeader, livePrice);

            String bidTime = LocalDateTime.now().toString();
            if (!bidRepository.createBid(conn, itemId, candidate.userId(), candidate.bidPrice(), bidTime)) {
                throw new SQLException("Failed to create auto-bid round bid for item: " + itemId);
            }
            if (!itemRepository.updateCurrentBidder(conn, itemId, candidate.bidPrice(), candidate.userId())) {
                throw new SQLException("Failed to update current bidder for item: " + itemId);
            }

            LOGGER.info(String.format(
                    "[AUTO_BID_ROUND][ACCEPT] time=%s itemId=%s userId=%s bidPrice=%.2f",
                    bidTime, itemId, candidate.userId(), candidate.bidPrice()));

            livePrice = candidate.bidPrice();
            currentLeader = candidate.userId();
            events.add(new BidEvent(currentLeader, livePrice, bidTime));
            round++;
        }

        return events;
    }

    // Private — broadcast

    private void broadcastAllBidEvents(String itemId, List<BidEvent> events,
            boolean isExtended, LocalDateTime newEndTime) {
        try {
            for (BidEvent event : events) {
                BidPayload payload = new BidPayload(
                        itemId, event.userId(), event.bidPrice(), event.bidTime());
                AuctionRoomManager.getInstance()
                        .broadcastToRoom(itemId, SocketEventConstants.EVENT_NEW_BID, payload);
                LOGGER.fine(String.format("Broadcast NEW_BID: itemId=%s userId=%s price=%.2f",
                        itemId, event.userId(), event.bidPrice()));
            }

            if (isExtended && newEndTime != null) {
                AuctionExtendedPayload extPayload = new AuctionExtendedPayload(
                        itemId, newEndTime.toString());
                AuctionRoomManager.getInstance()
                        .broadcastToRoom(itemId, SocketEventConstants.EVENT_AUCTION_EXTENDED, extPayload);
                LOGGER.fine("Broadcast AUCTION_EXTENDED cho item " + itemId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Broadcast thất bại cho item " + itemId, e);
        }
    }

    // Private — helpers

    private double computeImmediateAutoBidPrice(double currentPrice, double bidStep, double increment) {
        return currentPrice + Math.max(bidStep, increment);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            LOGGER.log(Level.SEVERE, "Rollback thất bại", rollbackError);
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (SQLException closeError) {
            LOGGER.log(Level.WARNING, "Đóng connection thất bại", closeError);
        }
    }

    private LocalDateTime antiSnipe(LocalDateTime currentEndTime, Connection conn, String itemId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = ChronoUnit.SECONDS.between(now, currentEndTime);

        if (secondsRemaining >= 0 && secondsRemaining < ANTI_SNIPE_SECONDS) {
            LocalDateTime newEndTime = now.plusSeconds(ANTI_SNIPE_SECONDS);
            if (!itemRepository.extendEndTime(conn, itemId, newEndTime)) {
                throw new SQLException("Failed to extend end time for item: " + itemId);
            }
            LOGGER.info("Anti-snipe: gia hạn đến " + newEndTime);
            return newEndTime;
        }
        return null;
    }

    private record BidEvent(String userId, double bidPrice, String bidTime) {
    }
}
