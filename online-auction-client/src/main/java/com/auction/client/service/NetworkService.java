package com.auction.client.service;

import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.enums.ActionType;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.payloads.RoomPayload;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

import static com.auction.client.util.AppConfig.ServerIp;
import static com.auction.client.util.AppConfig.SocketPort;

public class NetworkService {
    private static NetworkService instance;


    private final Gson gson = new Gson();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    public interface MessageListener {
        void onMessageReceived(ResponseMessage responseMessage);
    }

    private MessageListener currentListener;

    public void setCurrentListener(MessageListener listener) {
        this.currentListener = listener;
    }

    private NetworkService() {
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }

    //  Đặt listener mới và bắt đầu thread lắng nghe nếu kết nối thành công
    public void setListener(MessageListener listener) {
        this.currentListener = listener;
    }

    public boolean connectToAuctionRoom(String roomId, String token) {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket(ServerIp, SocketPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));


            }
            String jsonPayload = gson.toJson(new RoomPayload(roomId, token));
            RequestMessage reqest = new RequestMessage(ActionType.JOIN_ROOM, jsonPayload);

            out.println(gson.toJson(reqest));
            startListeningThread();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to the server");
            return false;
        }
    }


    private void startListeningThread() {
        listenerThread = new Thread(() -> {
            try {
                String jsonRes;

                while ((jsonRes = in.readLine()) != null) {
                    System.out.println("Client was received message: " + jsonRes);


                    ResponseMessage response = gson.fromJson(jsonRes, ResponseMessage.class);
                    if ("join_room_success".equals(response.getStatus())) {
                        System.out.println("Successfully joined the auction room");
                    } else if ("join_room_fail".equals(response.getStatus())) {
                        throw new IOException("Failed to join the auction room: " + response.getMessage());
                    }

                    if (currentListener != null) {
                        currentListener.onMessageReceived(response);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection closed by server or error occurred: " + e.getMessage());
                closeConnection();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendBid(String itemId, String userId, Double bidPrice, String bidTime) {
        if (out != null && socket != null && !socket.isClosed()) {
            BidPayload payload = new BidPayload(itemId, userId, bidPrice, bidTime);
            String jsonPayload = gson.toJson(payload);
            RequestMessage requestMessage = new RequestMessage(ActionType.BID, jsonPayload);
            String bidMessage = gson.toJson(requestMessage);
            out.println(bidMessage);
            System.out.println("Client was sent message: " + bidMessage);
        } else {
            System.out.println("Cannot send bid, not connected to server");
        }
    }

    public void sendAutoBidRegister(String itemId, String userId, Double maxBid, Double increment) {
        if (out != null && socket != null && !socket.isClosed()) {
            AutoBidPayload payload = new AutoBidPayload(itemId, userId, maxBid, increment);
            String jsonPayload = gson.toJson(payload);
            RequestMessage requestMessage = new RequestMessage(ActionType.AUTO_BID_REGISTER, jsonPayload);
            String autoBidMessage = gson.toJson(requestMessage);
            out.println(autoBidMessage);
            System.out.println("Client was sent message: " + autoBidMessage);
        } else {
            System.out.println("Cannot register auto-bid, not connected to server");
        }
    }

    public void sendGetAutoBidStatus(String itemId, String userId) {
        if (out != null && socket != null && !socket.isClosed()) {
            AutoBidPayload payload = new AutoBidPayload(itemId, userId, null, null);
            String jsonPayload = gson.toJson(payload);
            RequestMessage requestMessage = new RequestMessage(ActionType.GET_AUTO_BID_STATUS, jsonPayload);
            String statusMessage = gson.toJson(requestMessage);
            out.println(statusMessage);
            System.out.println("Client was sent message: " + statusMessage);
        } else {
            System.out.println("Cannot fetch auto-bid status, not connected to server");
        }
    }

    public void sendCancelAutoBid(String itemId, String userId) {
        if (out != null && socket != null && !socket.isClosed()) {
            AutoBidPayload payload = new AutoBidPayload(itemId, userId, null, null);
            String jsonPayload = gson.toJson(payload);
            RequestMessage requestMessage = new RequestMessage(ActionType.CANCEL_AUTO_BID, jsonPayload);
            String cancelMessage = gson.toJson(requestMessage);
            out.println(cancelMessage);
            System.out.printf("[AUTO_BID_CANCEL][CLIENT_SEND] time=%s itemId=%s userId=%s%n",
                    LocalDateTime.now(), itemId, userId);
            System.out.println("Client was sent message: " + cancelMessage);
        } else {
            System.out.println("Cannot cancel auto-bid, not connected to server");
        }
    }

    public void leaveRoom() {
        if (out != null && socket != null && !socket.isClosed()) {
            RequestMessage requestMessage = new RequestMessage(ActionType.LEAVE_ROOM, null);
            out.println(gson.toJson(requestMessage));
            System.out.println("Client was sent message: " + gson.toJson(requestMessage));
        } else {
            System.out.println("Cannot leave room, not connected to server");
        }
    }


    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();

            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }

            System.out.println("Connection to server closed.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            currentListener = null;
        }
    }

}
