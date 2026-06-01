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
    private final Gson gson = GsonUtil.getInstance();
    private final ItemService itemService = ItemService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {

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

        } else if ("POST".equals(exchange.getRequestMethod())) {
            // Handle adding a new item
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            ItemPayload itemPayload = gson.fromJson(isr, ItemPayload.class);
            boolean created = itemService.addItem(itemPayload);
            if (!created) {
                HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", "Failed to create item", null));
                return;
            }
            HttpResponseUtil.sendMessage(exchange, 200, new ResponseMessage("success", "Create item successfully", null));

        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }


}
