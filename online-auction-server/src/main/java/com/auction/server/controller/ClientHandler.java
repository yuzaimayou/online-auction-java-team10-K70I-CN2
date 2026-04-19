package com.auction.server.controller;

import com.auction.server.MainServer;
import com.auction.server.repository.ItemRepository;
import com.auction.server.service.AuctionRoomManager;
import com.auction.server.service.AuthService;
import com.auction.server.service.BidService;
import com.auction.server.service.ProductService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

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
                RequestMessage request=gson.fromJson(jsonRequest,RequestMessage.class);
                switch (request.getAction()){
                    case BID -> {}
                    case JOIN_ROOM -> {}
                    default -> {

                    }
                }

            }
        } catch (IOException e){
            e.printStackTrace();
        }
//        try {
//            clientSocket.setSoTimeout(15000);
//        } catch (Exception e) {
//            System.err.println("Loi set timeout");
//        }
//        try (
//                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//        ) {
//            String jsonRequest;
//            while ((jsonRequest = in.readLine()) != null) {
//                RequestMessage request = gson.fromJson(jsonRequest, RequestMessage.class);
//                ResponseMessage response = new ResponseMessage();
//                boolean keepConnectionAlive = true;
//                System.out.println("Kiem tra action");
//                switch (request.getAction()) {
//                    case BID -> keepConnectionAlive = bidAction(request.getPayload(), response);
//                    case AUTO_BID_REGISTER ->
//                            keepConnectionAlive = autoBidRegisterAction(request.getPayload(), response);
//                    default -> {
//                        response.setStatus("ERROR");
//                        response.setMessage("Invalid action");
//                        keepConnectionAlive = false;
//                    }
//                }
//                String jsonResponse = gson.toJson(response);
//
//                out.println(jsonResponse);
//                System.out.println("Server was sent: " + jsonResponse);
//                if (!keepConnectionAlive) {
//                    System.out.println("Ngắt kết nối với client.");
//
//                    break;
//                } else {
//                    clientSocket.setSoTimeout(0);
//                }
//            }
//
//        } catch (IOException e) {
//            System.out.println("Client disconnected/Error: " + e.getMessage());
//        } finally {
//            MainServer.activeClients.remove(this);
//            try {
//                if (!clientSocket.isClosed()) clientSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private boolean getDataItems(ResponseMessage response) {
        System.out.println("Dang goi den database");
        try {

            ItemRepository itemRepository = new ItemRepository();
            List<Item> payload = itemRepository.findAllItems();
            String jsonPayload = gson.toJson(payload);
            response.setStatus("SUCCESS");
            response.setMessage("Get data items succeed!");
            response.setData(jsonPayload);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus("FAIL");
            response.setMessage("Get data items failed!");
        }
        return true;
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

