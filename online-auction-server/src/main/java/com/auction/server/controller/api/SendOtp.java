package com.auction.server.controller.api;

import com.auction.server.service.user.VerifyService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;

public class SendOtp implements HttpHandler {
    private final VerifyService verifyService = VerifyService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {

            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            String email = extractEmail(query);

            ResponseMessage responseMessage = new ResponseMessage();
            if (email != null && !email.isEmpty()) {
                new Thread(() -> {
                    try {
                        verifyService.sendEmail(email);
                        System.out.println("Sent OTP to email: " + email);
                        String response = "OTP sent to email: " + email;
                        HttpResponseUtil.sendMessage(exchange, 200, new ResponseMessage("success", response, null));
                    } catch (Exception e) {
                        e.printStackTrace();
                        String response = "Failed to send OTP to email: " + email;
                        try {
                            HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", response, null));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            System.err.println("Failed to send error response for email: " + email);
                        }

                    }
                }).start();


            } else {
                String response = "Email parameter is missing!";
                HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", response, null));
            }
        } else {
            String response = "Method Not Allowed";
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", response, null));
        }
    }

    private String extractEmail(String query) {
        if (query == null) {
            return null;
        }
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "email".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
