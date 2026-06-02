package com.auction.server.http.handler;

import com.auction.server.repository.BidRepository;
import com.auction.server.http.response.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class MyBidsHandler implements HttpHandler {
    private final BidRepository bidRepository = new BidRepository();

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
                new ResponseMessage("success", "OK", bidRepository.findMyBids(userId)));
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
