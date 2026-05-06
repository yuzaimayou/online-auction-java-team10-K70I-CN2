package com.auction.server;

import com.auction.server.controller.ClientHandler;
import com.auction.server.controller.api.*;
import com.auction.server.database.DatabaseInit;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainServer {
    private static final int PORT = 8000;

    public static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public static void main(String[] agrs) {
        // tao database
        DatabaseInit.init();
        //start http server
        try {
            System.out.println("Starting HTTP server...");
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            httpServer.createContext("/api/login", new LoginHandler());
            httpServer.createContext("/api/register", new RegisterHandler());
            httpServer.createContext("/api/verify-account", new VerifyHandler());
            httpServer.createContext("/api/send-otp", new SendOtp());
            httpServer.createContext("/api/product", new ProductHandler());
            httpServer.createContext("/api/products", new GetDataProducts());
            httpServer.createContext("/images", new StaticFileServer.ImageHandler());
            // Wallet & settlement endpoints
            httpServer.createContext("/api/wallet/deposit", new WalletHandler.DepositHandler());
            httpServer.createContext("/api/auction/settle", new WalletHandler.SettleHandler());

            httpServer.setExecutor(null);
            httpServer.start();
            System.out.println("HTTP server started on port 8080");
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        //start socket server
        try {
            System.out.println("Starting Socket server...");

            ServerSocket serverSocket = new ServerSocket(9090);
            System.out.println("Socket server started on port 9090");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Client has connected with IP: %s%n", clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                activeClients.add(handler);

                Thread clientThread = new Thread(handler);

                clientThread.start();
            }
        } catch (Exception e) {
            System.err.println("Failed to start Socket server: " + e.getMessage());
            e.printStackTrace();

        }

    }
}
