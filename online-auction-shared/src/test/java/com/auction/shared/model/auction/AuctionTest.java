package com.auction.shared.model.auction;

import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
class AuctionTest {

    private Item activeItem(double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Laptop");
        when(item.getStartingPrice()).thenReturn(1000.0);
        when(item.getHighestCurrentPrice()).thenReturn(currentPrice);
        when(item.getStartTime()).thenReturn(startTime);
        when(item.getEndTime()).thenReturn(endTime);
        return item;
    }

    @Test
    void constructorShouldValidateInputsAndInitializeDefaults() {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));

        Auction auction = new Auction("auc-1", item);

        assertAll(
                () -> assertEquals("auc-1", auction.getAuctionId()),
                () -> assertSame(item, auction.getItem()),
                () -> assertEquals(AuctionStatus.UPCOMING, auction.getStatus()),
                () -> assertNull(auction.getWinnerId()),
                () -> assertEquals(1000.0, auction.getFinalPrice()),
                () -> assertNotNull(auction.getCreatedAt()),
                () -> assertNull(auction.getStartedAt()),
                () -> assertNull(auction.getEndedAt()),
                () -> assertTrue(auction.getBidHistory().isEmpty()),
                () -> assertEquals(0, auction.getBidCount())
        );

        assertThrows(IllegalArgumentException.class, () -> new Auction(null, item));
        assertThrows(IllegalArgumentException.class, () -> new Auction(" ", item));
        assertThrows(IllegalArgumentException.class, () -> new Auction("auc-2", null));
    }

    @Test
    void getBidHistoryShouldReturnDefensiveCopy() {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);
        List<BidTransaction> history = auction.getBidHistory();

        history.add(new BidTransaction("tx-1", "auc-1", "user-1", 1100.0, LocalDateTime.now()));

        assertEquals(0, auction.getBidCount());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void startAuctionShouldMoveUpcomingToOngoingWhenStartTimeReached() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        Item item = activeItem(1000.0, startTime, LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);

        auction.startAuction();

        assertAll(
                () -> assertEquals(AuctionStatus.ONGOING, auction.getStatus()),
                () -> assertNotNull(auction.getStartedAt())
        );

        assertThrows(Exception.class, auction::startAuction);
    }

    @Test
    void startAuctionShouldRejectFutureStartTime() {
        Item item = activeItem(1000.0, LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusMinutes(20));
        Auction auction = new Auction("auc-1", item);

        assertThrows(Exception.class, auction::startAuction);
        assertEquals(AuctionStatus.UPCOMING, auction.getStatus());
    }

    @Test
    void isActiveShouldReflectStatusAndTimeWindow() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));
        Auction auction = new Auction("auc-1", item);

        assertFalse(auction.isActive());
        auction.startAuction();
        assertTrue(auction.isActive());
    }

    @Test
    void addBidShouldValidateAndUpdateWinnerState() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);
        auction.startAuction();

        assertThrows(IllegalArgumentException.class, () -> auction.addBid(null));

        BidTransaction tooLow = new BidTransaction("tx-low", "auc-1", "user-1", 1000.0, LocalDateTime.now());
        assertThrows(Exception.class, () -> auction.addBid(tooLow));

        BidTransaction valid = new BidTransaction("tx-1", "auc-1", "user-1", 1200.0, LocalDateTime.now());
        auction.addBid(valid);

        assertAll(
                () -> assertEquals(1, auction.getBidCount()),
                () -> assertSame(valid, auction.getLatestBid()),
                () -> assertSame(valid, auction.getHighestBid()),
                () -> assertEquals("user-1", auction.getHighestBidderId()),
                () -> assertNull(auction.getWinnerId()),
                () -> assertEquals(1000.0, auction.getFinalPrice())
        );

        verify(item).setHighestCurrentPrice(1200.0);
    }

    @Test
    void endAuctionShouldSetWinnerFromHighestBidAndCloseAuction() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusHours(2), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);
        auction.startAuction();

        BidTransaction b1 = new BidTransaction("tx-1", "auc-1", "user-1", 1100.0, LocalDateTime.now().minusMinutes(30));
        BidTransaction b2 = new BidTransaction("tx-2", "auc-1", "user-2", 1300.0, LocalDateTime.now().minusMinutes(20));
        BidTransaction b3 = new BidTransaction("tx-3", "auc-1", "user-3", 1250.0, LocalDateTime.now().minusMinutes(10));
        auction.addBid(b1);
        auction.addBid(b2);
        auction.addBid(b3);

        when(item.getEndTime()).thenReturn(LocalDateTime.now().minusMinutes(1));

        auction.endAuction();

        assertAll(
                () -> assertEquals(AuctionStatus.ENDED, auction.getStatus()),
                () -> assertNotNull(auction.getEndedAt()),
                () -> assertEquals("user-2", auction.getWinnerId()),
                () -> assertEquals(1300.0, auction.getFinalPrice())
        );
    }

    @Test
    void endAuctionShouldResetWinnerWhenNoBids() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusHours(2), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);
        auction.startAuction();

        when(item.getEndTime()).thenReturn(LocalDateTime.now().minusMinutes(1));

        auction.endAuction();

        assertAll(
                () -> assertEquals(AuctionStatus.ENDED, auction.getStatus()),
                () -> assertNull(auction.getWinnerId()),
                () -> assertEquals(1000.0, auction.getFinalPrice())
        );
    }

    @Test
    void endAuctionShouldRejectWrongStatusOrPrematureEnd() {
        Item upcomingItem = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Auction upcomingAuction = new Auction("auc-1", upcomingItem);

        assertThrows(Exception.class, upcomingAuction::endAuction);

        Item futureItem = activeItem(1000.0, LocalDateTime.now().minusHours(2), LocalDateTime.now().plusMinutes(10));
        Auction futureAuction = new Auction("auc-2", futureItem);

        assertThrows(Exception.class, futureAuction::endAuction);
    }

    @Test
    void cancelAuctionShouldValidateReasonAndChangeStatus() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);

        assertThrows(IllegalArgumentException.class, () -> auction.cancelAuction(null));
        assertThrows(IllegalArgumentException.class, () -> auction.cancelAuction("   "));

        auction.cancelAuction("duplicate item");
        assertEquals(AuctionStatus.BANNED, auction.getStatus());
    }

    @Test
    void markAsPaidShouldOnlyWorkWhenEnded() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusHours(2), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);

        assertThrows(Exception.class, auction::markAsPaid);

        auction.startAuction();
        when(item.getEndTime()).thenReturn(LocalDateTime.now().minusMinutes(1));
        auction.endAuction();

        assertDoesNotThrow(auction::markAsPaid);
    }

    @Test
    void shouldExposeLatestAndHighestBidCorrectly() throws Exception {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);
        auction.startAuction();

        assertNull(auction.getLatestBid());
        assertNull(auction.getHighestBid());
        assertNull(auction.getHighestBidderId());

        BidTransaction b1 = new BidTransaction("tx-1", "auc-1", "user-1", 1200.0, LocalDateTime.now().minusMinutes(2));
        BidTransaction b2 = new BidTransaction("tx-2", "auc-1", "user-2", 1500.0, LocalDateTime.now().minusMinutes(1));
        auction.addBid(b1);
        auction.addBid(b2);

        assertAll(
                () -> assertSame(b2, auction.getLatestBid()),
                () -> assertSame(b2, auction.getHighestBid()),
                () -> assertEquals("user-2", auction.getHighestBidderId())
        );
    }

    @Test
    void toStringShouldContainMainFields() {
        Item item = activeItem(1000.0, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        Auction auction = new Auction("auc-1", item);

        String text = auction.toString();

        assertAll(
                () -> assertTrue(text.contains("auc-1")),
                () -> assertTrue(text.contains("Laptop")),
                () -> assertTrue(text.contains("UPCOMING")),
                () -> assertTrue(text.contains("bids=0"))
        );
    }
}
