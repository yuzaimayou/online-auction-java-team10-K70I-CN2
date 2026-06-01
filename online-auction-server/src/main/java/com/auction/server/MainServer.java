package com.auction.server;

import com.auction.server.config.AppConfig;
import com.auction.server.socket.handler.ClientHandler;
import com.auction.server.http.handler.*;
import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;
import com.auction.server.service.auction.AuctionSchedulerService;
import com.auction.server.service.item.ItemService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

public class MainServer {
    public static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public static void main(String[] agrs) {
        AppConfig.initFolders();
        // tao database
        DatabaseManager.init();
        DatabaseInit.init();

        // Start auction scheduler: polls every 5 s to transition
        // UPCOMING -> ONGOING and ONGOING -> ENDED in the database.
        new AuctionSchedulerService(null).start();
        System.out.println("Auction scheduler started.");

        // start http server
        try {
            System.out.println("Starting HTTP server...");
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            httpServer.createContext("/api/login", new LoginHandler());
            httpServer.createContext("/api/register", new RegisterHandler());
            httpServer.createContext("/api/verify-account", new VerifyHandler());
            httpServer.createContext("/api/send-otp", new SendOtp());
            httpServer.createContext("/api/items", new ItemsHandler());
            httpServer.createContext("/api/items/ban", new BanItemHandler());
            httpServer.createContext("/api/users/ban", new BanUserHandler());
            httpServer.createContext("/api/users", new GetUsersHandler());
            httpServer.createContext("/api/items/", new ItemDetailHandler());
            httpServer.createContext("/api/history/", new HistoryHandler());
            httpServer.createContext("/api/mybids", new MyBidsHandler());
            // ngix da xu ly viec /images
            // httpServer.createContext("/images", new StaticFileServer.ImageHandler());
            // Wallet & settlement endpoints
            httpServer.createContext("/api/wallet/deposit", new WalletHandler.DepositHandler());
            httpServer.createContext("/api/auction/settle", new WalletHandler.SettleHandler());
            httpServer.createContext("/api/wallet/balance", new WalletHandler.BalanceHandler());

            httpServer.setExecutor(Executors.newFixedThreadPool(50));
            httpServer.start();
            System.out.println("HTTP server started on port 8080");
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // start socket server
        try (ServerSocket serverSocket = new ServerSocket(9090)) {
            System.out.println("Starting Socket server...");
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
