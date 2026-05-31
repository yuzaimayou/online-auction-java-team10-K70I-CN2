package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void login_success_when_password_matches() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verifyService = mock(VerifyService.class);

        User user = new User("u1", "alice", "pass");
        when(repo.findByUsername("alice")).thenReturn(user);

        AuthService authService = createAuthService(repo, verifyService);

        User result = authService.login("alice", "pass");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }

    @Test
    void login_returns_null_when_password_invalid() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verifyService = mock(VerifyService.class);

        User user = new User("u2", "alice", "pass");
        when(repo.findByUsername("alice")).thenReturn(user);

        AuthService authService = createAuthService(repo, verifyService);

        User result = authService.login("alice", "wrong");

        assertNull(result);
    }

    @Test
    void register_returns_username_exists_when_duplicate_username() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verifyService = mock(VerifyService.class);

        when(repo.findByUsername("alice")).thenReturn(new User("u3", "alice", "pass"));

        AuthService authService = createAuthService(repo, verifyService);

        AuthService.RegisterResult result = authService.register("alice", "pass", "alice@example.com");

        assertEquals(AuthService.RegisterResult.USERNAME_EXISTS, result);
        verify(repo, never()).createUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void register_returns_email_exists_when_duplicate_email() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verifyService = mock(VerifyService.class);

        when(repo.findByUsername("alice")).thenReturn(null);
        when(repo.findByEmail("alice@example.com")).thenReturn(new User("u4", "bob", "pass"));

        AuthService authService = createAuthService(repo, verifyService);

        AuthService.RegisterResult result = authService.register("alice", "pass", "alice@example.com");

        assertEquals(AuthService.RegisterResult.EMAIL_EXISTS, result);
        verify(repo, never()).createUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void register_returns_failed_when_repository_create_fails() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verifyService = mock(VerifyService.class);

        when(repo.findByUsername("alice")).thenReturn(null);
        when(repo.findByEmail("alice@example.com")).thenReturn(null);
        when(repo.createUser("alice", "pass", "USER", "alice@example.com")).thenReturn(false);

        AuthService authService = createAuthService(repo, verifyService);

        AuthService.RegisterResult result = authService.register("alice", "pass", "alice@example.com");

        assertEquals(AuthService.RegisterResult.FAILED, result);
    }

    @Test
    void register_returns_success_when_repository_create_succeeds() {
        UserRepository repo = mock(UserRepository.class);
        VerifyService verifyService = mock(VerifyService.class);

        when(repo.findByUsername("alice")).thenReturn(null);
        when(repo.findByEmail("alice@example.com")).thenReturn(null);
        when(repo.createUser("alice", "pass", "USER", "alice@example.com")).thenReturn(true);

        AuthService authService = createAuthService(repo, verifyService);

        AuthService.RegisterResult result = authService.register("alice", "pass", "alice@example.com");

        assertEquals(AuthService.RegisterResult.SUCCESS, result);
    }

    private AuthService createAuthService(UserRepository repo, VerifyService verifyService) {
        return new AuthService(repo, verifyService);
    }
}
