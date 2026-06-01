package com.auction.server.http.handler;

import com.auction.server.service.user.VerifyService;
import com.auction.server.http.response.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendOtp implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(SendOtp.class.getName());
    private final VerifyService verifyService = VerifyService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {

            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            String email = extractEmail(query);


            if (email != null && !email.isEmpty()) {
                try {
                    verifyService.sendEmail(email);
                    LOGGER.info("Sent OTP to email: " + email);
                    String response = "OTP sent to email: " + email;
                    HttpResponseUtil.sendMessage(exchange, 200, new ResponseMessage("success", response, null));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to send OTP to email: " + email, e);
                    String response = "Failed to send OTP to email: " + email;
                    try {
                        HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", response, null));
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, "Failed to send error response for email: " + email, ex);
                    }
                }
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
