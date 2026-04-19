package com.auction.server.service;

import com.auction.server.controller.ClientHandler;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionRoomManager {
    private static final AuctionRoomManager instance = new AuctionRoomManager();

    private final Map<String, List<ClientHandler>> rooms = new ConcurrentHashMap<>();

    private AuctionRoomManager() {
    }

    public static AuctionRoomManager getInstance() {
        return instance;
    }

    public void joinRoom(String productId, ClientHandler client) {
        rooms.computeIfAbsent(productId, k -> new CopyOnWriteArrayList<>()).add(client);
        System.out.println("Client " + client.getUsername() + " joined room for product " + productId);
        //broadcastRoomInfo(productId);
    }

    public void broadcastToRoom(String productId, String message, String dataPayload) {
        List<ClientHandler> currentRoom = rooms.get(productId);
        if (currentRoom != null && !currentRoom.isEmpty()) {
            ResponseMessage response = new ResponseMessage();
            response.setStatus("success");
            response.setMessage(message);
            response.setData(dataPayload);

            String jsonMessage = new Gson().toJson(response);

            for (ClientHandler client : currentRoom) {
                client.sendMessage(jsonMessage);
            }
        }
    }

    public void leaveRoom(String productId, ClientHandler client) {
        List<ClientHandler> currentRoom = rooms.get(productId);
        if (currentRoom != null) {
            currentRoom.remove(client);
            System.out.println("Client " + client.getUsername() + " left room for product " + productId);
            if (rooms.isEmpty()) {
                rooms.remove(productId);
            }
        }
    }

    public void removeClientFromAllRooms(ClientHandler client) {
        for (String productId : rooms.keySet()) {
            leaveRoom(productId, client);
        }
    }

//    public void broadcastRoomInfo(String productId) {
//        List<ClientHandler> currentRoom = rooms.get(productId);
//        if (currentRoom != null) {
//            // Rút trích danh sách username
//            List<String> activeUsers = currentRoom.stream()
//                    .map(ClientHandler::getUsername)
//                    .toList();
//
//            String jsonUsers = new Gson().toJson(activeUsers);
//            broadcastToRoom(productId, "UPDATE_PARTICIPANTS", jsonUsers);
//        }
//    }
}
