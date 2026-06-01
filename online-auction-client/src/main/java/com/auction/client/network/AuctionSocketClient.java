package com.auction.client.network;

import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.enums.ActionType;
import com.auction.shared.model.payloads.AutoBidPayload;
import com.auction.shared.model.payloads.BidPayload;
import com.auction.shared.model.payloads.RoomPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.auction.client.util.AppConfig.ServerIp;
import static com.auction.client.util.AppConfig.SocketPort;

public class AuctionSocketClient {

    private static volatile AuctionSocketClient instance;

    private final Gson gson = GsonUtil.getInstance();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    private volatile AuctionRoomListener auctionRoomListener;

    public void setAuctionRoomListener(AuctionRoomListener listener) {
        this.auctionRoomListener = listener;
    }

    private AuctionSocketClient() {
    }

    public static AuctionSocketClient getInstance() {
        if (instance == null) {
            synchronized (AuctionSocketClient.class) {
                if (instance == null) {
                    instance = new AuctionSocketClient();
                }
            }
        }
        return instance;
    }


    public boolean connectToAuctionRoom(String roomId, String token) {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket(ServerIp, SocketPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            }
            String jsonPayload = gson.toJson(new RoomPayload(roomId, token));
            RequestMessage request = new RequestMessage(ActionType.JOIN_ROOM, jsonPayload);
            out.println(gson.toJson(request));
            startListeningThread();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to the server");
            return false;
        }
    }

    private void startListeningThread() {
        if (listenerThread != null && listenerThread.isAlive() && !listenerThread.isInterrupted())
            return;
        listenerThread = new Thread(() -> {
            try {
                String jsonRes;
                while ((jsonRes = in.readLine()) != null) {
                    ResponseMessage response = gson.fromJson(jsonRes, ResponseMessage.class);
                    System.out.println("Client received message: " + response.getStatus() + " - " + response.getMessage() + " - " + response.getData());
                    JsonObject jsonObject = gson.fromJson(jsonRes, JsonObject.class);
                    AutoBidPayload autoBidPayload = gson.fromJson(jsonObject.get("data"), AutoBidPayload.class);
                    if (SocketEventConstants.STATUS_JOIN_ROOM_SUCCESS.equals(response.getStatus())) {
                        System.out.println("Successfully joined the auction room");
                        auctionRoomListener.onAutoBidState(autoBidPayload);
                        continue;
                    } else if (SocketEventConstants.STATUS_JOIN_ROOM_FAIL.equals(response.getStatus())) {
                        throw new IOException("Failed to join the auction room: " + response.getMessage());
                    }
                    if (auctionRoomListener != null) {
                        dispatchToListener(response, auctionRoomListener);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection closed: " + e.getMessage());
                closeConnection();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private synchronized void send(ActionType actionType, Object payload) {
        if (out == null || socket == null || socket.isClosed()) {
            System.out.println("Cannot send message [" + actionType + "], not connected to server");
            return;
        }
        String jsonPayload = gson.toJson(payload);
        RequestMessage requestMessage = new RequestMessage(actionType, jsonPayload);
        String json = gson.toJson(requestMessage);
        out.println(json);
        System.out.println("Client was sent message: " + json);
    }

    public void sendBid(String itemId, String userId, Double bidPrice, String bidTime) {
        BidPayload payload = new BidPayload(itemId, userId, bidPrice, bidTime);
        send(ActionType.BID, payload);
    }

    /**
     * Gửi yêu cầu đăng ký auto-bid lên server.
     * <p>
     * [FIX #3] Trước đây có 2 overload sendAutoBidRegister:
     * - sendAutoBidRegister(String, String, double, double)   → dùng helper send()
     * - sendAutoBidRegister(String, String, Double, Double)   → code lặp, không nhất quán
     * <p>
     * Gộp lại thành 1 method dùng double (primitive), loại bỏ overload thừa.
     * Gọi từ ItemPageController dùng double literals → không bị ảnh hưởng.
     */
    public void sendAutoBidRegister(String itemId, String userId, double maxBid, double increment) {
        AutoBidPayload payload = new AutoBidPayload(itemId, userId, maxBid, increment);
        send(ActionType.AUTO_BID_REGISTER, payload);
    }

    public void sendGetAutoBidStatus(String itemId, String userId) {
        AutoBidPayload payload = new AutoBidPayload(itemId, userId, null, null);
        send(ActionType.GET_AUTO_BID_STATUS, payload);
    }

    public void sendCancelAutoBid(String itemId, String userId) {
        AutoBidPayload payload = new AutoBidPayload(itemId, userId, null, null);
        System.out.printf("[AUTO_BID_CANCEL][CLIENT_SEND] time=%s itemId=%s userId=%s%n",
                LocalDateTime.now(), itemId, userId);
        send(ActionType.CANCEL_AUTO_BID, payload);
    }

    public void leaveRoom() {
        send(ActionType.LEAVE_ROOM, null);
    }

    public synchronized void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
                try {
                    listenerThread.join(2000); // chờ tối đa 2 giây
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("Connection to server closed.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in = null;
            out = null;
            socket = null;
            listenerThread = null;
        }
    }

    private void dispatchToListener(ResponseMessage response, AuctionRoomListener listener) {
        String event = response.getMessage();
        switch (event) {
            case SocketEventConstants.EVENT_NEW_BID -> {
                if (!SocketEventConstants.STATUS_SUCCESS_LOWER.equals(response.getStatus())) return;
                BidPayload payload = gson.fromJson(gson.toJsonTree(response.getData()), BidPayload.class);
                if (payload != null) listener.onNewBid(payload);
            }
            case SocketEventConstants.EVENT_AUCTION_EXTENDED -> {
                JsonObject data = gson.fromJson(gson.toJsonTree(response.getData()), JsonObject.class);
                if (data != null && data.has("endTime"))
                    listener.onAuctionExtended(LocalDateTime.parse(data.get("endTime").getAsString()));
            }
            case SocketEventConstants.EVENT_ITEM_BANNED -> {
                JsonObject data = gson.fromJson(gson.toJsonTree(response.getData()), JsonObject.class);
                if (data != null && data.has("itemId"))
                    listener.onItemBanned(data.get("itemId").getAsString());
            }
            case SocketEventConstants.EVENT_UPDATE_TIME -> {
                try {
                    LocalDateTime newEndTime = LocalDateTime.parse(response.getData().toString());
                    listener.onAuctionExtended(newEndTime);
                } catch (Exception e) {
                    System.err.println("Failed to parse time update data: " + e.getMessage());
                    System.out.println("[TIME UPDATE] Received time update with unparseable data: " + response.getData());
                }

            }
        }
    }
}
