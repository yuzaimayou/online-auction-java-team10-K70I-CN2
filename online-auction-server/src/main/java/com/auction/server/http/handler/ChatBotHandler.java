package com.auction.server.http.handler;

import com.auction.server.http.response.HttpResponseUtil;
import com.auction.server.service.chatbot.ChatbotService;
import com.auction.shared.message.AIResponseData;
import com.auction.shared.message.ResponseMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.util.Scanner;

public class ChatBotHandler implements HttpHandler {
    private ChatbotService chatbotService = ChatbotService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {
        String method = exchange.getRequestMethod();
        ResponseMessage responseMessage = new ResponseMessage();
        System.out.println("Received " + method + " request for ChatBotHandler");
        switch (method) {
            case "POST" -> {

                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                Scanner scanner = new Scanner(isr).useDelimiter("\\A");
                String userMessage = scanner.hasNext() ? scanner.next() : "";
                System.out.println("Received message from client: " + userMessage);
                AIResponseData data = chatbotService.handlerMessage(userMessage);
                System.out.println("Generated chatbot response: " + data.getAiResponse());
                responseMessage.setStatus("success");
                responseMessage.setMessage("Chatbot response generated successfully");
                responseMessage.setData(data);
                HttpResponseUtil.sendMessage(exchange, 200, responseMessage);


            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

}
