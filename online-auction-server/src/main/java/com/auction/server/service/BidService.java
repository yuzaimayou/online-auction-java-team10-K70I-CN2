package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.*;
import com.auction.server.util.AuctionLockManager;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.DataException;
import com.auction.shared.exception.ErrorCode;
import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BidService — xử lý toàn bộ nghiệp vụ đặt giá (manual bid và auto-bid).
 *
 * Nguyên tắc thiết kế:
 *  1. Mỗi thao tác ghi DB chạy trong một transaction duy nhất (conn.setAutoCommit(false)).
 *  2. Mọi nhánh thất bại đều rollback TRƯỚC khi return false.
 *  3. Validate nghiệp vụ (item, user, giá, thời gian) được tập trung vào
 *     BidValidator — BidService chỉ điều phối, không tự validate.
 *  4. UserRepository được inject (không new() inline) để tái sử dụng connection pool.
 *  5. Anti-sniping áp dụng nhất quán cho cả manual bid lẫn auto-bid.
 *  6. broadcastNewBid() luôn được gọi SAU khi commit, ngoài synchronized block.
 */
public class BidService {

    private static final Logger LOGGER = Logger.getLogger(BidService.class.getName());
    private static final double PRICE_EPSILON  = 0.000001d;
    private static final int AUTO_BID_MAX_ROUNDS = 200;
    private static final long ANTI_SNIPE_SECONDS  = 60L;

    // Dependencies — inject qua constructor, không new() inline
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
     *
     * Flow:
     *  1. Validate đầu vào (item, user, status, thời gian, giá).
     *  2. Xử lý ví (freeze mới / unfreeze cũ) trong transaction.
     *  3. Ghi bid + cập nhật current bidder.
     *  4. Anti-snipe nếu còn < 60s.
     *  5. Chạy auto-bid rounds cho các user đã đăng ký.
     *  6. Commit → broadcast.
     */
    public boolean placeBid(String itemId, String userId, double bidPrice) {
        if (isBlank(itemId) || isBlank(userId)) {
            LOGGER.warning("Bid rejected: itemId hoặc userId rỗng");
            return false;
        }

        FinalBid finalBid     = null;
        boolean isExtended   = false;
        LocalDateTime newEndTime = null;

        synchronized (AuctionLockManager.getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                // 1. Load dữ liệu trong transaction (tránh stale read)
                Item item = itemRepository.findById(conn, itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Item không tồn tại - " + itemId);
                    return false;
                }

                // FIX: Load user trong cùng transaction thay vì dùng connection riêng
                User user = userRepository.findById(conn, userId);

                // 2. Validate nghiệp vụ
                try {
                    bidValidator.validateForManualBid(item, user, userId, bidPrice);
                } catch (InvalidBidException | AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: " + e.getMessage());
                    return false;
                }
                // FIX: Lấy lastBidder từ DB trong cùng transaction để tránh stale read
                String lastBidder = bidRepository.findLastBidder(conn, itemId);
                if (userId.equals(lastBidder)) {
                    conn.rollback();
                    LOGGER.info("Bid rejected: Cùng user không được bid liên tiếp - " + itemId);
                    return false;
                }

                // FIX: Lấy currentTopPlayer từ DB (không từ item snapshot) để đảm bảo nhất quán
                String currentTopPlayerId = itemRepository.getCurrentBidderId(conn, itemId);
                double currentPrice = item.getHighestCurrentPrice();

                handleWalletMovement(conn, itemId, userId, bidPrice, currentTopPlayerId, currentPrice);

                //  4. Ghi bid
                String bidTime = LocalDateTime.now().toString();
                LOGGER.info(String.format(
                        "[MANUAL_BID] time=%s itemId=%s userId=%s currentPrice=%.2f bidPrice=%.2f",
                        bidTime, itemId, userId, currentPrice, bidPrice));

                bidRepository.createBid(conn, itemId, userId, bidPrice, bidTime);
                itemRepository.updateCurrentBidder(conn, itemId, bidPrice, userId);

                //  5. Anti-snipe
                long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getEndTime());
                if (secondsRemaining >= 0 && secondsRemaining < ANTI_SNIPE_SECONDS) {
                    newEndTime = LocalDateTime.now().plusSeconds(ANTI_SNIPE_SECONDS);
                    itemRepository.extendEndTime(conn, itemId, newEndTime);
                    isExtended = true;
                    LOGGER.info("Anti-snipe: gia hạn đến " + newEndTime);
                }

                //  6. Auto-bid rounds
                finalBid = runAutoBiddingRounds(conn, itemId, userId, bidPrice, bidTime);
                conn.commit();

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "DB error trong placeBid - " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error trong placeBid - " + e.getMessage(), e);
                return false;
            }
        }

        // Broadcast ngoài synchronized block (tránh giữ lock khi I/O)
        broadcastAfterBid(itemId, finalBid, isExtended, newEndTime);
        return true;
    }

    /**
     * Đăng ký auto-bid VÀ đặt bid ngay nếu điều kiện cho phép.
     *
     * Flow:
     *  1. Validate đầu vào.
     *  2. Validate nghiệp vụ (item, user, status, thời gian, increment).
     *  3. Upsert auto-bid config.
     *  4. Quyết định: REGISTER_ONLY nếu đang dẫn đầu, PLACE_BID nếu chưa.
     *  5. Nếu PLACE_BID: xử lý ví → ghi bid → anti-snipe → auto-bid rounds.
     *  6. Commit → broadcast.
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

        FinalBid finalBid   = null;
        boolean shouldBroadcast = false;
        boolean isExtended = false;
        LocalDateTime newEndTime   = null;

        synchronized (AuctionLockManager.getItemLock(itemId)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                //  1. Load dữ liệu
                Item item = itemRepository.findById(conn, itemId);
                if (item == null) {
                    conn.rollback();
                    LOGGER.info("Auto-bid rejected: Item không tồn tại - " + itemId);
                    return false;
                }

                User user = userRepository.findById(conn, userId);

                //  2. Validate nghiệp vụ
                try {
                    bidValidator.validateForAutoBid(item, user, userId, maxBid, increment);
                } catch (InvalidBidException | AuctionClosedException e) {
                    conn.rollback();
                    LOGGER.info("Auto-bid rejected: " + e.getMessage());
                    return false;
                }

                //  3. Upsert config (TRƯỚC khi validate maxBid vs price) ---
                // FIX: upsert trước rồi mới quyết định bid hay không là đúng,
                // nhưng nếu REJECT thì phải rollback để xóa upsert vừa làm.
                // Với ON CONFLICT UPDATE thì rollback sẽ khôi phục giá trị cũ.
                bidRepository.upsertAutoBid(conn, itemId, userId, maxBid, increment,
                        LocalDateTime.now().toString());

                String currentBidderId = itemRepository.getCurrentBidderId(conn, itemId);
                double currentPrice    = item.getHighestCurrentPrice();
                double immediateBidPrice = computeImmediateAutoBidPrice(
                        currentPrice, item.getBidStep(), increment);

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION_INPUT] time=%s itemId=%s userId=%s " +
                                "currentBidder=%s currentPrice=%.2f nextBid=%.2f maxBid=%.2f",
                        LocalDateTime.now(), itemId, userId,
                        currentBidderId, currentPrice, immediateBidPrice, maxBid));

                if (userId.equals(currentBidderId)) {
                    // Đang dẫn đầu → chỉ đăng ký, không bid thêm
                    conn.commit();
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION=REGISTER_ONLY] itemId=%s userId=%s",
                            itemId, userId));
                    return true;
                }

                // FIX: validate maxBid TRƯỚC khi thực hiện wallet / bid
                if (immediateBidPrice + PRICE_EPSILON <= currentPrice
                        || immediateBidPrice > maxBid + PRICE_EPSILON) {
                    conn.rollback();  // rollback upsert vì config không hợp lệ
                    LOGGER.info(String.format(
                            "[AUTO_BID_REGISTER][DECISION=REJECT_MAX_TOO_LOW] itemId=%s userId=%s " +
                                    "immediateBid=%.2f maxBid=%.2f",
                            itemId, userId, immediateBidPrice, maxBid));
                    return false;
                }

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][DECISION=PLACE_IMMEDIATE_BID] itemId=%s userId=%s bidPrice=%.2f",
                        itemId, userId, immediateBidPrice));

                //  5. Xử lý ví + ghi bid
                handleWalletMovement(conn, itemId, userId, immediateBidPrice,
                        currentBidderId, currentPrice);

                String bidTime = LocalDateTime.now().toString();
                bidRepository.createBid(conn, itemId, userId, immediateBidPrice, bidTime);
                itemRepository.updateCurrentBidder(conn, itemId, immediateBidPrice, userId);

                LOGGER.info(String.format(
                        "[AUTO_BID_REGISTER][IMMEDIATE_BID] time=%s itemId=%s userId=%s bidPrice=%.2f",
                        bidTime, itemId, userId, immediateBidPrice));

                // 6. Anti-snipe (FIX: áp dụng nhất quán cho cả auto-bid)
                long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getEndTime());
                if (secondsRemaining >= 0 && secondsRemaining < ANTI_SNIPE_SECONDS) {
                    newEndTime = LocalDateTime.now().plusSeconds(ANTI_SNIPE_SECONDS);
                    itemRepository.extendEndTime(conn, itemId, newEndTime);
                    isExtended = true;
                    LOGGER.info("Anti-snipe (auto-bid): gia hạn đến " + newEndTime);
                }

                // 7. Auto-bid rounds
                finalBid = runAutoBiddingRounds(conn, itemId, userId, immediateBidPrice, bidTime);
                conn.commit();
                shouldBroadcast = true;

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "DB error trong registerAutoBidAndMaybeBid - " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error trong registerAutoBidAndMaybeBid - " + e.getMessage(), e);
                return false;
            }
        }

        if (shouldBroadcast) {
            broadcastAfterBid(itemId, finalBid, isExtended, newEndTime);
        }
        return true;
    }

    /**
     * Lấy trạng thái auto-bid hiện tại của user cho một item.
     */
    public AutoBidPayload getAutoBidStatus(String itemId, String userId) {
        if (isBlank(itemId) || isBlank(userId)) return null;

        AutoBidPayload config = bidRepository.findActiveAutoBid(itemId, userId);
        return config != null ? config : new AutoBidPayload(itemId, userId, null, null, false);
    }

    /**
     * Hủy auto-bid.
     */
    public boolean cancelAutoBid(String itemId, String userId) {
        if (isBlank(itemId) || isBlank(userId)) return false;
        return bidRepository.deactivateAutoBidIfPresent(itemId, userId);
    }

    // Private — wallet

    /**
     * Freeze tiền người bid mới, unfreeze tiền người bid cũ.
     *
     * FIX: Method ném Exception thay vì bọc lại thành SQLException,
     * để caller có thể rollback đúng transaction rồi return false.
     * Không tự rollback ở đây vì không giữ reference đến conn.
     */
    private void handleWalletMovement(Connection conn, String itemId,
                                      String bidderId, double bidPrice,
                                      String prevBidderId, double prevBidPrice) throws Exception {

        // 1. Kiểm tra số dư
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

        // 2. Freeze cho người bid mới
        if (!walletRepository.freezeAmount(conn, bidderId, bidPrice)) {
            throw DataException.of(ErrorCode.WALLET_OPERATION_FAILED,
                    "Không thể khóa số dư: " + bidderId);
        }

        // 3. Log FREEZE (không critical — chỉ warning nếu lỗi)
        try {
            txLogRepository.logFreeze(conn, bidderId, bidPrice, balance, balance - bidPrice, itemId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không ghi được log FREEZE", e);
        }

        // 4. Unfreeze cho người bid trước
        if (prevBidderId != null && !prevBidderId.isBlank() && !prevBidderId.equals(bidderId)) {
            if (!walletRepository.unfreezeAmount(conn, prevBidderId, prevBidPrice)) {
                throw DataException.of(ErrorCode.WALLET_OPERATION_FAILED,
                        "Không thể mở khóa số dư người cũ: " + prevBidderId);
            }

            // Log UNFREEZE
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
     * Dừng khi không còn candidate hợp lệ hoặc đạt MAX_ROUNDS.
     *
     * Ném Exception ra ngoài để caller rollback toàn bộ transaction.
     */
    private FinalBid runAutoBiddingRounds(Connection conn, String itemId,
                                          String leadingUserId, double currentPrice,
                                          String currentBidTime) throws Exception {
        String currentLeader = leadingUserId;
        double livePrice = currentPrice;
        String liveBidTime = currentBidTime;

        for (int round = 0; round < AUTO_BID_MAX_ROUNDS; round++) {
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

            AutoBidResolver.ResolvedAutoBid candidate =
                    autoBidResolver.selectNextBid(autoBids, currentLeader, livePrice);
            if (candidate == null) {
                LOGGER.info(String.format(
                        "[AUTO_BID_ROUND][STOP] reason=no_valid_candidate leader=%s price=%.2f",
                        currentLeader, livePrice));
                break;
            }

            // Kiểm tra số dư của candidate
            double[] candidateBalances = walletRepository.getBalances(conn, candidate.userId());
            if (candidateBalances == null || candidateBalances.length == 0
                    || candidateBalances[0] < candidate.bidPrice()) {
                LOGGER.warning(String.format(
                        "[AUTO_BID_ROUND][SKIP] Không đủ tiền: userId=%s needs=%.2f has=%.2f",
                        candidate.userId(),
                        candidate.bidPrice(),
                        candidateBalances != null && candidateBalances.length > 0 ? candidateBalances[0] : 0d));
                bidRepository.deactivateAutoBidIfPresent(conn, itemId, candidate.userId());
                continue;
            }

            // Xử lý ví
            handleWalletMovement(conn, itemId,
                    candidate.userId(), candidate.bidPrice(),
                    currentLeader, livePrice);

            // Ghi bid
            String bidTime = LocalDateTime.now().toString();
            bidRepository.createBid(conn, itemId, candidate.userId(), candidate.bidPrice(), bidTime);
            itemRepository.updateCurrentBidder(conn, itemId, candidate.bidPrice(), candidate.userId());

            LOGGER.info(String.format(
                    "[AUTO_BID_ROUND][ACCEPT] time=%s itemId=%s userId=%s bidPrice=%.2f",
                    bidTime, itemId, candidate.userId(), candidate.bidPrice()));

            livePrice     = candidate.bidPrice();
            currentLeader = candidate.userId();
            liveBidTime   = bidTime;
        }

        return new FinalBid(currentLeader, livePrice, liveBidTime);
    }

    // Private — broadcast

    private void broadcastAfterBid(String itemId, FinalBid finalBid,
                                   boolean isExtended, LocalDateTime newEndTime) {
        try {
            if (finalBid != null) {
                BidPayload payload = new BidPayload(
                        itemId, finalBid.userId(), finalBid.bidPrice(), finalBid.bidTime());
                AuctionRoomManager.getInstance()
                        .broadcastToRoom(itemId, SocketEventConstants.EVENT_NEW_BID, payload);
                LOGGER.fine("Broadcast NEW_BID cho item " + itemId);
            }

            if (isExtended && newEndTime != null) {
                com.auction.shared.model.payloads.AuctionExtendedPayload extPayload =
                        new com.auction.shared.model.payloads.AuctionExtendedPayload(
                                itemId, newEndTime.toString());
                AuctionRoomManager.getInstance()
                        .broadcastToRoom(itemId, SocketEventConstants.EVENT_AUCTION_EXTENDED, extPayload);
                LOGGER.fine("Broadcast AUCTION_EXTENDED cho item " + itemId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Broadcast thất bại cho item " + itemId, e);
        }
    }

    private void broadcastNewBid(String itemId, FinalBid finalBid) {
        broadcastAfterBid(itemId, finalBid, false, null);
    }

    // Private — validation helpers (delegate sang BidValidator)
    private double computeImmediateAutoBidPrice(double currentPrice, double bidStep, double increment) {
        return currentPrice + Math.max(bidStep, increment);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // Private record

    private record FinalBid(String userId, double bidPrice, String bidTime) {}

    // Inner class — BidValidator
    //Tách validate nghiệp vụ ra khỏi BidService.
    // BidValidator nhận các object đã được load sẵn (Item, User),
    // KHÔNG tự query DB — giúp giảm số lần query và dễ unit test.
    // =========================================================================

    /**
     * Tập trung toàn bộ validate nghiệp vụ cho bid và auto-bid.
     * Không phụ thuộc DB — chỉ làm việc với object đã được load.
     */
    public static class BidValidator {
        private final double epsilon;
        public BidValidator(double epsilon) {
            this.epsilon = epsilon;
        }
        /** Validate cho manual bid. */
        public void validateForManualBid(Item item, User user, String userId, double bidPrice)
                throws InvalidBidException, AuctionClosedException {

            validateItemAndUser(item, user, userId);
            validateStatus(item);
            validateAuctionTime(item);

            double minAllowed = item.getHighestCurrentPrice() + item.getBidStep();
            if (bidPrice + epsilon < minAllowed) {
                throw InvalidBidException.of(ErrorCode.BID_PRICE_TOO_LOW,
                        String.format("Giá bid %.2f thấp hơn tối thiểu %.2f", bidPrice, minAllowed));
            }
        }

        /** Validate cho auto-bid registration. */
        public void validateForAutoBid(Item item, User user, String userId,
                                       double maxBid, double increment)
                throws InvalidBidException, AuctionClosedException {

            validateItemAndUser(item, user, userId);
            validateStatus(item);
            validateAuctionTime(item);

            if (increment + epsilon < item.getBidStep()) {
                throw InvalidBidException.of(ErrorCode.INVALID_BID_PARAMETERS,
                        String.format("Increment %.2f thấp hơn bidStep %.2f", increment, item.getBidStep()));
            }
        }

        //  Helpers

        private void validateItemAndUser(Item item, User user, String userId)
                throws InvalidBidException {

            if (item.getSellerId().equals(userId)) {
                throw InvalidBidException.of(ErrorCode.SELLER_CANNOT_BID,
                        "Người bán không được tự đặt giá - itemId=" + item.getId());
            }
            if (user != null && "Suspended".equalsIgnoreCase(user.getStatus())) {
                throw InvalidBidException.of(ErrorCode.INVALID_INPUT,
                        "User bị cấm không được đặt giá - userId=" + userId);
            }
        }

        private void validateStatus(Item item) throws InvalidBidException {
            AuctionStatus stored   = item.getStoredStatus();       // AuctionStatus.BANNED...
            AuctionStatus computed = item.getStatus();

            if (stored == AuctionStatus.BANNED
                    || stored == AuctionStatus.ENDED
                    || computed != AuctionStatus.ONGOING) {
                throw InvalidBidException.of(ErrorCode.INVALID_BID,
                        "Item không ở trạng thái đấu giá hợp lệ - itemId=" + item.getId());
            }
        }

        private void validateAuctionTime(Item item) throws AuctionClosedException {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(item.getEndTime())) {
                throw AuctionClosedException.of(ErrorCode.AUCTION_ALREADY_CLOSED,
                        "Phiên đấu giá đã kết thúc lúc " + item.getEndTime());
            }
            if (now.isBefore(item.getStartTime())) {
                throw AuctionClosedException.of(ErrorCode.AUCTION_NOT_STARTED,
                        "Phiên đấu giá chưa bắt đầu, sẽ bắt đầu lúc " + item.getStartTime());
            }
        }
    }
}