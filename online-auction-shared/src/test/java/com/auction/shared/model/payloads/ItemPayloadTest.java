package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemPayloadTest {

    @Test
    void shouldKeepConstructorValues() {
        ItemPayload payload = PayloadTestSupport.itemPayload();

        assertAll(
                () -> assertEquals("Laptop", payload.getItemName()),
                () -> assertEquals("Electronics", payload.getCategory()),
                () -> assertEquals("Gaming laptop", payload.getItemDesc()),
                () -> assertNotNull(payload.getImagesConverted()),
                () -> assertEquals(1, payload.getImagesConverted().size()),
                () -> assertEquals("cover.png", payload.getImagesConverted().get(0)[0]),
                () -> assertEquals("image/png", payload.getImagesConverted().get(0)[1]),
                () -> assertNotNull(payload.getStartDateTime()),
                () -> assertNotNull(payload.getEndDateTime()),
                () -> assertEquals(1000.0, payload.getInitPrice()),
                () -> assertEquals(50.0, payload.getBidStep()),
                () -> assertEquals("seller-1", payload.getUserId())
        );
    }
}

