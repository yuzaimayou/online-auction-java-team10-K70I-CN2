package com.auction.server.http.handler;

import com.auction.server.service.user.UserService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class BanUserHandlerTest {

    private HttpExchange exchange;
    private ByteArrayOutputStream responseBody;
    private int statusCode;

    @BeforeEach
    void setUp() throws Exception {
        exchange = mock(HttpExchange.class);
        responseBody = new ByteArrayOutputStream();
        statusCode = -1;

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(responseBody);
        org.mockito.Mockito.doAnswer(invocation -> {
            statusCode = invocation.getArgument(0);
            return null;
        }).when(exchange).sendResponseHeaders(org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyLong());
    }

    @Test
    void post_missing_targetUserId_returns_400() throws Exception {
        BanUserHandler handler = new BanUserHandler();

        // body without targetUserId
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

        handler.handle(exchange);

        JsonObject json = JsonParser.parseString(responseBody.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(400, statusCode);
        assertEquals("error", json.get("status").getAsString());
        assertTrue(json.get("message").getAsString().contains("targetUserId"));
    }

    @Test
    void post_ban_success_returns_200() throws Exception {
        BanUserHandler handler = new BanUserHandler();

        // inject mock UserService
        UserService mockService = mock(UserService.class);
        when(mockService.banUser("admin-1", "target-1")).thenReturn(true);
        setPrivateField(handler, "userService", mockService);

        String body = "{\"adminId\":\"admin-1\",\"targetUserId\":\"target-1\"}";
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        handler.handle(exchange);

        JsonObject json = JsonParser.parseString(responseBody.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(200, statusCode);
        assertEquals("success", json.get("status").getAsString());
        verify(mockService).banUser("admin-1", "target-1");
    }

    @Test
    void post_ban_failure_returns_400() throws Exception {
        BanUserHandler handler = new BanUserHandler();

        UserService mockService = mock(UserService.class);
        when(mockService.banUser(null, "target-2")).thenReturn(false);
        setPrivateField(handler, "userService", mockService);

        String body = "{\"targetUserId\":\"target-2\"}";
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        handler.handle(exchange);

        JsonObject json = JsonParser.parseString(responseBody.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(400, statusCode);
        assertEquals("error", json.get("status").getAsString());
    }

    @Test
    void non_post_method_returns_405() throws Exception {
        BanUserHandler handler = new BanUserHandler();
        when(exchange.getRequestMethod()).thenReturn("GET");

        handler.handle(exchange);

        assertEquals(405, statusCode);
    }

    @Test
    void service_exception_returns_500() throws Exception {
        BanUserHandler handler = new BanUserHandler();

        UserService mockService = mock(UserService.class);
        when(mockService.banUser("a", "b")).thenThrow(new RuntimeException("boom"));
        setPrivateField(handler, "userService", mockService);

        String body = "{\"adminId\":\"a\",\"targetUserId\":\"b\"}";
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        handler.handle(exchange);

        JsonObject json = JsonParser.parseString(responseBody.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(500, statusCode);
        assertEquals("error", json.get("status").getAsString());
    }

    // helper to set private final fields
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}

