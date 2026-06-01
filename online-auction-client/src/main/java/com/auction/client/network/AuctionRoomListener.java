package com.auction.client.network;

import com.auction.shared.model.payloads.BidPayload;
import java.time.LocalDateTime;

public interface AuctionRoomListener {
    void onNewBid(BidPayload payload);
    void onAuctionExtended(LocalDateTime newEndTime);
    void onItemBanned(String itemId);
}