package com.auction.server.controller.api;

import com.auction.server.database.DatabaseManager;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class GetUsersHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpResponseUtil.sendMessage(exchange, 405,
                    new ResponseMessage("error", "Method Not Allowed", null));
            return;
        }
   String sql = "SELECT id, username, email, role, status FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Map<String, String>> users = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> u = new LinkedHashMap<>();
                String role = rs.getString("role");
                u.put("id",       rs.getString("id")       != null ? rs.getString("id")       : "");
                u.put("username", rs.getString("username") != null ? rs.getString("username") : "");
                u.put("email",    rs.getString("email")    != null ? rs.getString("email")    : "");
                u.put("role",     role                     != null ? role                     : "User");
                String status = rs.getString("status");
                u.put("status", status != null ? status : "Active");
                users.add(u);
            }
            String json = gson.toJson(users);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes().length);
            exchange.getResponseBody().write(json.getBytes());
            exchange.getResponseBody().close();

        } catch (Exception e) {
            e.printStackTrace();
            HttpResponseUtil.sendMessage(exchange, 500,
                    new ResponseMessage("error", "Internal Server Error", null));
        }
    }
}