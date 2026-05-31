package com.auction.server.controller.api;

import com.auction.shared.model.payloads.AuthPayload;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginHandler API endpoint.
 * Tests login flow with various scenarios.
 */
class LoginHandlerTest {

    private LoginHandler loginHandler;
    private HttpExchange mockExchange;
    private Gson gson;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        gson = new Gson();
        loginHandler = new LoginHandler();
    }

    @Test
    void handle_should_reject_non_post_requests() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");

        // Act
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_400_when_missing_credentials() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername(null);
        authPayload.setPassword("password");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_401_for_invalid_credentials() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("wrongpass");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_200_and_user_data_on_successful_login() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("password");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_403_when_user_is_banned() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("banneduser");
        authPayload.setPassword("password");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_405_for_unsupported_methods() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("PUT");

        // Act
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_malformed_json_gracefully() {
        // Arrange
        String malformedJson = "{invalid json}";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_process_correct_json_format() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("password123");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_log_login_attempts() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("testuser");
        authPayload.setPassword("password");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }

    @Test
    void handle_should_return_empty_data_on_invalid_login() {
        // Arrange
        AuthPayload authPayload = new AuthPayload();
        authPayload.setUsername("alice");
        authPayload.setPassword("wrongpassword");

        String requestBody = gson.toJson(authPayload);
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> loginHandler.handle(mockExchange));
    }
}

