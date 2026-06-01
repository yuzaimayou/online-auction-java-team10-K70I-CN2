package com.auction.shared.model.payloads;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayloadsTest {

    private final Gson gson = new Gson();

    @Test
    void authPayloadShouldSupportConstructorsAndSetters() {
        AuthPayload empty = new AuthPayload();
        assertAll(
                () -> assertNull(empty.getUsername()),
                () -> assertNull(empty.getPassword()),
                () -> assertNull(empty.getEmail())
        );

        AuthPayload payload = new AuthPayload("user1", "pass1");
        assertAll(
                () -> assertEquals("user1", payload.getUsername()),
                () -> assertEquals("pass1", payload.getPassword()),
                () -> assertNull(payload.getEmail())
        );

        payload.setUsername("user2");
        payload.setPassword("pass2");
        payload.setEmail("user2@example.com");

        assertAll(
                () -> assertEquals("user2", payload.getUsername()),
                () -> assertEquals("pass2", payload.getPassword()),
                () -> assertEquals("user2@example.com", payload.getEmail())
        );
    }

    @Test
    void verifyPayloadShouldSupportConstructorsAndSetters() {
        VerifyPayload empty = new VerifyPayload();
        assertAll(
                () -> assertNull(empty.getEmail()),
                () -> assertNull(empty.getOtp())
        );

        VerifyPayload payload = new VerifyPayload("a@b.com", "123456");
        assertAll(
                () -> assertEquals("a@b.com", payload.getEmail()),
                () -> assertEquals("123456", payload.getOtp())
        );

        payload.setEmail("c@d.com");
        payload.setOtp("999999");

        assertAll(
                () -> assertEquals("c@d.com", payload.getEmail()),
                () -> assertEquals("999999", payload.getOtp())
        );
    }

    @Test
    void roomPayloadShouldSupportConstructorsAndSetters() {
        RoomPayload empty = new RoomPayload();
        assertAll(
                () -> assertNull(empty.getItemId()),
                () -> assertNull(empty.getToken())
        );

        RoomPayload payload = new RoomPayload("item-1", "token-1");
        assertAll(
                () -> assertEquals("item-1", payload.getItemId()),
                () -> assertEquals("token-1", payload.getToken())
        );

        payload.setItemId("item-2");
        payload.setToken("token-2");

        assertAll(
                () -> assertEquals("item-2", payload.getItemId()),
                () -> assertEquals("token-2", payload.getToken())
        );
    }

    @Test
    void auctionExtendedPayloadShouldSupportConstructorsAndSetters() {
        AuctionExtendedPayload empty = new AuctionExtendedPayload();
        assertAll(
                () -> assertNull(empty.getItemId()),
                () -> assertNull(empty.getNewEndTime())
        );

        AuctionExtendedPayload payload = new AuctionExtendedPayload("item-1", "2026-05-31T10:15:30");
        assertAll(
                () -> assertEquals("item-1", payload.getItemId()),
                () -> assertEquals("2026-05-31T10:15:30", payload.getNewEndTime())
        );

        payload.setItemId("item-2");
        payload.setNewEndTime("2026-06-01T12:00:00");

        assertAll(
                () -> assertEquals("item-2", payload.getItemId()),
                () -> assertEquals("2026-06-01T12:00:00", payload.getNewEndTime())
        );
    }

    @Test
    void autoBidPayloadShouldSupportBothConstructors() {
        AutoBidPayload empty = new AutoBidPayload();
        assertAll(
                () -> assertNull(empty.getItemId()),
                () -> assertNull(empty.getUserId()),
                () -> assertNull(empty.getMaxBid()),
                () -> assertNull(empty.getIncrement()),
                () -> assertNull(empty.getIsActive())
        );

        AutoBidPayload inactive = new AutoBidPayload("item-1", "user-1", 2000.0, 50.0);
        assertAll(
                () -> assertEquals("item-1", inactive.getItemId()),
                () -> assertEquals("user-1", inactive.getUserId()),
                () -> assertEquals(2000.0, inactive.getMaxBid()),
                () -> assertEquals(50.0, inactive.getIncrement()),
                () -> assertNull(inactive.getIsActive())
        );

        AutoBidPayload active = new AutoBidPayload("item-2", "user-2", 5000.0, 100.0, true);
        assertAll(
                () -> assertEquals("item-2", active.getItemId()),
                () -> assertEquals("user-2", active.getUserId()),
                () -> assertEquals(5000.0, active.getMaxBid()),
                () -> assertEquals(100.0, active.getIncrement()),
                () -> assertTrue(active.getIsActive())
        );
    }

    @Test
    void itemPayloadShouldKeepConstructorValues() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 31, 10, 0);
        LocalDateTime end = start.plusHours(2);
        List<String[]> images = List.<String[]>of(new String[]{"cover.png", "image/png"});

        ItemPayload payload = new ItemPayload(
                "Laptop",
                "Electronics",
                "Gaming laptop",
                images,
                start,
                end,
                1000.0,
                50.0,
                "seller-1"
        );

        assertAll(
                () -> assertEquals("Laptop", payload.getItemName()),
                () -> assertEquals("Electronics", payload.getCategory()),
                () -> assertEquals("Gaming laptop", payload.getItemDesc()),
                () -> assertSame(images, payload.getImagesConverted()),
                () -> assertEquals(start, payload.getStartDateTime()),
                () -> assertEquals(end, payload.getEndDateTime()),
                () -> assertEquals(1000.0, payload.getInitPrice()),
                () -> assertEquals(50.0, payload.getBidStep()),
                () -> assertEquals("seller-1", payload.getUserId())
        );
    }

    @Test
    void bidPayloadShouldExposeImmutableValues() {
        BidPayload empty = new BidPayload();
        assertAll(
                () -> assertNull(empty.getItemId()),
                () -> assertNull(empty.getUserId()),
                () -> assertNull(empty.getBidPrice()),
                () -> assertNull(empty.getBidTime())
        );

        BidPayload payload = new BidPayload("item-1", "user-1", 1234.5, "2026-05-31T10:10:10");
        assertAll(
                () -> assertEquals("item-1", payload.getItemId()),
                () -> assertEquals("user-1", payload.getUserId()),
                () -> assertEquals(1234.5, payload.getBidPrice()),
                () -> assertEquals("2026-05-31T10:10:10", payload.getBidTime())
        );
    }

    @Test
    void bidPayloadShouldNotDeclareSetters() {
        for (Method method : BidPayload.class.getDeclaredMethods()) {
            assertFalse(method.getName().startsWith("set"), "BidPayload must remain immutable");
        }
    }

    @Test
    void payloadsShouldRoundTripThroughGson() {
        AuthPayload auth = new AuthPayload("user", "secret", "user@example.com");
        RoomPayload room = new RoomPayload("item-1", "token-abc");

        AuthPayload authCopy = gson.fromJson(gson.toJson(auth), AuthPayload.class);
        RoomPayload roomCopy = gson.fromJson(gson.toJson(room), RoomPayload.class);

        assertAll(
                () -> assertEquals(auth.getUsername(), authCopy.getUsername()),
                () -> assertEquals(auth.getPassword(), authCopy.getPassword()),
                () -> assertEquals(auth.getEmail(), authCopy.getEmail()),
                () -> assertEquals(room.getItemId(), roomCopy.getItemId()),
                () -> assertEquals(room.getToken(), roomCopy.getToken())
        );
    }
}

