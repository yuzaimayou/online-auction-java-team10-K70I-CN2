package com.auction.server.http.handler;

import com.auction.server.service.auction.AuctionSettlementService;
import com.auction.server.service.wallet.WalletService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// HTTP handler cho các thao tác ví và thanh toán đấu giá.
//
// Endpoint:
//   POST /api/wallet/deposit  — nạp tiền vào tài khoản
//   POST /api/auction/settle  — thanh toán khi phiên đấu giá kết thúc
public class WalletHandler {

    private static final Gson GSON = new Gson();

    // POST /api/wallet/deposit
    // Body: { "userId": "...", "amount": 500.0 }
    public static class DepositHandler implements HttpHandler {

        private final WalletService walletService;

        public DepositHandler() {
            this(new WalletService());
        }

        DepositHandler(WalletService walletService) {
            this.walletService = walletService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, error("Chỉ hỗ trợ POST"));
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject req = GSON.fromJson(body, JsonObject.class);

                String userId = req.has("userId") ? req.get("userId").getAsString() : null;
                double amount  = req.has("amount")  ? req.get("amount").getAsDouble()  : 0;

                if (userId == null || userId.isBlank() || amount <= 0) {
                    sendResponse(exchange, 400, error("Cần userId và amount > 0"));
                    return;
                }

                boolean ok = walletService.deposit(userId, amount);
                if (ok) {
                    double[] balances = walletService.getBalance(userId);
                    JsonObject resp = new JsonObject();
                    resp.addProperty("status", "SUCCESS");
                    resp.addProperty("message", "Nạp " + amount + " vào tài khoản " + userId);
                    if (balances != null) {
                        JsonObject data = new JsonObject();
                        data.addProperty("balance", balances[0]);
                        data.addProperty("frozenBalance", balances[1]);
                        resp.add("data", data);
                    }
                    sendResponse(exchange, 200, resp.toString());
                } else {
                    sendResponse(exchange, 400, error("Nạp tiền thất bại — không tìm thấy auth"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, error("Lỗi server: " + e.getMessage()));
            }
        }
    }

    // POST /api/auction/settle
    // Body: { "itemId": "..." }
    public static class SettleHandler implements HttpHandler {

        private final AuctionSettlementService settlementService;

        public SettleHandler() {
            this(new AuctionSettlementService());
        }

        SettleHandler(AuctionSettlementService settlementService) {
            this.settlementService = settlementService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, error("Chỉ hỗ trợ POST"));
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject req = GSON.fromJson(body, JsonObject.class);

                String itemId = req.has("itemId") ? req.get("itemId").getAsString() : null;
                if (itemId == null || itemId.isBlank()) {
                    sendResponse(exchange, 400, error("Cần itemId"));
                    return;
                }

                AuctionSettlementService.SettlementResult result =
                        settlementService.settleAuction(itemId);

                JsonObject resp = new JsonObject();
                if (result.success) {
                    resp.addProperty("status", "SUCCESS");
                    resp.addProperty("hadBids", result.hadBids);
                    if (result.hadBids) {
                        resp.addProperty("winnerId",     result.winnerId);
                        resp.addProperty("sellerId",     result.sellerId);
                        resp.addProperty("winningPrice", result.winningPrice);
                    }
                    sendResponse(exchange, 200, resp.toString());
                } else {
                    resp.addProperty("status", "FAIL");
                    resp.addProperty("message", result.errorMessage);
                    sendResponse(exchange, 400, resp.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, error("Lỗi server: " + e.getMessage()));
            }
        }
    }
    // GET /api/wallet/balance?userId=...
    public static class BalanceHandler implements HttpHandler {

        private final WalletService walletService = new WalletService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, error("Chỉ hỗ trợ GET"));
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String userId = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("userId=")) {
                            userId = param.substring("userId=".length());
                            break;
                        }
                    }
                }

                if (userId == null || userId.isBlank()) {
                    sendResponse(exchange, 400, error("Cần userId"));
                    return;
                }

                double[] balances = walletService.getBalance(userId);
                if (balances == null) {
                    sendResponse(exchange, 404, error("Không tìm thấy user: " + userId));
                    return;
                }

                JsonObject data = new JsonObject();
                data.addProperty("balance",       balances[0]);
                data.addProperty("frozenBalance", balances[1]);

                JsonObject resp = new JsonObject();
                resp.addProperty("status",  "success");
                resp.addProperty("message", "OK");
                resp.add("data", data);

                sendResponse(exchange, 200, resp.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, error("Lỗi server: " + e.getMessage()));
            }
        }
    }

    // Đọc body từ request
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Gửi JSON response
    private static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Tạo JSON lỗi
    private static String error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("status", "ERROR");
        obj.addProperty("message", message);
        return obj.toString();
    }
}
