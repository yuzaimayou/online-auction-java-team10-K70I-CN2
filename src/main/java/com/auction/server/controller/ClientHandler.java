package com.auction.server.controller;

import com.auction.server.repository.ItemRepository;
import com.auction.server.service.AuthService;
import com.auction.server.service.ProductService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.AuthPayload;
import com.auction.shared.model.payloads.ProductPayload;
import com.auction.shared.model.product.Item;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private AuthService authService;
    private ProductService productService;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ClientHandler(Socket socket, AuthService authService, ProductService productService) {
        this.clientSocket = socket;
        this.authService = authService;
        this.productService = productService;
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
        String userId = productData.getUserId();
        //goi class tao product
        boolean created = productService.addProduct(payload);

        if (created) {
            response.setStatus("SUCCESS");
            response.setMessage("Product added successfully!" + userId);
        } else {
            response.setStatus("FAIL");
            response.setMessage("Failed to add product. Please try again.");
        }
        return true;
    }

    private boolean getDataItems(ResponseMessage response) {
        System.out.println("Dang goi den database");
        try {

            ItemRepository itemRepository = new ItemRepository();
            List<Item> payload = itemRepository.findAllItems();
            String jsonPayload = gson.toJson(payload);
            response.setStatus("SUCCESS");
            response.setMessage("Get data items succeed!");
            response.setData(jsonPayload);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus("FAIL");
            response.setMessage("Get data items failed!");
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
                    case GETDATAPRODUCT -> keepConnectionAlive = getDataItems(response);
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
