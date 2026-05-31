package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidPayloadTest {

    @Test
    void shouldSupportBothConstructors() {
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
}

