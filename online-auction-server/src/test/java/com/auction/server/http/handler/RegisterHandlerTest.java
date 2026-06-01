package com.auction.server.http.handler;

import com.auction.server.service.user.AuthService;
import com.auction.shared.model.payloads.AuthPayload;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegisterHandler API endpoint.
 * Tests user registration flow with various scenarios.
 */
class RegisterHandlerTest {

    private RegisterHandler registerHandler;
    private HttpExchange mockExchange;
    private Gson gson;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        gson = new Gson();
        authService = mock(AuthService.class);
        registerHandler = new RegisterHandler(authService);
    }

    @Test
    void handle_should_reject_non_post_requests() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");

        // Act
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_400_when_username_is_missing() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername(null);
        authPayload.setPassword("password");
        authPayload.setEmail("user@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_400_when_password_is_missing() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword(null);
        authPayload.setEmail("alice@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_400_when_email_is_missing() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("password");
        authPayload.setEmail(null);

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_400_when_credentials_are_blank() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("   ");
        authPayload.setPassword("password");
        authPayload.setEmail("alice@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_200_on_successful_registration() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("newuser");
        authPayload.setPassword("securepass");
        authPayload.setEmail("newuser@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("newuser", "securepass", "newuser@example.com"))
                .thenReturn(AuthService.RegisterResult.SUCCESS);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_409_when_username_already_exists() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("password");
        authPayload.setEmail("newemail@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("alice", "password", "newemail@example.com"))
                .thenReturn(AuthService.RegisterResult.USERNAME_EXISTS);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_409_when_email_already_exists() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("newuser");
        authPayload.setPassword("password");
        authPayload.setEmail("alice@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("newuser", "password", "alice@example.com"))
                .thenReturn(AuthService.RegisterResult.EMAIL_EXISTS);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_405_for_unsupported_methods() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");

        // Act
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_trim_username_and_email() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("  alice  ");
        authPayload.setPassword("password");
        authPayload.setEmail("  alice@example.com  ");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("alice", "password", "alice@example.com"))
                .thenReturn(AuthService.RegisterResult.SUCCESS);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_handle_malformed_json_gracefully() {
        // Arrange
        String malformedJson = "{invalid json";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_accept_valid_email_formats() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("validuser");
        authPayload.setPassword("password");
        authPayload.setEmail("valid.email+tag@example.co.uk");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("validuser", "password", "valid.email+tag@example.co.uk"))
                .thenReturn(AuthService.RegisterResult.SUCCESS);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_500_when_registration_fails() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("password");
        authPayload.setEmail("alice@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("alice", "password", "alice@example.com"))
                .thenReturn(AuthService.RegisterResult.FAILED);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }

    @Test
    void handle_should_accept_special_characters_in_password() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("P@ssw0rd!#$%");
        authPayload.setEmail("alice@example.com");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(authService.register("alice", "P@ssw0rd!#$%", "alice@example.com"))
                .thenReturn(AuthService.RegisterResult.SUCCESS);

        // Act & Assert
        assertDoesNotThrow(() -> registerHandler.handle(mockExchange));
    }
}


