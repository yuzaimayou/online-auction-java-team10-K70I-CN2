package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void login_success_when_password_match() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verify = mock(VerifyService.class);

        User user = new User("u1", "alice", "123");

        when(repo.findByUsername("alice")).thenReturn(user);

        AuthService authService = new AuthService(repo, verify);

        User result = authService.login("alice", "123");
        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }

    @Test
    void login_fail_when_wrong_password() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verify = mock(VerifyService.class);

        User user = new User("u2", "alice", "123");

        when(repo.findByUsername("alice")).thenReturn(user);

        AuthService authService = new AuthService(repo, verify);

        User result = authService.login("alice", "wrong");
        assertNull(result);
    }

    @Test
    void register_fail_when_username_exists() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verify = mock(VerifyService.class);

        User existing = new User("u3", "bob", "pass");
        when(repo.findByUsername("bob")).thenReturn(existing);

        AuthService authService = new AuthService(repo, verify);

        boolean created = authService.register("bob", "pass", "bob@mail.com");
        assertFalse(created);
        verify(repo, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void register_success_when_username_not_exists() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verify = mock(VerifyService.class);

        when(repo.findByUsername("bob")).thenReturn(null);
        when(repo.createUser("bob", "pass", "USER", "bob@mail.com")).thenReturn(true);

        AuthService authService = new AuthService(repo, verify);

        boolean created = authService.register("bob", "pass", "bob@mail.com");
        assertTrue(created);
        verify(repo).createUser("bob", "pass", "USER", "bob@mail.com");
    }
}