package com.auction.server.http.handler;

import com.auction.server.http.response.HttpResponseUtil;
import com.auction.server.service.user.OtpService;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.payloads.VerifyPayload;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;

public class VerifyHandler implements HttpHandler {
    private final OtpService otpService = OtpService.getInstance();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            VerifyPayload verifyData = gson.fromJson(isr, VerifyPayload.class);

            String email = verifyData.getEmail();
            String otp = verifyData.getOtp();

            boolean checked = otpService.verifyOtp(email, otp);
            ResponseMessage response = new ResponseMessage();
            if (checked) {
                response.setStatus("success");
                response.setMessage("Verification successful!");
                HttpResponseUtil.sendMessage(exchange, 200, response);
            } else {
                response.setStatus("error");
                response.setMessage("Invalid OTP or email!");
                HttpResponseUtil.sendMessage(exchange, 400, response);
            }
        } else {
            HttpResponseUtil.sendMessage(exchange, 405, new ResponseMessage("error", "Method Not Allowed", null));
        }
    }
}
