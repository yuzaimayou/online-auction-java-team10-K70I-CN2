package com.auction.server.service;

import com.auction.server.service.bid.BidService;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BidServiceTest {

    private static final double EPSILON = 0.000001d;

    @Test
    void validateForManualBid_rejects_seller_bidding() {
        BidService.BidValidator validator = new BidService.BidValidator(EPSILON);
        Item item = baseItem(LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));
        when(item.getSellerId()).thenReturn("seller1");

        User user = new User("seller1", "seller", "pass");

        assertThrows(InvalidBidException.class,
                () -> validator.validateForManualBid(item, user, "seller1", 200.0));
    }

    @Test
    void validateForManualBid_rejects_bid_below_minimum() {
        BidService.BidValidator validator = new BidService.BidValidator(EPSILON);
        Item item = baseItem(LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));
        when(item.getHighestCurrentPrice()).thenReturn(1000.0);
        when(item.getBidStep()).thenReturn(50.0);

        User user = new User("u1", "alice", "pass");

        assertThrows(InvalidBidException.class,
                () -> validator.validateForManualBid(item, user, "u1", 1000.0));
    }

    @Test
    void validateForManualBid_rejects_when_auction_not_started() {
        BidService.BidValidator validator = new BidService.BidValidator(EPSILON);
        Item item = baseItem(LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(10));

        User user = new User("u1", "alice", "pass");

        assertThrows(AuctionClosedException.class,
                () -> validator.validateForManualBid(item, user, "u1", 200.0));
    }

    @Test
    void validateForManualBid_accepts_valid_bid() {
        BidService.BidValidator validator = new BidService.BidValidator(EPSILON);
        Item item = baseItem(LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));
        when(item.getHighestCurrentPrice()).thenReturn(100.0);
        when(item.getBidStep()).thenReturn(10.0);

        User user = new User("u1", "alice", "pass");

        assertDoesNotThrow(() -> validator.validateForManualBid(item, user, "u1", 120.0));
    }

    @Test
    void validateForAutoBid_rejects_increment_below_bid_step() {
        BidService.BidValidator validator = new BidService.BidValidator(EPSILON);
        Item item = baseItem(LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));
        when(item.getBidStep()).thenReturn(50.0);

        User user = new User("u1", "alice", "pass");

        assertThrows(InvalidBidException.class,
                () -> validator.validateForAutoBid(item, user, "u1", 200.0, 10.0));
    }

    private Item baseItem(LocalDateTime startTime, LocalDateTime endTime) {
        Item item = mock(Item.class);
        when(item.getSellerId()).thenReturn("seller-default");
        when(item.getHighestCurrentPrice()).thenReturn(100.0);
        when(item.getBidStep()).thenReturn(10.0);
        when(item.getStartTime()).thenReturn(startTime);
        when(item.getEndTime()).thenReturn(endTime);
        when(item.getStoredStatus()).thenReturn(AuctionStatus.ONGOING);
        when(item.getStatus()).thenReturn(AuctionStatus.ONGOING);
        when(item.getId()).thenReturn("item-1");
        return item;
    }
}