package com.auction.server.http.handler;

import com.auction.server.service.auction.AuctionSettlementService;
import com.auction.server.service.wallet.WalletService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletHandlerTest {

    private HttpExchange exchange;
    private ByteArrayOutputStream responseBody;
    private int statusCode;

    @BeforeEach
    void setUp() throws Exception {
        exchange = mock(HttpExchange.class);
        responseBody = new ByteArrayOutputStream();
        statusCode = -1;

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(responseBody);
        org.mockito.Mockito.doAnswer(invocation -> {
            statusCode = invocation.getArgument(0);
            return null;
        }).when(exchange).sendResponseHeaders(org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyLong());
    }

    @Test
    void deposit_returns_success_and_balance_when_service_accepts_deposit() throws Exception {
        WalletService walletService = mock(WalletService.class);
        when(walletService.deposit("user-1", 250.0)).thenReturn(true);
        when(walletService.getBalance("user-1")).thenReturn(new double[]{1000.0, 75.0});
        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler(walletService);

        request("POST", "{\"userId\":\"user-1\",\"amount\":250.0}");

        handler.handle(exchange);

        JsonObject json = responseJson();
        assertEquals(200, statusCode);
        assertEquals("SUCCESS", json.get("status").getAsString());
        assertEquals(1000.0, json.getAsJsonObject("data").get("balance").getAsDouble());
        assertEquals(75.0, json.getAsJsonObject("data").get("frozenBalance").getAsDouble());
        verify(walletService).deposit("user-1", 250.0);
    }

    @Test
    void deposit_returns_bad_request_when_required_fields_are_invalid() throws Exception {
        WalletService walletService = mock(WalletService.class);
        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler(walletService);

        request("POST", "{\"userId\":\" \",\"amount\":0}");

        handler.handle(exchange);

        JsonObject json = responseJson();
        assertEquals(400, statusCode);
        assertEquals("ERROR", json.get("status").getAsString());
        assertTrue(json.get("message").getAsString().contains("amount > 0"));
    }

    @Test
    void deposit_rejects_non_post_methods() throws Exception {
        WalletService walletService = mock(WalletService.class);
        WalletHandler.DepositHandler handler = new WalletHandler.DepositHandler(walletService);

        request("GET", "");

        handler.handle(exchange);

        assertEquals(405, statusCode);
        assertEquals("ERROR", responseJson().get("status").getAsString());
    }

    @Test
    void settle_returns_winner_data_when_settlement_succeeds_with_bids() throws Exception {
        AuctionSettlementService settlementService = mock(AuctionSettlementService.class);
        when(settlementService.settleAuction("item-1")).thenReturn(
                AuctionSettlementService.SettlementResult.success("item-1", "buyer-1", "seller-1", 900.0)
        );
        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler(settlementService);

        request("POST", "{\"itemId\":\"item-1\"}");

        handler.handle(exchange);

        JsonObject json = responseJson();
        assertEquals(200, statusCode);
        assertEquals("SUCCESS", json.get("status").getAsString());
        assertTrue(json.get("hadBids").getAsBoolean());
        assertEquals("buyer-1", json.get("winnerId").getAsString());
        assertEquals("seller-1", json.get("sellerId").getAsString());
        assertEquals(900.0, json.get("winningPrice").getAsDouble());
    }

    @Test
    void settle_returns_success_without_winner_when_auction_has_no_bids() throws Exception {
        AuctionSettlementService settlementService = mock(AuctionSettlementService.class);
        when(settlementService.settleAuction("item-2")).thenReturn(
                AuctionSettlementService.SettlementResult.noBids("item-2")
        );
        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler(settlementService);

        request("POST", "{\"itemId\":\"item-2\"}");

        handler.handle(exchange);

        JsonObject json = responseJson();
        assertEquals(200, statusCode);
        assertEquals("SUCCESS", json.get("status").getAsString());
        assertFalse(json.get("hadBids").getAsBoolean());
        assertFalse(json.has("winnerId"));
    }

    @Test
    void settle_returns_bad_request_when_service_reports_failure() throws Exception {
        AuctionSettlementService settlementService = mock(AuctionSettlementService.class);
        when(settlementService.settleAuction("item-3")).thenReturn(
                AuctionSettlementService.SettlementResult.fail("not ended")
        );
        WalletHandler.SettleHandler handler = new WalletHandler.SettleHandler(settlementService);

        request("POST", "{\"itemId\":\"item-3\"}");

        handler.handle(exchange);

        JsonObject json = responseJson();
        assertEquals(400, statusCode);
        assertEquals("FAIL", json.get("status").getAsString());
        assertEquals("not ended", json.get("message").getAsString());
    }

    @Test
    void balance_returns_bad_request_when_user_id_is_missing() throws Exception {
        WalletHandler.BalanceHandler handler = new WalletHandler.BalanceHandler();

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost/api/wallet/balance"));

        handler.handle(exchange);

        JsonObject json = responseJson();
        assertEquals(400, statusCode);
        assertEquals("ERROR", json.get("status").getAsString());
    }

    private void request(String method, String body) throws Exception {
        when(exchange.getRequestMethod()).thenReturn(method);
        when(exchange.getRequestBody()).thenReturn(
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
        );
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost/api/test"));
    }

    private JsonObject responseJson() {
        return JsonParser.parseString(responseBody()).getAsJsonObject();
    }

    private String responseBody() {
        OutputStream ignored = responseBody;
        return responseBody.toString(StandardCharsets.UTF_8);
    }
}
