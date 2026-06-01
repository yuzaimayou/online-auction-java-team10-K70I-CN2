package com.auction.server.socket.handler;

import com.auction.server.socket.room.AuctionRoomManager;
import com.auction.server.service.bid.BidService;
import com.auction.shared.constant.SocketEventConstants;
import com.auction.shared.exception.AuctionException;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

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

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error initializing ClientHandler streams", e);
        }
    }

    public synchronized void sendMessage(String jsonMessage) {
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
                if (request == null || request.getAction() == null) {
                    LOGGER.warning("Ignoring invalid socket request from " + username);
                    continue;
                }
                ResponseMessage responseMessage = new ResponseMessage();
                String jsonPayload = request.getPayload();
                switch (request.getAction()) {
                    case BID -> {
                        bidAction(jsonPayload, responseMessage);
                        sendIfHasStatus(responseMessage);
                    }
                    case JOIN_ROOM -> joinRoomAction(jsonPayload, responseMessage);
                    case LEAVE_ROOM -> leaveRoomAction();
                    case AUTO_BID_REGISTER -> {
                        autoBidRegisterAction(jsonPayload, responseMessage);
                        sendIfHasStatus(responseMessage);
                    }
                    case GET_AUTO_BID_STATUS -> {
                        getAutoBidStatusAction(jsonPayload, responseMessage);
                        sendIfHasStatus(responseMessage);
                    }
                    case CANCEL_AUTO_BID -> {
                        cancelAutoBidAction(jsonPayload, responseMessage);
                        sendIfHasStatus(responseMessage);
                    }
                    default -> {

                    }
                }

            }
        } catch (IOException e) {
            LOGGER.info("Client disconnected: " + username);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unexpected socket handler error for client " + username, e);
        } finally {
            closeResources();
        }
    }

    private void sendIfHasStatus(ResponseMessage responseMessage) {
        if (responseMessage.getStatus() != null) {
            sendMessage(gson.toJson(responseMessage));
        }
    }

    private void closeResources() {
        try {
            com.auction.server.MainServer.activeClients.remove(this);
            roomManager.removeClientFromAllRooms(this);
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error closing ClientHandler resources", e);
        } finally {
            in = null;
            out = null;
            clientSocket = null;
        }
    }

    private void joinRoomAction(String jsonPayload, ResponseMessage responseMessage) {
        try {
            RoomPayload roomPayload = gson.fromJson(jsonPayload, RoomPayload.class);
            if (roomPayload == null || roomPayload.getItemId() == null || roomPayload.getItemId().isBlank()) {
                responseMessage.setStatus(SocketEventConstants.STATUS_JOIN_ROOM_FAIL);
                responseMessage.setMessage("Invalid room payload");
                sendMessage(gson.toJson(responseMessage));
                return;
            }
            this.username = roomPayload.getToken();
            LOGGER.fine("UserId: " + roomPayload.getToken() + " joining ItemId: " + roomPayload.getItemId());

            if (currentRoomId != null && !currentRoomId.equals(roomPayload.getItemId())) {
                roomManager.leaveRoom(currentRoomId, this);
            }
            currentRoomId = roomPayload.getItemId();
            roomManager.joinRoom(currentRoomId, this);
            responseMessage.setStatus(SocketEventConstants.STATUS_JOIN_ROOM_SUCCESS);
            responseMessage.setMessage("Joined room successfully");
            sendMessage(gson.toJson(responseMessage));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to join room", e);
            responseMessage.setStatus(SocketEventConstants.STATUS_JOIN_ROOM_FAIL);
            responseMessage.setMessage("Failed to join room");
            sendMessage(gson.toJson(responseMessage));
        }
    }

    private void leaveRoomAction() {
        if (currentRoomId != null) {
            roomManager.leaveRoom(currentRoomId, this);
            currentRoomId = null;
        }
    }

    private boolean bidAction(String payload, ResponseMessage response) {
        try {
            BidPayload bidData = gson.fromJson(payload, BidPayload.class);
            LOGGER.fine("Received bid action");

            if (bidData == null || bidData.getItemId() == null || bidData.getUserId() == null || bidData.getBidPrice() == null) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Thông tin bid không hợp lệ: thiếu itemId, userId hoặc bidPrice");
                LOGGER.warning("Invalid bid payload received");
                return true;
            }

            // Kiểm tra giá bid có hợp lệ không
            if (bidData.getBidPrice() <= 0) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Giá bid phải lớn hơn 0");
                LOGGER.warning("Invalid bid price: " + bidData.getBidPrice());
                return true;
            }

            boolean created = bidService.placeBid(
                    bidData.getItemId(),
                    bidData.getUserId(),
                    bidData.getBidPrice()
            );

            if (created) {
                LOGGER.info("Bid placed successfully for item " + bidData.getItemId() + " by user " + bidData.getUserId());
            } else {
                LOGGER.warning("Failed to place bid for item " + bidData.getItemId());
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Không thể đặt giá. Vui lòng kiểm tra giá bid, thời gian phiên, hoặc số dư tài khoản");
            }

            return true;
        } catch (AuctionException e) {
            LOGGER.log(Level.WARNING, "Auction error during bid: " + e.getMessage());
            response.setStatus(SocketEventConstants.STATUS_FAIL);
            response.setMessage("Lỗi đấu giá: " + e.getMessage());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing bid action", e);
            response.setStatus(SocketEventConstants.STATUS_FAIL);
            response.setMessage("Lỗi khi xử lý bid: Vui lòng thử lại sau");
            return true;
        }
    }

    private boolean autoBidRegisterAction(String payload, ResponseMessage response) {
        try {
            AutoBidPayload autoBidData = gson.fromJson(payload, AutoBidPayload.class);

            if (autoBidData == null
                    || autoBidData.getItemId() == null
                    || autoBidData.getUserId() == null
                    || autoBidData.getMaxBid() == null
                    || autoBidData.getIncrement() == null) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Thông tin auto-bid không hợp lệ: thiếu các trường bắt buộc");
                LOGGER.warning("Invalid auto-bid payload received");
                return true;
            }

            // Kiểm tra các tham số
            if (autoBidData.getMaxBid() <= 0 || autoBidData.getIncrement() <= 0) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Giá tối đa và bước tăng phải lớn hơn 0");
                LOGGER.warning("Invalid auto-bid parameters");
                return true;
            }

            boolean created = bidService.registerAutoBidAndMaybeBid(
                    autoBidData.getItemId(),
                    autoBidData.getUserId(),
                    autoBidData.getMaxBid(),
                    autoBidData.getIncrement()
            );

            if (created) {
                response.setStatus(SocketEventConstants.STATUS_SUCCESS);
                response.setMessage("Đăng ký auto-bid thành công");
                LOGGER.info("Auto-bid registered successfully");
            } else {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Không thể đăng ký auto-bid. Vui lòng kiểm tra giá tối đa, phiên đấu giá hoặc số dư");
                LOGGER.warning("Failed to register auto-bid");
            }

            return true;
        } catch (AuctionException e) {
            LOGGER.log(Level.WARNING, "Auction error during auto-bid registration: " + e.getMessage());
            response.setStatus(SocketEventConstants.STATUS_FAIL);
            response.setMessage("Lỗi đấu giá: " + e.getMessage());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing auto-bid register action", e);
            response.setStatus(SocketEventConstants.STATUS_FAIL);
            response.setMessage("Lỗi khi đăng ký auto-bid: Vui lòng thử lại sau");
            return true;
        }
    }

    private boolean getAutoBidStatusAction(String payload, ResponseMessage response) {
        try {
            AutoBidPayload autoBidData = gson.fromJson(payload, AutoBidPayload.class);

            if (autoBidData == null
                    || autoBidData.getItemId() == null
                    || autoBidData.getUserId() == null) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Thông tin không hợp lệ: thiếu itemId hoặc userId");
                LOGGER.warning("Invalid get auto-bid status payload");
                return true;
            }

            AutoBidPayload autoBidStatus = bidService.getAutoBidStatus(
                    autoBidData.getItemId(),
                    autoBidData.getUserId()
            );

            if (autoBidStatus == null) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage(SocketEventConstants.EVENT_AUTO_BID_STATUS);
                LOGGER.warning("Failed to retrieve auto-bid status");
                return true;
            }

            response.setStatus(SocketEventConstants.STATUS_SUCCESS);
            response.setMessage(SocketEventConstants.EVENT_AUTO_BID_STATUS);
            response.setData(autoBidStatus);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing get auto-bid status action", e);
            response.setStatus(SocketEventConstants.STATUS_FAIL);
            response.setMessage("Lỗi khi lấy trạng thái auto-bid: Vui lòng thử lại sau");
            return true;
        }
    }

    private boolean cancelAutoBidAction(String payload, ResponseMessage response) {
        try {
            AutoBidPayload autoBidData = gson.fromJson(payload, AutoBidPayload.class);

            if (autoBidData == null
                    || autoBidData.getItemId() == null
                    || autoBidData.getUserId() == null) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Thông tin không hợp lệ: thiếu itemId hoặc userId");
                LOGGER.warning("Invalid cancel auto-bid payload");
                return true;
            }

            LOGGER.info(String.format("[AUTO_BID_CANCEL][SERVER_RECEIVE] time=%s itemId=%s userId=%s",
                    LocalDateTime.now(), autoBidData.getItemId(), autoBidData.getUserId()));

            boolean cancelled = bidService.cancelAutoBid(
                    autoBidData.getItemId(),
                    autoBidData.getUserId()
            );

            if (!cancelled) {
                response.setStatus(SocketEventConstants.STATUS_FAIL);
                response.setMessage("Không thể hủy auto-bid");
                LOGGER.warning("Failed to cancel auto-bid");
                return true;
            }

            response.setStatus(SocketEventConstants.STATUS_SUCCESS);
            response.setMessage(SocketEventConstants.EVENT_AUTO_BID_CANCELLED);
            response.setData(new AutoBidPayload(
                    autoBidData.getItemId(),
                    autoBidData.getUserId(),
                    null,
                    null,
                    false
            ));
            LOGGER.info("Auto-bid cancelled successfully");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing cancel auto-bid action", e);
            response.setStatus(SocketEventConstants.STATUS_FAIL);
            response.setMessage("Lỗi khi hủy auto-bid: Vui lòng thử lại sau");
            return true;
        }
    }

    public String getUsername() {
        return username;
    }
}
