package com.auction.server.service;

import com.auction.server.repository.BidRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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

        return autoBids.stream()
                .filter(config -> !config.getUserId().equals(currentLeader))
                .map(config -> toCandidate(config, currentPrice))
                .filter(candidate -> candidate.bidPrice() > currentPrice + epsilon)
                .max(Comparator
                        .comparingDouble(ResolvedAutoBid::bidPrice)
                        .thenComparing(ResolvedAutoBid::registeredAt, Comparator.reverseOrder())
                        .thenComparing(ResolvedAutoBid::userId, Comparator.reverseOrder()))
                .orElse(null);
    }

    private ResolvedAutoBid toCandidate(BidRepository.AutoBidConfig config, double currentPrice) {
        double nextBid = Math.min(config.getMaxBid(), currentPrice + config.getIncrement());
        return new ResolvedAutoBid(config.getUserId(), nextBid, config.getRegisteredAt());
    }

    public record ResolvedAutoBid(String userId, double bidPrice, LocalDateTime registeredAt) {
    }
}
