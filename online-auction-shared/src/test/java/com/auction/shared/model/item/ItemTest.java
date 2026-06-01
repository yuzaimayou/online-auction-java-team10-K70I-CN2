package com.auction.shared.model.item;

import com.auction.shared.model.enums.AuctionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Item entity.
 * Tests constructor validation, status computation, and business logic.
 */
class ItemTest {

    private static final List<String> VALID_IMAGES = Arrays.asList("image1.jpg", "image2.jpg");
    private static final LocalDateTime FUTURE_START = LocalDateTime.now().plusHours(2);
    private static final LocalDateTime FUTURE_END = LocalDateTime.now().plusHours(3);

    @Test
    void constructorShouldInitializeItemWithValidParameters() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(2);

        Item item = new Item(
                "Test Laptop",
                "A high-performance laptop",
                1000.0,
                startTime,
                endTime,
                "seller123",
                "Electronics",
                50.0,
                VALID_IMAGES
        );

        assertAll(
                () -> assertEquals("Test Laptop", item.getName()),
                () -> assertEquals("A high-performance laptop", item.getDescription()),
                () -> assertEquals(1000.0, item.getStartingPrice()),
                () -> assertEquals(1000.0, item.getCurrentPrice()),
                () -> assertEquals("seller123", item.getSellerId()),
                () -> assertEquals("Electronics", item.getCategory()),
                () -> assertEquals(50.0, item.getBidStep()),
                () -> assertEquals(VALID_IMAGES, item.getImagesPath()),
                () -> assertNotNull(item.getId()),
                () -> assertNotNull(item.getCreate_at())
        );
    }

    @Test
    void constructorShouldRejectNullOrEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Item(null, "description", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Item("   ", "description", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectNullDescription() {
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Valid Name", null, 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectNegativeStartingPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", -100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectNullOrEmptyTimes() {
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, null, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, null, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectPastStartTime() {
        LocalDateTime pastStart = LocalDateTime.now().minusHours(1);
        LocalDateTime futureEnd = LocalDateTime.now().plusHours(2);

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, pastStart, futureEnd, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectPastEndTime() {
        LocalDateTime futureStart = LocalDateTime.now().plusHours(1);
        LocalDateTime pastEnd = LocalDateTime.now().minusHours(1);

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, futureStart, pastEnd, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectEndTimeBeforeStartTime() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, start, end, "seller", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectNullOrEmptySellerId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, null, "cat", 10.0, VALID_IMAGES)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, "   ", "cat", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectNullOrEmptyCategory() {
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", null, 10.0, VALID_IMAGES)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "   ", 10.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectInvalidBidStep() {
        // Bid step <= 0
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 0.0, VALID_IMAGES)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", -10.0, VALID_IMAGES)
        );

        // Bid step > starting price
        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 150.0, VALID_IMAGES)
        );
    }

    @Test
    void constructorShouldRejectNullOrEmptyImages() {
        LocalDateTime start = FUTURE_START;
        LocalDateTime end = FUTURE_END;

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, start, end, "seller", "cat", 10.0, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Name", "desc", 100.0, start, end, "seller", "cat", 10.0, new ArrayList<>())
        );
    }

    @Test
    void statusShouldBeUpcomingWhenBeforeStartTime() {
        LocalDateTime startTime = LocalDateTime.now().plusMinutes(30);
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(90);

        Item item = new Item(
                "Name", "desc", 100.0, startTime, endTime, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertEquals(AuctionStatus.UPCOMING, item.getStatus());
    }

    @Test
    void statusShouldBeOngoingDuringAuctionTime() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(20);

        Item item = new Item(
                "Name", "desc", 100.0, 100.0, startTime, endTime, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertEquals(AuctionStatus.ONGOING, item.getStatus());
    }

    @Test
    void statusShouldBeEndedWhenAfterEndTime() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(30);
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(1);

        Item item = new Item(
                "Name", "desc", 100.0, 100.0, startTime, endTime, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertEquals(AuctionStatus.ENDED, item.getStatus());
    }

    @Test
    void statusShouldBeBannedWhenStoredStatusIsBanned() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(10);

        Item item = new Item(
                "Name", "desc", 100.0, 100.0, startTime, endTime, "seller", "cat", 10.0, VALID_IMAGES
        );

        item.setStatus(AuctionStatus.BANNED);

        assertEquals(AuctionStatus.BANNED, item.getStatus());
    }

    @Test
    void setCurrentPriceShouldRejectBelowStartingPrice() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertThrows(IllegalArgumentException.class, () -> item.setCurrentPrice(50.0));
    }

    @Test
    void setCurrentPriceShouldAcceptValidPrice() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        item.setCurrentPrice(150.0);
        assertEquals(150.0, item.getCurrentPrice());
    }

    @Test
    void setHighestCurrentPriceShouldUpdatePrice() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        item.setHighestCurrentPrice(200.0);
        assertEquals(200.0, item.getHighestCurrentPrice());
    }

    @Test
    void getHighestCurrentPriceShouldReturnCurrentPrice() {
        Item item = new Item(
                "Name", "desc", 100.0, 150.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertEquals(150.0, item.getHighestCurrentPrice());
    }

    @Test
    void isOwnerShouldReturnTrueForSeller() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller123", "cat", 10.0, VALID_IMAGES
        );

        assertTrue(item.isOwner("seller123"));
    }

    @Test
    void isOwnerShouldReturnFalseForNonSeller() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller123", "cat", 10.0, VALID_IMAGES
        );

        assertFalse(item.isOwner("other_user"));
    }

    @Test
    void isOwnerShouldReturnFalseForNullSellerId() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller123", "cat", 10.0, VALID_IMAGES
        );


        assertFalse(item.isOwner(null));
    }

    @Test
    void setEndTimeShouldRejectNull() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertThrows(IllegalArgumentException.class, () -> item.setEndTime(null));
    }

    @Test
    void setEndTimeShouldUpdateTime() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        LocalDateTime newEndTime = FUTURE_END.plusHours(1);
        item.setEndTime(newEndTime);

        assertEquals(newEndTime, item.getEndTime());
    }

    @Test
    void secondConstructorShouldInitializeWithCurrentPrice() {
        Item item = new Item(
                "Name", "desc", 100.0, 150.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertEquals(100.0, item.getStartingPrice());
        assertEquals(150.0, item.getCurrentPrice());
    }

    @Test
    void topPlayerManagementMethods() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertNull(item.getCurrentBidderId());

        item.setCurrentBidderId("player1");
        assertEquals("player1", item.getCurrentBidderId());
    }

    @Test
    void myLastBidManagementMethods() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        assertEquals(0.0, item.getMyLastBid());

        item.setMyLastBid(250.0);
        assertEquals(250.0, item.getMyLastBid());
    }

    @Test
    void storedStatusShouldReturnInternalStatus() {
        Item item = new Item(
                "Name", "desc", 100.0, FUTURE_START, FUTURE_END, "seller", "cat", 10.0, VALID_IMAGES
        );

        item.setStatus(AuctionStatus.BANNED);
        assertEquals(AuctionStatus.BANNED, item.getStoredStatus());
    }
}
