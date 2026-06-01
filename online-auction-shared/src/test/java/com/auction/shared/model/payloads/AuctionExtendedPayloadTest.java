package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuctionExtendedPayloadTest {

    @Test
    void shouldSupportConstructorsAndSetters() {
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
}

