package com.auction.server.controller.api;

import com.auction.server.service.AuctionSettlementService;
import com.auction.server.service.WalletService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WalletHandler API endpoints.
 * Tests deposit and settlement operations.
 */
class WalletHandlerTest {

    // ============ DepositHandler Tests ============

    @Test
    void depositHandler_should_reject_non_post_requests() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void depositHandler_should_return_400_when_userId_missing() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("amount", 500.0);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void depositHandler_should_return_400_when_amount_invalid() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("userId", "user123");
        request.addProperty("amount", -100.0);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void depositHandler_should_return_400_when_amount_zero() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("userId", "user123");
        request.addProperty("amount", 0.0);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void depositHandler_should_accept_valid_deposit() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("userId", "user123");
        request.addProperty("amount", 500.0);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void depositHandler_should_delegate_valid_request_to_wallet_service() throws Exception {
        HttpExchange mockExchange = mock(HttpExchange.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WalletService walletService = mock(WalletService.class);

        JsonObject request = new JsonObject();
        request.addProperty("userId", "user123");
        request.addProperty("amount", 500.0);

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(
                request.toString().getBytes(StandardCharsets.UTF_8)));
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(outputStream);
        when(walletService.deposit("user123", 500.0)).thenReturn(true);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler(walletService);

        assertDoesNotThrow(() -> handler.handle(mockExchange));

        verify(walletService).deposit("user123", 500.0);
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(outputStream.toString(StandardCharsets.UTF_8).contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void depositHandler_should_accept_large_deposit_amounts() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("userId", "user123");
        request.addProperty("amount", 1_000_000.0);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void depositHandler_should_handle_malformed_json() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        String malformedJson = "{invalid json}";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void depositHandler_should_handle_decimal_amounts() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("userId", "user123");
        request.addProperty("amount", 99.99);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    // ============ SettleHandler Tests ============

    @Test
    void settleHandler_should_reject_non_post_requests() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void settleHandler_should_return_400_when_itemId_missing() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        // itemId not set

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void settleHandler_should_return_400_when_itemId_blank() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("itemId", "   ");

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void settleHandler_should_accept_valid_settlement_request() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("itemId", "item123");

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void settleHandler_should_delegate_valid_request_to_settlement_service() throws Exception {
        HttpExchange mockExchange = mock(HttpExchange.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AuctionSettlementService settlementService = mock(AuctionSettlementService.class);

        JsonObject request = new JsonObject();
        request.addProperty("itemId", "item123");

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(
                request.toString().getBytes(StandardCharsets.UTF_8)));
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(outputStream);
        when(settlementService.settleAuction("item123")).thenReturn(
                AuctionSettlementService.SettlementResult.success("item123", "winner1", "seller1", 750.0));

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler(settlementService);

        assertDoesNotThrow(() -> handler.handle(mockExchange));

        verify(settlementService).settleAuction("item123");
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("\"status\":\"SUCCESS\""));
        assertTrue(response.contains("\"winnerId\":\"winner1\""));
        assertTrue(response.contains("\"winningPrice\":750.0"));
    }

    @Test
    void settleHandler_should_handle_malformed_json() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        String malformedJson = "{invalid: json}";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void settleHandler_should_handle_special_characters_in_itemId() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        request.addProperty("itemId", "item-123-abc-xyz");

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }

    @Test
    void settleHandler_should_accept_long_itemIds() throws Exception {
        // Arrange
        HttpExchange mockExchange = mock(HttpExchange.class);
        OutputStream outputStream = new ByteArrayOutputStream();

        JsonObject request = new JsonObject();
        String longItemId = "item-" + "x".repeat(100);
        request.addProperty("itemId", longItemId);

        String requestBody = request.toString();
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        when(mockExchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler();

        // Act & Assert
        assertDoesNotThrow(() -> handler.handle(mockExchange));
    }
}

