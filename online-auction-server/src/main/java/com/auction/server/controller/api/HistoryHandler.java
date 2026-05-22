package com.auction.server.controller.api;

import com.auction.server.service.BidHistoryService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class HistoryHandler implements HttpHandler {
    private BidHistoryService bidHistoryService = BidHistoryService.getInstance();
    private final Gson gson = GsonUtil.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String itemId = path.substring(path.lastIndexOf("/") + 1);
        ResponseMessage responseMessage = new ResponseMessage();
        switch (method) {
            case "GET" -> {
                List<BidHistoryItemDTO> history = bidHistoryService.getHistory(itemId);

                if (history != null && !history.isEmpty()) {
                    responseMessage.setStatus("success");
                    responseMessage.setMessage("Get bid history successfully");
                    responseMessage.setData(history);
                    HttpResponseUtil.sendMessage(exchange, 200, responseMessage);
                } else {
                    responseMessage.setStatus("error");
                    responseMessage.setMessage("No bid history found for this item");
                    HttpResponseUtil.sendMessage(exchange, 404, responseMessage);
                }
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }
}
