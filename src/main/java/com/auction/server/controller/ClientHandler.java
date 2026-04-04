package com.auction.server.controller;

import com.auction.server.service.AuthService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.AuthPayload;
import com.auction.shared.model.payloads.ProductPayload;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private AuthService authService;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ClientHandler(Socket socket, AuthService authService) {
        this.clientSocket = socket;
        this.authService = authService;
    }

    private boolean loginAction(String payload, ResponseMessage response) {
        AuthPayload authData = gson.fromJson(payload, AuthPayload.class);
        String username = authData.getUsername();
        String password = authData.getPassword();
        System.out.println(username + ',' + password);

        User loggedInuser = authService.login(username, password);
        if (loggedInuser != null) {
            response.setStatus("SUCCESS");
            response.setMessage("Log in successful");
            response.setData(gson.toJson(loggedInuser));
            System.out.println("success");
            return true;
        } else {
            System.out.println("Incorrect");
            response.setStatus("FAIL");
            response.setMessage("Incorrect username or password");
            return false;
        }

    }

    private boolean registerAction(String payload, ResponseMessage response) {
        AuthPayload authData = gson.fromJson(payload, AuthPayload.class);
        String username = authData.getUsername();
        String password = authData.getPassword();


        boolean created = authService.register(username, password);

        if (created) {
            response.setStatus("SUCCESS");
            response.setMessage("Register successful");
        } else {
            response.setStatus("FAIL");
            response.setMessage("Username already exists");
        }
        return false;
    }

    private boolean addProductAction(String payload, ResponseMessage response) {
        ProductPayload productData = gson.fromJson(payload, ProductPayload.class);
        String productName = productData.getProductName();
        String category = productData.getCategory();
        LocalDateTime startTime = productData.getStartDateTime();
        LocalDateTime endTime = productData.getEndDateTime();
        String productDesc = productData.getProductDesc();
        String productImg = productData.getProductImg();
        Double initPrice = productData.getInitPrice();
        Double bidStep = productData.getBidStep();
        Double maxPrice = productData.getMaxPrice();
        Double minPrice = productData.getMinPrice();
        String userId = productData.getUserId();
        //goi class tao product
        boolean created = true;

        if (created) {
            response.setStatus("SUCCESS");
            response.setMessage("Product added successfully!" + userId);
        } else {
            response.setStatus("FAIL");
            response.setMessage("Failed to add product. Please try again.");
        }
        return true;
    }

    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(15000);
        } catch (Exception e) {
            System.err.println("Loi set timeout");
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                RequestMessage request = gson.fromJson(jsonRequest, RequestMessage.class);
                ResponseMessage response = new ResponseMessage();
                boolean keepConnectionAlive = true;
                System.out.println("Kiem tra action");
                switch (request.getAction()) {
                    case LOGIN -> keepConnectionAlive = loginAction(request.getPayload(), response);
                    case REGISTER -> keepConnectionAlive = registerAction(request.getPayload(), response);
                    case ADDPRODUCT -> keepConnectionAlive = addProductAction(request.getPayload(), response);
                    default -> {
                        response.setStatus("ERROR");
                        response.setMessage("Invalid action");
                        keepConnectionAlive = false;
                    }
                }
                String jsonResponse = gson.toJson(response);

                out.println(jsonResponse);
                System.out.println("Server was sent: " + jsonResponse);
                if (!keepConnectionAlive) {
                    System.out.println("Ngắt kết nối với client.");

                    break;
                } else {
                    clientSocket.setSoTimeout(0);
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
