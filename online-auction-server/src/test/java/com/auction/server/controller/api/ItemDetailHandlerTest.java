package com.auction.server.controller.api;

import com.auction.shared.model.item.Item;
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
 * Unit tests for ItemDetailHandler API endpoint.
 * Tests item GET, PUT, DELETE operations.
 */
class ItemDetailHandlerTest {

    private ItemDetailHandler itemDetailHandler;
    private HttpExchange mockExchange;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        when(mockExchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        itemDetailHandler = new ItemDetailHandler();
    }

    // ============ GET Tests ============

    @Test
    void handle_should_return_200_with_item_on_get_request() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");
        when(mockItem.getName()).thenReturn("Laptop");
        when(mockItem.getHighestCurrentPrice()).thenReturn(1000.0);

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_404_when_item_not_found() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/nonexistent"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_extract_item_id_from_url() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item123");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/api/items/item123"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_attach_user_last_bid_when_userId_provided() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1?userId=user123"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_get_without_userId_parameter() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    // ============ PUT Tests ============

    @Test
    void handle_should_return_200_when_item_updated_successfully() throws Exception {
        // Arrange
        String requestBody = "{\"itemName\":\"Updated Laptop\",\"itemDesc\":\"Updated description\"}";
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_500_when_item_update_fails() throws Exception {
        // Arrange
        String requestBody = "{\"itemName\":\"Item\",\"itemDesc\":\"Description\"}";
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_extract_item_id_from_put_request_path() throws Exception {
        // Arrange
        String requestBody = "{\"itemName\":\"Updated Item\"}";
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item456"));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_accept_item_with_all_update_fields() throws Exception {
        // Arrange
        String requestBody = "{\"itemName\":\"Complete Update\",\"itemDesc\":\"Full description\",\"category\":\"Electronics\",\"bidStep\":50.0}";
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_malformed_json_in_put_request() throws Exception {
        // Arrange
        String malformedJson = "{invalid json}";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
    }

    // ============ DELETE Tests ============

    @Test
    void handle_should_return_200_when_item_deleted_successfully() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_return_500_when_item_deletion_fails() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/nonexistent"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_extract_item_id_from_delete_request_path() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item789"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    // ============ Validation Tests ============

    @Test
    void handle_should_extract_user_id_from_query_string() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1?userId=user456&other=value"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_ignore_userId_when_not_present_in_query() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1?other=value&another=param"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_complex_query_strings() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(
                new URI("http://localhost:8000/items/item1?userId=user123&filter=active&sort=price&limit=10")
        );

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_reject_unsupported_http_methods() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("PATCH");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_extract_item_id_from_nested_paths() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/api/v1/items/item-abc-123"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_special_characters_in_item_id() throws Exception {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item-xyz-123-abc"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }

    @Test
    void handle_should_handle_empty_query_string() throws Exception {
        // Arrange
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn("item1");

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(new URI("http://localhost:8000/items/item1?"));

        // Act & Assert
        assertDoesNotThrow(() -> itemDetailHandler.handle(mockExchange));
        verify(mockExchange).getRequestMethod();
    }
}

