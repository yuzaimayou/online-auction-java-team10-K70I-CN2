package com.auction.server.controller;

import com.auction.server.service.AuctionRoomManager;
import com.auction.server.service.BidService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.payloads.RoomPayload;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private String currentRoomId = null;
    private String username = "unknown";

    private AuctionRoomManager roomManager = AuctionRoomManager.getInstance();
    private BidService bidService = new BidService();


    public ClientHandler() {
    }

    ;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        }

    }

    @Override
    public void run() {
        try {
            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                RequestMessage request = gson.fromJson(jsonRequest, RequestMessage.class);
                String jsonPayload = request.getPayload();

                switch (request.getAction()) {
                    case BID -> {
                    }
                    case JOIN_ROOM -> joinRoomAction(jsonPayload);
                    default -> {

                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void joinRoomAction(String jsonPayload) {
        RoomPayload roomPayload = gson.fromJson(jsonPayload, RoomPayload.class);
        String userId = roomPayload.getToken();

        currentRoomId = roomPayload.getProductId();
        roomManager.joinRoom(currentRoomId, this);
        System.out.println("Client " + userId + " joined room for product " + currentRoomId);

    }

    private boolean bidAction(String payload, ResponseMessage response) {
        BidPayload bidData = gson.fromJson(payload, BidPayload.class);

        if (bidData == null || bidData.getItemId() == null || bidData.getUserId() == null || bidData.getBidPrice() == null) {
            response.setStatus("FAIL");
            response.setMessage("Invalid bid payload");
            return true;
        }

        boolean created = bidService.placeBid(
                bidData.getItemId(),
                bidData.getUserId(),
                bidData.getBidPrice(),
                bidData.getBidTime()
        );

        if (created) {
            response.setStatus("SUCCESS");
            response.setMessage("Bid placed successfully");
        } else {
            response.setStatus("FAIL");
            response.setMessage("Failed to place bid");
        }

        return true;
    }

    private boolean autoBidRegisterAction(String payload, ResponseMessage response) {
        AutoBidPayload autoBidData = gson.fromJson(payload, AutoBidPayload.class);

        if (autoBidData == null
                || autoBidData.getItemId() == null
                || autoBidData.getUserId() == null
                || autoBidData.getMaxBid() == null
                || autoBidData.getIncrement() == null) {
            response.setStatus("FAIL");
            response.setMessage("Invalid auto bid payload");
            return true;
        }

        boolean created = bidService.registerAutoBid(
                autoBidData.getItemId(),
                autoBidData.getUserId(),
                autoBidData.getMaxBid(),
                autoBidData.getIncrement()
        );

        if (created) {
            response.setStatus("SUCCESS");
            response.setMessage("Auto-bid registered successfully");
        } else {
            response.setStatus("FAIL");
            response.setMessage("Failed to register auto-bid");
        }

        return true;
    }

    public String getUsername() {
        return "username";
    }
}

