package com.auction.shared.model.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseDTOTest {

    @Test
    void shouldInitializeEmptyObjectWithDefaultValues() {
        UserResponseDTO dto = new UserResponseDTO();

        assertAll(
                () -> assertNull(dto.getId()),
                () -> assertNull(dto.getUsername()),
                () -> assertNull(dto.getEmail()),
                () -> assertNull(dto.getRole()),
                () -> assertEquals(0.0, dto.getBalance()),
                () -> assertEquals(0.0, dto.getFrozenBalance()),
                () -> assertEquals(0.0, dto.getRating()),
                () -> assertFalse(dto.isVerify())
        );
    }

    @Test
    void shouldSupportConstructorAndSetters() {
        UserResponseDTO dto = new UserResponseDTO(
                "u1",
                "alice",
                "alice@example.com",
                "User",
                1000.0,
                250.0,
                4.7,
                true
        );

        assertAll(
                () -> assertEquals("u1", dto.getId()),
                () -> assertEquals("alice", dto.getUsername()),
                () -> assertEquals("alice@example.com", dto.getEmail()),
                () -> assertEquals("User", dto.getRole()),
                () -> assertEquals(1000.0, dto.getBalance()),
                () -> assertEquals(250.0, dto.getFrozenBalance()),
                () -> assertEquals(4.7, dto.getRating()),
                () -> assertTrue(dto.isVerify())
        );

        dto.setId("u2");
        dto.setUsername("bob");
        dto.setEmail("bob@example.com");
        dto.setRole("Admin");
        dto.setBalance(2000.0);
        dto.setFrozenBalance(300.0);
        dto.setRating(5.0);
        dto.setVerify(false);

        assertAll(
                () -> assertEquals("u2", dto.getId()),
                () -> assertEquals("bob", dto.getUsername()),
                () -> assertEquals("bob@example.com", dto.getEmail()),
                () -> assertEquals("Admin", dto.getRole()),
                () -> assertEquals(2000.0, dto.getBalance()),
                () -> assertEquals(300.0, dto.getFrozenBalance()),
                () -> assertEquals(5.0, dto.getRating()),
                () -> assertFalse(dto.isVerify())
        );
    }
}

