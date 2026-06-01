package com.auction.server.service;

import com.auction.server.repository.BidRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Resolves auto-bid conflicts deterministically.
 *
 * Rules:
 * - Compare all eligible auto-bids at the same time.
 * - Each next bid is capped by maxBid.
 * - Higher valid next bid wins.
 * - If same next bid, earlier registration time wins.
 * - If still tied, userId is used for deterministic ordering.
 */
public class AutoBidResolver {
    private static final Logger LOGGER = Logger.getLogger(AutoBidResolver.class.getName());
    private final double epsilon;

    public AutoBidResolver(double epsilon) {
        this.epsilon = epsilon;
    }

    public ResolvedAutoBid selectNextBid(List<BidRepository.AutoBidConfig> autoBids,
                                         String currentLeader,
                                         double currentPrice) {
        if (autoBids == null || autoBids.isEmpty()) {
            return null;
        }

        for (BidRepository.AutoBidConfig config : autoBids) {
            if (config.getUserId().equals(currentLeader)) {
                LOGGER.info(String.format("[AUTO_BID_RESOLVE][REJECT] time=%s userId=%s reason=current_leader currentPrice=%.2f maxBid=%.2f increment=%.2f",
                        LocalDateTime.now(), config.getUserId(), currentPrice, config.getMaxBid(), config.getIncrement()));
                continue;
            }

            ResolvedAutoBid candidate = toCandidate(config, currentPrice);
            if (candidate.bidPrice() <= currentPrice + epsilon) {
                LOGGER.info(String.format("[AUTO_BID_RESOLVE][REJECT] time=%s userId=%s reason=max_or_increment_too_low currentPrice=%.2f nextBid=%.2f maxBid=%.2f increment=%.2f",
                        LocalDateTime.now(), config.getUserId(), currentPrice, candidate.bidPrice(), config.getMaxBid(), config.getIncrement()));
            } else {
                LOGGER.info(String.format("[AUTO_BID_RESOLVE][CANDIDATE] time=%s userId=%s currentPrice=%.2f nextBid=%.2f maxBid=%.2f increment=%.2f",
                        LocalDateTime.now(), config.getUserId(), currentPrice, candidate.bidPrice(), config.getMaxBid(), config.getIncrement()));
            }
        }

        ResolvedAutoBid selected = autoBids.stream()
                .filter(config -> !config.getUserId().equals(currentLeader))
                .map(config -> toCandidate(config, currentPrice))
                .filter(candidate -> candidate.bidPrice() > currentPrice + epsilon)
                .max(Comparator
                        .comparingDouble(ResolvedAutoBid::bidPrice)
                        .thenComparing(ResolvedAutoBid::registeredAt, Comparator.reverseOrder())
                        .thenComparing(ResolvedAutoBid::userId, Comparator.reverseOrder()))
                .orElse(null);

        if (selected == null) {
            LOGGER.info(String.format("[AUTO_BID_RESOLVE][SELECT_NONE] time=%s currentLeader=%s currentPrice=%.2f",
                    LocalDateTime.now(), currentLeader, currentPrice));
        } else {
            LOGGER.info(String.format("[AUTO_BID_RESOLVE][SELECT] time=%s userId=%s bidPrice=%.2f currentLeader=%s currentPrice=%.2f",
                    LocalDateTime.now(), selected.userId(), selected.bidPrice(), currentLeader, currentPrice));
        }

        return selected;
    }

    private ResolvedAutoBid toCandidate(BidRepository.AutoBidConfig config, double currentPrice) {
        double nextBid = Math.min(config.getMaxBid(), currentPrice + config.getIncrement());
        return new ResolvedAutoBid(config.getUserId(), nextBid, config.getRegisteredAt());
    }

    public record ResolvedAutoBid(String userId, double bidPrice, LocalDateTime registeredAt) {
    }
}
