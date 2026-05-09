package com.auction.server.controller.api;

import com.auction.server.service.ItemService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.LocalDateTime;

public class ItemHandler implements HttpHandler {
    private final ItemService itemService = ItemService.getInstance();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {

            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            if (query == null || !query.contains("action")) {
                return;
            }
            String[] params = query.split("&");
            String action = null;
            String itemId = null;
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "action".equals(keyValue[0])) {
                    action = keyValue[1];
                } else if (keyValue.length == 2 && "itemId".equals(keyValue[0])) {
                    itemId = keyValue[1];
                }
            }

            // Handle adding a new item
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            ItemPayload itemData = gson.fromJson(isr, ItemPayload.class);

            if (action.equals("create")) {
                createItem(exchange, itemData, new ResponseMessage());
            } else if (action.equals("update")) {
                updateItem(exchange, itemData, itemId, new ResponseMessage());
            } else if (action.equals("delete")) {
                deleteItem(exchange, itemId, new ResponseMessage());
            } else {
                HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Invalid action parameter", null));

            }

        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }

    private void createItem(HttpExchange exchange, ItemPayload itemData, ResponseMessage response) throws IOException {
        boolean created = itemService.addItem(itemData);

        if (created) {
            System.out.println("Item added successfully: " + itemData.getItemName());
            response.setStatus("success");
            response.setMessage("Item added successfully!");
            HttpResponseUtil.sendMessage(exchange, 200, response);
        } else {
            System.out.println("Failed to add item: " + itemData.getItemName());
            response.setStatus("error");
            response.setMessage("Failed to add item!");
            HttpResponseUtil.sendMessage(exchange, 500, response);
        }
    }

    private void updateItem(HttpExchange exchange, ItemPayload itemData, String itemId, ResponseMessage response) throws IOException {
        boolean updated = itemService.updateItem(itemData, itemId);
        if (updated) {
            System.out.println("Item updated successfully: " + itemData.getItemName());
            response.setStatus("success");
            response.setMessage("Item updated successfully!");
            HttpResponseUtil.sendMessage(exchange, 200, response);
        } else {
            System.out.println("Failed to update item: " + itemData.getItemName());
            response.setStatus("error");
            response.setMessage("Failed to update item!");
            HttpResponseUtil.sendMessage(exchange, 500, response);
        }
    }

    private void deleteItem(HttpExchange exchange, String itemId, ResponseMessage response) throws IOException {
        boolean deleted = itemService.deleteItem(itemId);
        if (deleted) {
            System.out.println("Item deleted successfully: " + itemId);
            response.setStatus("success");
            response.setMessage("Item deleted successfully!");
            HttpResponseUtil.sendMessage(exchange, 200, response);
        } else {
            System.out.println("Failed to delete item: " + itemId);
            response.setStatus("error");
            response.setMessage("Failed to delete item!");
            HttpResponseUtil.sendMessage(exchange, 500, response);
        }
    }


}
