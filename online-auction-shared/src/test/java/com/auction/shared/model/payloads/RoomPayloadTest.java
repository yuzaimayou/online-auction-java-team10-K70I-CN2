package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomPayloadTest {

    @Test
    void shouldSupportConstructorsAndSetters() {
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
}

