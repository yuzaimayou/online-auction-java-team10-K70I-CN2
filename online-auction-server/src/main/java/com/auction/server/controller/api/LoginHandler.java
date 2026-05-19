package com.auction.server.controller.api;

import com.auction.server.service.user.AuthService;
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
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(LoginHandler.class.getName());
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                AuthPayload authData;
                try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8")) {
                    authData = gson.fromJson(isr, AuthPayload.class);
                }

                if (authData == null || authData.getUsername() == null || authData.getPassword() == null) {
                    ResponseMessage response = new ResponseMessage("error", "Username and password are required!", null);
                    HttpResponseUtil.sendMessage(exchange, 400, response);
                    return;
                }

                String username = authData.getUsername();
                String password = authData.getPassword();
                LOGGER.info("Received login request for username: " + username);

                User loggedInUser = authService.login(username, password);

                ResponseMessage response = new ResponseMessage();
                if (loggedInUser != null) {
                    response.setStatus("success");
                    response.setMessage("Login successful!");

                    com.auction.shared.model.dto.UserResponseDTO userDTO = new com.auction.shared.model.dto.UserResponseDTO(
                            loggedInUser.getId(),
                            loggedInUser.getUsername(),
                            loggedInUser.getEmail(),
                            loggedInUser.getRole(),
                            loggedInUser.getBalance(),
                            loggedInUser.getFrozenBalance(),
                            loggedInUser.getRating(),
                            loggedInUser.isVerify()
                    );

                    response.setData(userDTO);
                    HttpResponseUtil.sendMessage(exchange, 200, response);

                } else {
                    response.setStatus("error");
                    response.setMessage("Invalid username or password!");
                    HttpResponseUtil.sendMessage(exchange, 401, response);
                }
            } else {
                HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Exception in LoginHandler", e);
            try {
                ResponseMessage response = new ResponseMessage("error", "Internal Server Error: " + e.getMessage(), null);
                HttpResponseUtil.sendMessage(exchange, 500, response);
            } catch (Exception ex) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Failed to send 500 response", ex);
            }
        }
    }


}
