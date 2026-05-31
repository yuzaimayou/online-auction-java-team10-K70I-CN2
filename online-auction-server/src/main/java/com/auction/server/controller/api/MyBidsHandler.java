package com.auction.server.controller.api;

import com.auction.server.repository.ItemRepository;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class MyBidsHandler implements HttpHandler {
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final Gson gson = GsonUtil.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpResponseUtil.sendMessage(exchange, 405,
                    new ResponseMessage("error", "Method Not Allowed", null));
            return;
        }
        String userId = extractParam(exchange.getRequestURI().getQuery(), "userId");
        if (userId == null || userId.isBlank()) {
            HttpResponseUtil.sendMessage(exchange, 400,
                    new ResponseMessage("error", "userId is required", null));
            return;
        }
        HttpResponseUtil.sendMessage(exchange, 200,
                new ResponseMessage("success", "OK", itemRepository.findMyBids(userId)));
    }

    private String extractParam(String query, String key) {
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && key.equals(kv[0])) return kv[1];
        }
        return null;
    }
}