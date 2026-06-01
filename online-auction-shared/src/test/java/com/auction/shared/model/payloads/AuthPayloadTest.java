package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthPayloadTest {

    @Test
    void shouldSupportConstructorsAndSetters() {
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
}

