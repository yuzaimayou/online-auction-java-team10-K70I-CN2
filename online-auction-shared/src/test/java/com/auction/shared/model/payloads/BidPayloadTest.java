package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class BidPayloadTest {

    @Test
    void shouldExposeImmutableValues() {
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
    void shouldNotDeclareSetters() {
        for (Method method : BidPayload.class.getDeclaredMethods()) {
            assertFalse(method.getName().startsWith("set"), "BidPayload must remain immutable");
        }
    }
}

