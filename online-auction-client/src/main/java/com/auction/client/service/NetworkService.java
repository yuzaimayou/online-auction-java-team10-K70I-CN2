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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.auction.client.util.AppConfig.ServerIp;
import static com.auction.client.util.AppConfig.SocketPort;

public class NetworkService {

    private static volatile NetworkService instance;

    private final Gson gson = new Gson();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    public interface MessageListener {
        void onMessageReceived(ResponseMessage responseMessage);
    }

    private volatile MessageListener currentListener;

    public void setCurrentListener(MessageListener listener) {
        this.currentListener = listener;
    }

    private NetworkService() {
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            synchronized (NetworkService.class) {
                if (instance == null) {
                    instance = new NetworkService();
                }
            }
        }
        return instance;
    }

    public void setListener(MessageListener listener) {
        this.currentListener = listener;
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
        if (listenerThread != null && listenerThread.isAlive()) {
            return;
        }

        listenerThread = new Thread(() -> {
            try {
                String jsonRes;
                while ((jsonRes = in.readLine()) != null) {
                    ResponseMessage response = gson.fromJson(jsonRes, ResponseMessage.class);
                    if ("join_room_success".equals(response.getStatus())) {
                        System.out.println("Successfully joined the auction room");
                        continue;
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
     *
     * [FIX #3] Trước đây có 2 overload sendAutoBidRegister:
     *   - sendAutoBidRegister(String, String, double, double)   → dùng helper send()
     *   - sendAutoBidRegister(String, String, Double, Double)   → code lặp, không nhất quán
     *
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
            }
            System.out.println("Connection to server closed.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in = null;
            out = null;
            socket = null;
            listenerThread = null;
            currentListener = null;
        }
    }
}
