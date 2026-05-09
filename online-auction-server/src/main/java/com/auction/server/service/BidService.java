package com.auction.server.service;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.item.Item;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BidService {
    private static final double PRICE_EPSILON = 0.000001d;
    private static final int AUTO_BID_MAX_ROUNDS = 200;

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final AutoBidResolver autoBidResolver;
    private final WalletService walletService;          // ← wallet-aware bidding
    private final ConcurrentMap<String, Object> itemLocks;

    public BidService() {
        this.bidRepository  = new BidRepository();
        this.itemRepository = new ItemRepository();
        this.autoBidResolver = new AutoBidResolver(PRICE_EPSILON);
        this.walletService  = new WalletService();
        this.itemLocks      = new ConcurrentHashMap<>();
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

                Item item = itemRepository.findById(conn, itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    return false;
                }

                // [FIX] Kiểm tra xem phiên đấu giá đã hết thời gian chưa
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
                        LocalDateTime.now().toString()
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

    /**
     * Place a bid with full wallet money-freezing semantics.
     *
     * <p>Delegates to {@link WalletService#placeBid} which handles the entire
     * freeze / unfreeze / update / log flow inside ONE database transaction.
     * After a successful commit, auto-bid resolution and live broadcast run
     * in a separate transaction (same pattern as before).
     *
     * @param bidTime optional ISO timestamp from client; uses server time when null
     * @return true if the bid was accepted
     */
    public boolean placeBid(String itemId, String userId, double bidPrice, String bidTime) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        // ── Wallet-aware bid (freeze money, refund previous bidder, log) ───
        WalletService.BidResult result = walletService.placeBid(itemId, userId, bidPrice);
        if (!result.success) {
            System.out.println("[BidService] Bid rejected: " + result.errorMessage);
            return false;
        }

        // ── Auto-bid resolution (runs AFTER the committed bid) ────────────
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            runAutoBiddingRounds(conn, itemId, userId, bidPrice);
            conn.commit();
        } catch (Exception e) {
            // Auto-bid failure does not roll back the primary bid
            System.err.println("[BidService] Auto-bid round error: " + e.getMessage());
        }

        // ── Broadcast to room ─────────────────────────────────────────────
        try {
            String resolvedBidTime = (bidTime == null || bidTime.isBlank())
                    ? LocalDateTime.now().toString() : bidTime;
            BidPayload newBidData = new BidPayload(itemId, userId, bidPrice, resolvedBidTime);
            String jsonPayload = new com.google.gson.Gson().toJson(newBidData);
            AuctionRoomManager.getInstance().broadcastToRoom(itemId, "NEW_BID", jsonPayload);
        } catch (Exception e) {
            System.err.println("[BidService] Broadcast error: " + e.getMessage());
        }

        return true;
    }

    private void runAutoBiddingRounds(Connection conn, String itemId, String leadingUserId, double currentPrice) throws Exception {
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
                throw new Exception("Failed to create auto bid");
            }
            if (!itemRepository.updateCurrentPrice(conn, itemId, candidate.bidPrice())) {
                throw new Exception("Failed to update current price after auto bid");
            }

            livePrice = candidate.bidPrice();
            currentLeader = candidate.userId();
        }
    }

    private Object getItemLock(String itemId) {
        return itemLocks.computeIfAbsent(itemId, ignored -> new Object());
    }

}

