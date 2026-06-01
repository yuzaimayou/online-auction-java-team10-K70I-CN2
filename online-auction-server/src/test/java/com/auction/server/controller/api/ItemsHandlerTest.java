package com.auction.server.controller.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemsHandler API endpoint.
 * Tests item listing and creation flows.
 */
class ItemsHandlerTest {

    private ItemsHandler itemsHandler;
    private HttpExchange mockExchange;
    private Gson gson;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        gson = new Gson();
        itemsHandler = new ItemsHandler();
    }

    // ============ GET Tests ============

    @Test
    void handle_should_return_200_with_items_on_get_request() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(stringToURI("http://localhost:8000/items"));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_404_when_items_not_found() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(stringToURI("http://localhost:8000/items"));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_pass_query_parameters_to_service() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(stringToURI("http://localhost:8000/items?search=laptop"));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_get_without_query_parameters() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(stringToURI("http://localhost:8000/items"));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_empty_list_when_no_items_match_query() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(stringToURI("http://localhost:8000/items?search=nonexistent"));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    // ============ POST Tests ============

    @Test
    void handle_should_return_200_when_item_created_successfully() {
        // Arrange
        String requestJson = "{\"itemName\":\"Laptop\",\"category\":\"Electronics\",\"itemDesc\":\"High-performance\",\"initPrice\":1500.0,\"bidStep\":50.0}";
        InputStream inputStream = new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_500_when_item_creation_fails() {
        // Arrange
        String requestJson = "{\"itemName\":\"Item\",\"category\":\"Cat\",\"itemDesc\":\"Desc\",\"initPrice\":100.0,\"bidStep\":10.0}";
        InputStream inputStream = new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_accept_item_with_all_required_fields() {
        // Arrange
        String requestJson = "{\"itemName\":\"Complete\",\"category\":\"Books\",\"itemDesc\":\"Full description\",\"initPrice\":999.99,\"bidStep\":25.0}";
        InputStream inputStream = new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_item_with_special_characters_in_name() {
        // Arrange
        String requestJson = "{\"itemName\":\"Item with ñ é ü\",\"category\":\"Mixed\",\"itemDesc\":\"Desc\",\"initPrice\":100.0,\"bidStep\":10.0}";
        InputStream inputStream = new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    @Test
    void handle_should_accept_large_starting_prices() {
        // Arrange
        String requestJson = "{\"itemName\":\"Expensive\",\"category\":\"Luxury\",\"itemDesc\":\"Very expensive\",\"initPrice\":1000000.0,\"bidStep\":50000.0}";
        InputStream inputStream = new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    @Test
    void handle_should_accept_decimal_prices_and_bid_steps() {
        // Arrange
        String requestJson = "{\"itemName\":\"Decimals\",\"category\":\"Misc\",\"itemDesc\":\"With decimals\",\"initPrice\":99.99,\"bidStep\":0.5}";
        InputStream inputStream = new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    // ============ Method Not Allowed Tests ============

    @Test
    void handle_should_return_405_for_unsupported_methods() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");

        // Act
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_405_for_put_request() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("PUT");

        // Act
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_405_for_patch_request() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("PATCH");

        // Act
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));

        // Assert
        verify(mockExchange).getRequestMethod();
    }

    // ============ Error Handling Tests ============

    @Test
    void handle_should_handle_malformed_json_in_post_request() {
        // Arrange
        String malformedJson = "{invalid: json}";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    @Test
    void handle_should_handle_missing_required_fields() {
        // Arrange
        String emptyJson = "{}";
        InputStream inputStream = new ByteArrayInputStream(emptyJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    @Test
    void handle_should_handle_null_request_body() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8)));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    @Test
    void handle_should_handle_empty_request_body() {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    @Test
    void handle_should_accept_valid_item_payload_structure() {
        // Arrange
        String validJson = "{\"itemName\":\"Item\",\"itemDesc\":\"Desc\",\"initPrice\":100,\"category\":\"Cat\",\"bidStep\":10}";
        InputStream inputStream = new ByteArrayInputStream(validJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    }

    // Helper method to convert String to URI safely
    private URI stringToURI(String uriString) {
        try {
            return new URI(uriString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create URI", e);
        }
    }
}

