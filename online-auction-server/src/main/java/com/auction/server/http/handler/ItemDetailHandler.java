package com.auction.server.http.handler;

import com.auction.server.service.item.ItemService;
import com.auction.server.http.response.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class ItemDetailHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(ItemDetailHandler.class.getName());
    private ItemService itemService = ItemService.getInstance();
    private final Gson gson = GsonUtil.getInstance();

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String itemId = path.substring(path.lastIndexOf("/") + 1); // Extract item ID from URL
            LOGGER.info("Received " + method + " request for item ID: " + itemId);
            switch (method) {
                case "GET" -> {
                    getItem(exchange, itemId, new ResponseMessage());
                }
                case "PUT" -> {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    ItemPayload itemData = gson.fromJson(isr, ItemPayload.class);
                    if (itemData == null) {
                        HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Invalid item payload", null));
                        return;
                    }
                    updateItem(exchange, itemData, itemId, new ResponseMessage());
                }
                case "DELETE" -> {
                    deleteItem(exchange, itemId, new ResponseMessage());
                }
                default -> exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        } catch (com.google.gson.JsonSyntaxException | IllegalArgumentException e) {
            HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", "Internal Server Error: " + e.getMessage(), null));
        }
    }

    private void getItem(HttpExchange exchange, String itemId, ResponseMessage responseMessage) throws IOException {
        Item item = itemService.getItem(itemId);
        if (item != null) {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                String userId = extractUserIdFromQuery(query);
                if (userId != null) {
                    double lastBid = itemService.getUserLastBid(itemId, userId);
                    item.setMyLastBid(lastBid); // Gán giá trị vào object Item trước khi parse ra JSON
                }
            }
            responseMessage.setStatus("success");
            responseMessage.setMessage("Get item details successfully");
            responseMessage.setData(item);
            HttpResponseUtil.sendMessage(exchange, 200, responseMessage);
        } else {
            responseMessage.setStatus("error");
            responseMessage.setMessage("Item not found");
            HttpResponseUtil.sendMessage(exchange, 404, responseMessage);
        }
    }


    // Hàm tiện ích hỗ trợ trích xuất userId từ query string
    private String extractUserIdFromQuery(String query) {
        String[] params = query.split("&");

        for (String param : params) {
            String[] keyValue = param.split("=");

            if (keyValue.length == 2 && "userId".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    private void updateItem(HttpExchange exchange, ItemPayload itemData, String itemId, ResponseMessage response) throws IOException {
        boolean updated = itemService.updateItem(itemData, itemId);
        if (updated) {
            LOGGER.info("Item updated successfully: " + itemData.getItemName());
            response.setStatus("success");
            response.setMessage("Item updated successfully!");
            HttpResponseUtil.sendMessage(exchange, 200, response);
        } else {
            LOGGER.warning("Failed to update item: " + itemData.getItemName());
            response.setStatus("error");
            response.setMessage("Failed to update item!");
            HttpResponseUtil.sendMessage(exchange, 500, response);
        }
    }

    private void deleteItem(HttpExchange exchange, String itemId, ResponseMessage response) throws IOException {
        boolean deleted = itemService.deleteItem(itemId);
        if (deleted) {
            LOGGER.info("Item deleted successfully: " + itemId);
            response.setStatus("success");
            response.setMessage("Item deleted successfully!");
            HttpResponseUtil.sendMessage(exchange, 200, response);
        } else {
            LOGGER.warning("Failed to delete item: " + itemId);
            response.setStatus("error");
            response.setMessage("Failed to delete item!");
            HttpResponseUtil.sendMessage(exchange, 500, response);
        }
    }
}
