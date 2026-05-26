package com.auction.server.service;

import com.auction.server.controller.ClientHandler;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class AuctionRoomManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionRoomManager.class.getName());
    private static final AuctionRoomManager instance = new AuctionRoomManager();

    private final Gson gson = new Gson();
    private final Map<String, CopyOnWriteArrayList<ClientHandler>> rooms = new ConcurrentHashMap<>();

    private AuctionRoomManager() {
    }

    public static AuctionRoomManager getInstance() {
        return instance;
    }

    public void joinRoom(String itemId, ClientHandler client) {
        rooms.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>()).addIfAbsent(client);
        LOGGER.fine("Client " + client.getUsername() + " joined room for item " + itemId);
        //broadcastRoomInfo(itemId);
    }

    public void broadcastToRoom(String itemId, String message, Object dataPayload) {
        CopyOnWriteArrayList<ClientHandler> currentRoom = rooms.get(itemId);
        if (currentRoom != null && !currentRoom.isEmpty()) {
            ResponseMessage response = new ResponseMessage();
            response.setStatus(SocketEventConstants.STATUS_SUCCESS_LOWER);
            response.setMessage(message);
            response.setData(dataPayload);
            LOGGER.fine("Broadcasting message to room " + itemId + ": " + message);

            String jsonMessage = gson.toJson(response);

            for (ClientHandler client : currentRoom) {
                try {
                    client.sendMessage(jsonMessage);
                } catch (Exception e) {
                    LOGGER.warning("Failed to broadcast to client " + client.getUsername() + ": " + e.getMessage());
                }
            }
        } else {
            LOGGER.fine("No clients in room " + itemId + " to broadcast message: " + message);
        }
    }

    public void leaveRoom(String itemId, ClientHandler client) {
        CopyOnWriteArrayList<ClientHandler> currentRoom = rooms.get(itemId);
        if (currentRoom != null) {
            currentRoom.remove(client);
            LOGGER.fine("Client " + client.getUsername() + " left room for item " + itemId);
            if (currentRoom.isEmpty()) {
                rooms.remove(itemId, currentRoom);
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
