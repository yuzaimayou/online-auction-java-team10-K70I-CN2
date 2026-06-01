package com.auction.server.http.handler;

import com.auction.server.service.user.AuthService;
import com.auction.server.http.response.HttpResponseUtil;
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
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                HttpResponseUtil.sendMessage(exchange, 405,
                        new ResponseMessage("error", "Method Not Allowed", null));
                return;
            }

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            AuthPayload authData = gson.fromJson(isr, AuthPayload.class);

            if (authData == null
                    || isBlank(authData.getUsername())
                    || isBlank(authData.getPassword())
                    || isBlank(authData.getEmail())) {
                HttpResponseUtil.sendMessage(exchange, 400,
                        new ResponseMessage("error", "Username, password and email are required", null));
                return;
            }

            String username = authData.getUsername().trim();
            String password = authData.getPassword();
            String email    = authData.getEmail().trim();

            AuthService.RegisterResult result = authService.register(username, password, email);

            ResponseMessage response = new ResponseMessage();
            switch (result) {
                case SUCCESS -> {
                    response.setStatus("success");
                    response.setMessage("User registered successfully!");
                    HttpResponseUtil.sendMessage(exchange, 200, response);
                }
                case USERNAME_EXISTS -> {
                    response.setStatus("error");
                    response.setMessage("Username already exists!");
                    HttpResponseUtil.sendMessage(exchange, 409, response);
                }
                case EMAIL_EXISTS -> {
                    response.setStatus("error");
                    response.setMessage("Email already registered!");
                    HttpResponseUtil.sendMessage(exchange, 409, response);
                }
                case FAILED -> {
                    response.setStatus("error");
                    response.setMessage("Registration failed. Please try again.");
                    HttpResponseUtil.sendMessage(exchange, 500, response);
                }
            }
        } catch (com.google.gson.JsonSyntaxException | IllegalArgumentException e) {
            HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", "Internal Server Error: " + e.getMessage(), null));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
