package com.auction.server.controller.api;

import com.auction.server.service.AuthService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.AuthPayload;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;

public class LoginHandler implements HttpHandler {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            AuthPayload authData = gson.fromJson(isr, AuthPayload.class);

            String username = authData.getUsername();
            String password = authData.getPassword();
            System.out.println("Received login request for username: " + username);

            User loggedInUser = authService.login(username, password);

            ResponseMessage response = new ResponseMessage();
            if (loggedInUser != null) {
                response.setStatus("success");
                response.setMessage("Login successful!");

                response.setData(gson.toJson(loggedInUser));
                HttpResponseUtil.sendMessage(exchange, 200, response);

            } else {
                response.setStatus("error");
                response.setMessage("Invalid username or password!");
                HttpResponseUtil.sendMessage(exchange, 401, response);
            }
        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }


}
