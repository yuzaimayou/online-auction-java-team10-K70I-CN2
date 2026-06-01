package com.auction.server.service.bid;

import com.auction.server.repository.HistoryRepository;
import com.auction.shared.model.dto.BidHistoryItemDTO;

import java.util.List;

/**
 * Service quan ly lich su bid va du lieu cho Price Curve
 * Requirement 3.2.5: Bid History Visualization - Realtime Price Curve
 */
public class BidHistoryService {
    private static final BidHistoryService instance = new BidHistoryService();
    private final HistoryRepository historyRepository = HistoryRepository.getInstance();

    public static BidHistoryService getInstance() {
        return instance;
    }

    public List<BidHistoryItemDTO> getHistory(String itemId) {
        return historyRepository.getBidHistoryForItem(itemId);
    }
}