package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyPayloadTest {

    @Test
    void shouldSupportConstructorsAndSetters() {
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
}

