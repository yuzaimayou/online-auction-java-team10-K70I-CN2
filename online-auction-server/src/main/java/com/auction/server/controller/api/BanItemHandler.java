package com.auction.server.controller.api;

import com.auction.server.MainServer;
import com.auction.server.service.ItemService;
import com.auction.server.util.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class BanItemHandler implements HttpHandler {

    private final ItemService itemService = ItemService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equals(exchange.getRequestMethod())) {
            HttpResponseUtil.sendMessage(exchange, 405,
                    new ResponseMessage("error", "Method Not Allowed", null));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String itemId = path.substring(path.lastIndexOf("/") + 1);

        if (itemId.isBlank() || itemId.equals("ban")) {
            HttpResponseUtil.sendMessage(exchange, 400,
                    new ResponseMessage("error", "Missing item ID", null));
            return;
        }

        boolean success = itemService.banItem(itemId);

        if (success) {
            System.out.println("[BanItemHandler] Banned item: " + itemId);

            // [REALTIME UPDATE] Broadcast tới tất cả Socket Client đang kết nối.
            // Payload dùng đúng format ResponseMessage để client parse được:
            //   { "status": "success", "message": "ITEM_BANNED", "data": { "itemId": "..." } }
            // Trước đây dùng format raw {"event":"ITEM_BANNED","itemId":"..."} khiến
            // NetworkService không map được vào ResponseMessage → onMessageReceived bị bỏ qua.
            String banPayload = String.format(
                    "{\"status\":\"success\",\"message\":\"ITEM_BANNED\",\"data\":{\"itemId\":\"%s\"}}",
                    itemId
            );

            for (com.auction.server.controller.ClientHandler client : MainServer.activeClients) {
                try {
                    client.sendMessage(banPayload);
                } catch (Exception e) {
                    System.err.println("[BanItemHandler] Failed to broadcast ban to client: " + e.getMessage());
                }
            }

            HttpResponseUtil.sendMessage(exchange, 200,
                    new ResponseMessage("success", "Item banned successfully", null));
        } else {
            HttpResponseUtil.sendMessage(exchange, 500,
                    new ResponseMessage("error", "Failed to ban item", null));
        }
    }
}