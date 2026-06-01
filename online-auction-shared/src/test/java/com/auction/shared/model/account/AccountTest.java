package com.auction.shared.model.account;

import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountTest {

    @Test
    void userShouldInitializeDefaultState() {
        User user = new User("u1", "alice", "123");

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
    void userExtendedConstructorShouldSetEmailAndVerificationFlag() {
        User user = new User("u2", "bob", "pass", " bob@mail.com ", false);

        assertAll(
                () -> assertEquals(" bob@mail.com ", user.getEmail()),
                () -> assertFalse(user.isVerify()),
                () -> assertEquals("User", user.getRole())
        );
    }

    @Test
    void userShouldValidateConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new User(null, "alice", "123"));
        assertThrows(IllegalArgumentException.class, () -> new User("u1", "", "123"));
        assertThrows(IllegalArgumentException.class, () -> new User("u1", "alice", null));
        assertThrows(IllegalArgumentException.class, () -> new User("u1", "alice", "   "));
    }

    @Test
    void personSettersShouldValidateAndUpdateValues() {
        User user = new User("u3", "alice", "123");

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
    void userFinancialFieldsShouldValidateAndComputeAvailableBalance() {
        User user = new User("u4", "alice", "123");

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
    void userShouldValidateRatingAndEmailAndVerifyFlag() {
        User user = new User("u5", "alice", "123");

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
    void placeBidShouldRejectInvalidInputsAndStates() {
        User user = new User("u6", "alice", "123");
        user.setBalance(1000.0);

        Item item = mock(Item.class);
        when(item.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(10));
        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(item.isOwner("u6")).thenReturn(false);
        when(item.getHighestCurrentPrice()).thenReturn(100.0);

        assertThrows(IllegalArgumentException.class, () -> user.placeBid(null, 200.0));
        assertThrows(IllegalArgumentException.class, () -> user.placeBid(item, 0));

        when(item.getStartTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));

        when(item.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(10));
        when(item.getEndTime()).thenReturn(LocalDateTime.now().minusMinutes(1));
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));

        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(item.isOwner("u6")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));

        when(item.isOwner("u6")).thenReturn(false);
        when(item.getHighestCurrentPrice()).thenReturn(250.0);
        assertThrows(IllegalArgumentException.class, () -> user.placeBid(item, 200.0));

        user.setBalance(150.0);
        when(item.getHighestCurrentPrice()).thenReturn(100.0);
        assertThrows(IllegalStateException.class, () -> user.placeBid(item, 200.0));
    }

    @Test
    void placeBidShouldUpdateHighestPriceWhenValid() {
        User user = new User("u7", "alice", "123");
        user.setBalance(1000.0);

        Item item = mock(Item.class);
        when(item.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(10));
        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(item.isOwner("u7")).thenReturn(false);
        when(item.getHighestCurrentPrice()).thenReturn(100.0);
        when(item.getName()).thenReturn("Laptop");

        assertDoesNotThrow(() -> user.placeBid(item, 250.0));
        verify(item).setHighestCurrentPrice(250.0);
    }

    @Test
    void createItemShouldValidateNameAndRating() {
        User user = new User("u8", "alice", "123");

        assertThrows(IllegalArgumentException.class, () -> user.createItem(null));
        assertThrows(IllegalArgumentException.class, () -> user.createItem("   "));

        user.setRating(1.5);
        assertThrows(IllegalStateException.class, () -> user.createItem("Camera"));

        user.setRating(2.0);
        assertDoesNotThrow(() -> user.createItem("Camera"));
    }

    @Test
    void adminShouldInitializeDefaultRoleAndAllowAdminActions() {
        Admin admin = new Admin("a1", "root", "secret");

        assertAll(
                () -> assertEquals("a1", admin.getId()),
                () -> assertEquals("root", admin.getUsername()),
                () -> assertEquals("secret", admin.getPassword()),
                () -> assertEquals("Admin", admin.getRole()),
                () -> assertEquals(0.0, admin.getBalance())
        );

        assertDoesNotThrow(() -> admin.deleteUser("u1"));
        assertDoesNotThrow(() -> admin.approveAuction("au1"));
        assertDoesNotThrow(() -> admin.suspendUser("u2"));

        assertThrows(IllegalArgumentException.class, () -> admin.deleteUser(null));
        assertThrows(IllegalArgumentException.class, () -> admin.approveAuction(""));
        assertThrows(IllegalArgumentException.class, () -> admin.suspendUser("   "));
    }

    @Test
    void adminShouldRejectSelfDeletionAndSelfSuspension() {
        Admin admin = new Admin("a2", "root", "secret");

        assertThrows(IllegalStateException.class, () -> admin.deleteUser("a2"));
        assertThrows(IllegalStateException.class, () -> admin.suspendUser("a2"));
    }

    @Test
    void adminExtendedConstructorShouldSetEmailAndVerifyFlag() {
        Admin admin = new Admin("a3", "root", "secret", " admin@mail.com ", false);

        assertAll(
                () -> assertEquals(" admin@mail.com ", admin.getEmail()),
                () -> assertFalse(admin.isVerify()),
                () -> assertEquals("Admin", admin.getRole())
        );
    }
}
