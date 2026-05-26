package com.auction.server.util;

import com.auction.shared.message.ResponseMessage;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class HttpResponseUtil {
    private static final Gson gson = GsonUtil.getInstance();

    public static void sendMessage(HttpExchange exchange, int statusCode, ResponseMessage responseMessage) throws IOException {
        String jsonResponse = gson.toJson(responseMessage);
        System.out.println("DEBUG - HTTP Response JSON: " + jsonResponse);
        byte[] bytes = jsonResponse.getBytes("utf-8");

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
