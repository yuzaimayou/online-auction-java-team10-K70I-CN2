package com.auction.shared.model.account;

import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserTest {

    @Test
    void shouldInitializeDefaultState() {
        User user = AccountTestSupport.user("u1", "alice", "123");

        assertAll(
                () -> assertEquals("u1", user.getId()),
                () -> assertEquals("alice", user.getUsername()),
                () -> assertEquals("123", user.getPassword()),
                () -> assertEquals(0.0, user.getBalance()),
                () -> assertEquals(0.0, user.getFrozenBalance()),
                () -> assertEquals(0.0, user.getAvailableBalance()),
                () -> assertEquals(5.0, user.getRating()),
                () -> assertNull(user.getEmail()),
                () -> assertTrue(user.isVerify()),
                () -> assertEquals("User", user.getRole())
        );
    }

    @Test
    void shouldRejectInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> AccountTestSupport.user(null, "alice", "123"));
        assertThrows(IllegalArgumentException.class, () -> AccountTestSupport.user("u1", "", "123"));
        assertThrows(IllegalArgumentException.class, () -> AccountTestSupport.user("u1", "alice", null));
        assertThrows(IllegalArgumentException.class, () -> AccountTestSupport.user("u1", "alice", "   "));
    }

    @Test
    void shouldUpdateBasicFieldsAndValidateInputs() {
        User user = AccountTestSupport.user("u2", "alice", "123");

        user.setUsername("alice2");
        user.setPassword("456");
        user.setRole("CUSTOM_ROLE");

        assertAll(
                () -> assertEquals("alice2", user.getUsername()),
                () -> assertEquals("456", user.getPassword()),
                () -> assertEquals("CUSTOM_ROLE", user.getRole())
        );

        assertThrows(IllegalArgumentException.class, () -> user.setUsername(""));
        assertThrows(IllegalArgumentException.class, () -> user.setPassword(null));
        assertThrows(IllegalArgumentException.class, () -> user.setRole(" "));
    }

    @Test
    void shouldManageBalancesAndAvailability() {
        User user = AccountTestSupport.user("u3", "alice", "123");

        user.setBalance(1000.0);
        user.setFrozenBalance(250.0);

        assertAll(
                () -> assertEquals(1000.0, user.getBalance()),
                () -> assertEquals(250.0, user.getFrozenBalance()),
                () -> assertEquals(750.0, user.getAvailableBalance())
        );

        assertThrows(IllegalArgumentException.class, () -> user.setBalance(-1));
        assertThrows(IllegalArgumentException.class, () -> user.setFrozenBalance(-1));
    }

    @Test
    void shouldValidateRatingEmailAndVerifyFlag() {
        User user = AccountTestSupport.user("u4", "alice", "123");

        user.setRating(4.5);
        user.setEmail("  alice@example.com  ");
        user.setEnable(false);

        assertAll(
                () -> assertEquals(4.5, user.getRating()),
                () -> assertEquals("alice@example.com", user.getEmail()),
                () -> assertFalse(user.isVerify())
        );

        assertThrows(IllegalArgumentException.class, () -> user.setRating(0.9));
        assertThrows(IllegalArgumentException.class, () -> user.setRating(5.1));
    }

    @Test
    void shouldRejectInvalidBidStates() {
        User user = AccountTestSupport.user("u5", "alice", "123");
        user.setBalance(1000.0);

        Item item = mock(Item.class);
        when(item.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(10));
        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(item.isOwner("u5")).thenReturn(false);
        when(item.getHighestCurrentPrice()).thenReturn(100.0);

        assertThrows(IllegalArgumentException.class, () -> user.placeBid(null, 200.0));
        assertThrows(IllegalArgumentException.class, () -> user.placeBid(item, 0));

        when(item.getStartTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));

        when(item.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(10));
        when(item.getEndTime()).thenReturn(LocalDateTime.now().minusMinutes(1));
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));

        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(item.isOwner("u5")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));

        when(item.isOwner("u5")).thenReturn(false);
        when(item.getHighestCurrentPrice()).thenReturn(250.0);
        assertThrows(IllegalArgumentException.class, () -> user.placeBid(item, 200.0));

        user.setBalance(150.0);
        when(item.getHighestCurrentPrice()).thenReturn(100.0);
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));
    }

    @Test
    void shouldPlaceBidWhenValid() {
        User user = AccountTestSupport.user("u6", "alice", "123");
        user.setBalance(1000.0);

        Item item = AccountTestSupport.activeItemFor("u6", 100.0);
        when(item.getName()).thenReturn("Laptop");

        assertDoesNotThrow(() -> user.placeBid(item, 250.0));
        verify(item).setHighestCurrentPrice(250.0);
    }

    @Test
    void shouldValidateCreateItem() {
        User user = AccountTestSupport.user("u7", "alice", "123");

        assertThrows(IllegalArgumentException.class, () -> user.createItem(null));
        assertThrows(IllegalArgumentException.class, () -> user.createItem("   "));

        user.setRating(1.5);
        assertThrows(IllegalStateException.class, () -> user.createItem("Camera"));

        user.setRating(2.0);
        assertDoesNotThrow(() -> user.createItem("Camera"));
    }
}
