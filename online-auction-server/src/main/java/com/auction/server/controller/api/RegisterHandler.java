package com.auction.server.controller.api;

import com.auction.server.service.user.AuthService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.payloads.AuthPayload;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;

public class RegisterHandler implements HttpHandler {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            AuthPayload authData = gson.fromJson(isr, AuthPayload.class);

            String username = authData.getUsername();
            String password = authData.getPassword();
            String email = authData.getEmail();

            boolean created = authService.register(username, password, email);

            ResponseMessage response = new ResponseMessage();
            if (created) {
                response.setStatus("success");
                response.setMessage("User registered successfully!");
                HttpResponseUtil.sendMessage(exchange, 200, response);
            } else {
                response.setStatus("error");
                response.setMessage("Username already exists!");
                HttpResponseUtil.sendMessage(exchange, 409, response);
            }

        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }

    }
}
