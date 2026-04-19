package com.auction.server.controller.api;

import com.auction.server.repository.ItemRepository;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class GetDataProducts implements HttpHandler {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {

            ItemRepository itemRepository = new ItemRepository();
            List<Item> payload = itemRepository.findAllItems();
            ResponseMessage response = new ResponseMessage();
            if (payload != null) {
                String jsonPayload = gson.toJson(payload);


                response.setStatus("success");
                response.setMessage("Get data products successfully");
                response.setData(jsonPayload);
                HttpResponseUtil.sendMessage(exchange, 200, response);
            } else {
                response.setStatus("error");
                response.setMessage("Failed to get products");
                HttpResponseUtil.sendMessage(exchange, 404, response);

            }

        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }
}
