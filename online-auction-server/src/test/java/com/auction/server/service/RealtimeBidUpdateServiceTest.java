package com.auction.server.service;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.auction.BidTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
class RealtimeBidUpdateServiceTest {

    private RealtimeBidUpdateService service;

    @BeforeEach
    void setUp() {
        service = new RealtimeBidUpdateService();
    }

    @Test
    void shouldNotifyObserverOnNewBid() {
        Auction auction = mock(Auction.class);
        when(auction.getAuctionId()).thenReturn("A1");

        RealtimeBidUpdateService.BidObserver observer = mock(RealtimeBidUpdateService.BidObserver.class);
        service.addObserver("A1", observer);

        BidTransaction bid = new BidTransaction("B1", "A1", "U1", 1000, LocalDateTime.now());

        service.notifyNewBid(auction, bid);

        verify(observer).onNewBid(auction, bid);
    }
}
