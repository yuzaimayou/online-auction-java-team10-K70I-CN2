package com.auction.server.service;

import com.auction.server.repository.HistoryRepository;
import com.auction.shared.model.dto.BidHistoryItemDTO;

import java.util.List;

/**
 * Service quan ly lich su bid va du lieu cho Price Curve
 * Requirement 3.2.5: Bid History Visualization - Realtime Price Curve
 */
public class BidHistoryService {
    private static BidHistoryService instance;
    private final HistoryRepository historyRepository = HistoryRepository.getInstance();

    public static BidHistoryService getInstance() {
        if (instance == null) {
            instance = new BidHistoryService();
        }
        return instance;
    }

    public List<BidHistoryItemDTO> getHistory(String itemId) {
        return historyRepository.getBidHistoryForItem(itemId);
    }
}