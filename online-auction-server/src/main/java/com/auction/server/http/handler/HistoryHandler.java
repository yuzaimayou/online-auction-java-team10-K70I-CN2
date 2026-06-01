package com.auction.server.http.handler;

import com.auction.server.service.bid.BidHistoryService;
import com.auction.server.http.response.HttpResponseUtil;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.dto.BidHistoryItemDTO;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class HistoryHandler implements HttpHandler {
    private BidHistoryService bidHistoryService = BidHistoryService.getInstance();
    private final Gson gson = GsonUtil.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String itemId = path.substring(path.lastIndexOf("/") + 1);
        ResponseMessage responseMessage = new ResponseMessage();
        switch (method) {
            case "GET" -> {
                List<BidHistoryItemDTO> history = bidHistoryService.getHistory(itemId);

                if (history != null) {
                    responseMessage.setStatus("success");
                    responseMessage.setMessage("Get bid history successfully");
                    responseMessage.setData(history);
                    HttpResponseUtil.sendMessage(exchange, 200, responseMessage);
                } else {
                    // Chỉ rơi vào trường hợp này nếu có lỗi thực sự từ Database (ví dụ Exception làm trả về null)
                    responseMessage.setStatus("error");
                    responseMessage.setMessage("Failed to retrieve bid history");
                    HttpResponseUtil.sendMessage(exchange, 500, responseMessage);
                }
                // --- [KẾT THÚC CHỈNH SỬA] ---
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }
}
