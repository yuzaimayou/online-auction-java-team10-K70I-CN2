package com.auction.shared.model.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOTest {

    @Test
    void shouldExposeConstructorValuesAndPublicFields() {
        UserDTO dto = new UserDTO(
                "u1",
                "alice",
                "alice@example.com",
                "User",
                true,
                1000.0,
                250.0
        );

        assertAll(
                () -> assertEquals("u1", dto.getId()),
                () -> assertEquals("alice", dto.getUsername()),
                () -> assertEquals("alice@example.com", dto.getEmail()),
                () -> assertEquals("User", dto.getRole()),
                () -> assertTrue(dto.isVerify()),
                () -> assertEquals(1000.0, dto.getBalance()),
                () -> assertEquals(250.0, dto.getFrozenBalance()),
                () -> assertEquals("u1", dto.id),
                () -> assertEquals("alice", dto.username),
                () -> assertEquals("alice@example.com", dto.email),
                () -> assertEquals("User", dto.role),
                () -> assertTrue(dto.isVerify),
                () -> assertEquals(1000.0, dto.balance),
                () -> assertEquals(250.0, dto.frozenBalance)
        );
    }

    @Test
    void shouldAllowPublicFieldMutation() {
        UserDTO dto = new UserDTO("u1", "alice", "alice@example.com", "User", true, 1000.0, 250.0);

        dto.id = "u2";
        dto.username = "bob";
        dto.email = "bob@example.com";
        dto.role = "Admin";
        dto.isVerify = false;
        dto.balance = 2000.0;
        dto.frozenBalance = 300.0;

        assertAll(
                () -> assertEquals("u2", dto.getId()),
                () -> assertEquals("bob", dto.getUsername()),
                () -> assertEquals("bob@example.com", dto.getEmail()),
                () -> assertEquals("Admin", dto.getRole()),
                () -> assertFalse(dto.isVerify()),
                () -> assertEquals(2000.0, dto.getBalance()),
                () -> assertEquals(300.0, dto.getFrozenBalance())
        );
    }
}

