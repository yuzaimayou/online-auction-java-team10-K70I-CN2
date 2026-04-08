package com.auction.server.service;

import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.product.Item;

import java.time.LocalDateTime;

public class BidService {
    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;

    public BidService() {
        this.bidRepository = new BidRepository();
        this.itemRepository = new ItemRepository();
    }

    public boolean placeBid(String itemId, String userId, double bidPrice, String bidTime) {
        Item item = itemRepository.findById(itemId);

        if (item == null || userId == null || userId.isBlank()) {
            return false;
        }

        if (item.getSellerId().equals(userId)) {
            return false;
        }

        double minAllowedPrice = item.getHighestCurrentPrice() + item.getBidStep();
        if (bidPrice < minAllowedPrice) {
            return false;
        }

        String resolvedBidTime = (bidTime == null || bidTime.isBlank()) ? LocalDateTime.now().toString() : bidTime;
        boolean created = bidRepository.createBid(itemId, userId, bidPrice, resolvedBidTime);
        if (!created) {
            return false;
        }

        itemRepository.updateCurrentPrice(itemId, bidPrice);
        return true;
    }
}

