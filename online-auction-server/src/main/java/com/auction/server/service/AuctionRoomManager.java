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

    public void joinRoom(String itemId, ClientHandler client) {
        rooms.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>()).add(client);
        System.out.println("Client " + client.getUsername() + " joined room for item " + itemId);
        //broadcastRoomInfo(itemId);
    }

    public void broadcastToRoom(String itemId, String message, String dataPayload) {
        List<ClientHandler> currentRoom = rooms.get(itemId);
        if (currentRoom != null && !currentRoom.isEmpty()) {
            ResponseMessage response = new ResponseMessage();
            response.setStatus("success");
            response.setMessage(message);
            response.setData(dataPayload);
            System.out.println("Broadcasting message to room " + itemId + ": " + message);

            String jsonMessage = new Gson().toJson(response);

            for (ClientHandler client : currentRoom) {
                client.sendMessage(jsonMessage);
            }
        } else {
            System.out.println("No clients in room " + itemId + " to broadcast message: " + message);
        }
    }

    public void leaveRoom(String itemId, ClientHandler client) {
        List<ClientHandler> currentRoom = rooms.get(itemId);
        if (currentRoom != null) {
            currentRoom.remove(client);
            System.out.println("Client " + client.getUsername() + " left room for item " + itemId);
            if (rooms.isEmpty()) {
                rooms.remove(itemId);
            }
        }
    }

    public void removeClientFromAllRooms(ClientHandler client) {
        for (String itemId : rooms.keySet()) {
            leaveRoom(itemId, client);
        }
    }

//    public void broadcastRoomInfo(String itemId) {
//        List<ClientHandler> currentRoom = rooms.get(itemId);
//        if (currentRoom != null) {
//            // Rút trích danh sách username
//            List<String> activeUsers = currentRoom.stream()
//                    .map(ClientHandler::getUsername)
//                    .toList();
//
//            String jsonUsers = new Gson().toJson(activeUsers);
//            broadcastToRoom(itemId, "UPDATE_PARTICIPANTS", jsonUsers);
//        }
//    }
}
