package com.auction.server.controller.api;

import com.auction.server.service.ProductService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.payloads.ProductPayload;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

public class AddProduct implements HttpHandler {
    private final ProductService productService = new ProductService();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            // Handle adding a new product
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            ProductPayload productData = gson.fromJson(isr, ProductPayload.class);


            boolean created = productService.addProduct(productData);

            ResponseMessage response = new ResponseMessage();
            if (created) {
                System.out.println("Product added successfully: " + productData.getProductName());
                response.setStatus("success");
                response.setMessage("Product added successfully!");
                HttpResponseUtil.sendMessage(exchange, 200, response);
            } else {
                System.out.println("Failed to add product: " + productData.getProductName());
                response.setStatus("error");
                response.setMessage("Failed to add product!");
                HttpResponseUtil.sendMessage(exchange, 500, response);
            }
        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }


}
