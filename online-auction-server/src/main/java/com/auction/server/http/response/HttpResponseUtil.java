package com.auction.server.http.response;

import com.auction.shared.message.ResponseMessage;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class HttpResponseUtil {
    private static final Logger LOGGER = Logger.getLogger(HttpResponseUtil.class.getName());
    private static final Gson gson = GsonUtil.getInstance();

    public static void sendMessage(HttpExchange exchange, int statusCode, ResponseMessage responseMessage) throws IOException {
        String jsonResponse = gson.toJson(responseMessage);
        LOGGER.fine("DEBUG - HTTP Response JSON: " + jsonResponse);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
