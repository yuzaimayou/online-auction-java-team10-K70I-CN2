package com.auction.shared.model.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    @Test
    void shouldInitializeDefaultAdminState() {
        Admin admin = AccountTestSupport.admin("a1", "root", "secret");

        assertAll(
                () -> assertEquals("a1", admin.getId()),
                () -> assertEquals("root", admin.getUsername()),
                () -> assertEquals("secret", admin.getPassword()),
                () -> assertEquals("Admin", admin.getRole()),
                () -> assertEquals(0.0, admin.getBalance()),
                () -> assertTrue(admin.isVerify())
        );
    }

    @Test
    void shouldSupportExtendedConstructor() {
        Admin admin = new Admin("a2", "root", "secret", " admin@mail.com ", false);

        assertAll(
                () -> assertEquals(" admin@mail.com ", admin.getEmail()),
                () -> assertFalse(admin.isVerify()),
                () -> assertEquals("Admin", admin.getRole())
        );
    }

    @Test
    void shouldAllowAdminActionsWithValidInputs() {
        Admin admin = AccountTestSupport.admin("a3", "root", "secret");

        assertDoesNotThrow(() -> admin.deleteUser("u1"));
        assertDoesNotThrow(() -> admin.approveAuction("au1"));
        assertDoesNotThrow(() -> admin.suspendUser("u2"));
    }

    @Test
    void shouldRejectInvalidAdminActionInputs() {
        Admin admin = AccountTestSupport.admin("a4", "root", "secret");

        assertThrows(IllegalArgumentException.class, () -> admin.deleteUser(null));
        assertThrows(IllegalArgumentException.class, () -> admin.approveAuction(""));
        assertThrows(IllegalArgumentException.class, () -> admin.suspendUser("   "));
    }

    @Test
    void shouldRejectSelfDeletionAndSuspension() {
        Admin admin = AccountTestSupport.admin("a5", "root", "secret");

        assertThrows(IllegalStateException.class, () -> admin.deleteUser("a5"));
        assertThrows(IllegalStateException.class, () -> admin.suspendUser("a5"));
    }
}
