package com.auction.server.http.handler;

import com.auction.server.service.item.ItemService;
import com.auction.server.http.response.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

public class ItemsHandler implements HttpHandler {
    private final Gson gson;
    private final ItemService itemService;

    public ItemsHandler() {
        this(ItemService.getInstance(), GsonUtil.getInstance());
    }

    ItemsHandler(ItemService itemService) {
        this(itemService, GsonUtil.getInstance());
    }

    ItemsHandler(ItemService itemService, Gson gson) {
        this.itemService = itemService;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if ("GET".equals(method)) {

                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery();

                List<ItemSummary> payload = itemService.getItems(query);


                ResponseMessage response = new ResponseMessage();
                if (payload != null) {

                    response.setStatus("success");
                    response.setMessage("Get data items successfully");
                    response.setData(payload);
                    HttpResponseUtil.sendMessage(exchange, 200, response);
                } else {
                    response.setStatus("error");
                    response.setMessage("Failed to get items");
                    HttpResponseUtil.sendMessage(exchange, 404, response);

                }

            } else if ("POST".equals(method)) {
                // Handle adding a new item
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                ItemPayload itemPayload = gson.fromJson(isr, ItemPayload.class);
                if (itemPayload == null) {
                    HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Invalid item payload", null));
                    return;
                }
                boolean created = itemService.addItem(itemPayload);
                if (!created) {
                    HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", "Failed to create item", null));
                    return;
                }
                HttpResponseUtil.sendMessage(exchange, 200, new ResponseMessage("success", "Create item successfully", null));

            } else {
                HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
            }
        } catch (com.google.gson.JsonSyntaxException | IllegalArgumentException e) {
            HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", "Internal Server Error: " + e.getMessage(), null));
        }
    }


}
