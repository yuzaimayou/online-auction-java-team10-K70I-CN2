package com.auction.server.http.handler;

import com.auction.server.service.user.UserService;
import com.auction.server.http.response.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BanUserHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(BanUserHandler.class.getName());
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8")) {
                JsonObject body = gson.fromJson(isr, JsonObject.class);
                if (body == null || !body.has("targetUserId")) {
                    HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "targetUserId is required", null));
                    return;
                }
                
                String targetUserId = body.get("targetUserId").getAsString();
                String adminId = body.has("adminId") ? body.get("adminId").getAsString() : null;

                boolean success = userService.banUser(adminId, targetUserId);
                if (success) {
                    HttpResponseUtil.sendMessage(exchange, 200, new ResponseMessage("success", "User banned successfully", null));
                } else {
                    HttpResponseUtil.sendMessage(exchange, 400, new ResponseMessage("error", "Failed to ban user", null));
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to ban user", e);
                HttpResponseUtil.sendMessage(exchange, 500, new ResponseMessage("error", "Internal Server Error", null));
            }
        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }
}
