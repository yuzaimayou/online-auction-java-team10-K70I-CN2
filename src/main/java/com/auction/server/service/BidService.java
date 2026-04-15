package com.auction.server.service;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.product.Item;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BidService {
    private static final double PRICE_EPSILON = 0.000001d;
    private static final int AUTO_BID_MAX_ROUNDS = 200;

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final ConcurrentMap<String, Object> itemLocks;

    public BidService() {
        this.bidRepository = new BidRepository();
        this.itemRepository = new ItemRepository();
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
            try (Connection conn = DatabaseConnection.connect()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
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

    public boolean placeBid(String itemId, String userId, double bidPrice, String bidTime) {
        if (itemId == null || itemId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        synchronized (getItemLock(itemId)) {
            try (Connection conn = DatabaseConnection.connect()) {
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null || item.getSellerId().equals(userId)) {
                    conn.rollback();
                    return false;
                }

                double minAllowedPrice = item.getHighestCurrentPrice() + item.getBidStep();
                if (bidPrice + PRICE_EPSILON < minAllowedPrice) {
                    conn.rollback();
                    return false;
                }

                String resolvedBidTime = (bidTime == null || bidTime.isBlank())
                        ? LocalDateTime.now().toString()
                        : bidTime;
                boolean created = bidRepository.createBid(conn, itemId, userId, bidPrice, resolvedBidTime);
                if (!created) {
                    conn.rollback();
                    return false;
                }

                if (!itemRepository.updateCurrentPrice(conn, itemId, bidPrice)) {
                    conn.rollback();
                    return false;
                }

                runAutoBiddingRounds(conn, itemId, userId, bidPrice);
                conn.commit();
                return true;

            } catch (Exception e) {
                return false;
            }
        }
    }

    private void runAutoBiddingRounds(Connection conn, String itemId, String leadingUserId, double currentPrice) throws Exception {
        String currentLeader = leadingUserId;
        double livePrice = currentPrice;

        for (int round = 0; round < AUTO_BID_MAX_ROUNDS; round++) {
            List<BidRepository.AutoBidConfig> autoBids = bidRepository.findActiveAutoBids(conn, itemId);
            if (autoBids.isEmpty()) {
                return;
            }

            BidRepository.AutoBidConfig leaderAutoBid = findAutoBidConfig(autoBids, currentLeader);
            AutoBidCandidate candidate = chooseNextAutoBid(autoBids, currentLeader, livePrice, leaderAutoBid);
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

    private AutoBidCandidate chooseNextAutoBid(List<BidRepository.AutoBidConfig> autoBids,
                                               String currentLeader,
                                               double currentPrice,
                                               BidRepository.AutoBidConfig leaderAutoBid) {
        return autoBids.stream()
                .filter(config -> !config.getUserId().equals(currentLeader))
                .filter(config -> leaderAutoBid == null || config.getMaxBid() > leaderAutoBid.getMaxBid() + PRICE_EPSILON)
                .map(config -> toCandidate(config, currentPrice))
                .filter(candidate -> candidate.bidPrice() > currentPrice + PRICE_EPSILON)
                .max(Comparator
                        .comparingDouble(AutoBidCandidate::bidPrice)
                        .thenComparing(AutoBidCandidate::registeredAt, Comparator.reverseOrder()))
                .orElse(null);
    }

    private BidRepository.AutoBidConfig findAutoBidConfig(List<BidRepository.AutoBidConfig> autoBids, String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        for (BidRepository.AutoBidConfig autoBid : autoBids) {
            if (userId.equals(autoBid.getUserId())) {
                return autoBid;
            }
        }

        return null;
    }

    private AutoBidCandidate toCandidate(BidRepository.AutoBidConfig config, double currentPrice) {
        double nextBid = Math.min(config.getMaxBid(), currentPrice + config.getIncrement());
        return new AutoBidCandidate(config.getUserId(), nextBid, config.getRegisteredAt());
    }

    private Object getItemLock(String itemId) {
        return itemLocks.computeIfAbsent(itemId, ignored -> new Object());
    }

    private record AutoBidCandidate(String userId, double bidPrice, LocalDateTime registeredAt) {
    }
}

