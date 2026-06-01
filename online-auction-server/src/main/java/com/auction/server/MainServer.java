package com.auction.server;

import com.auction.server.config.AppConfig;
import com.auction.server.socket.handler.ClientHandler;
import com.auction.server.http.handler.*;
import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;
import com.auction.server.service.auction.AuctionSchedulerService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainServer {
    private static final Logger LOGGER = Logger.getLogger(MainServer.class.getName());
    public static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public static void main(String[] agrs) {
        AppConfig.initFolders();
        DatabaseManager.init();
        DatabaseInit.init();

        AuctionSchedulerService auctionScheduler = new AuctionSchedulerService();
        auctionScheduler.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            auctionScheduler.shutdown();
            DatabaseManager.shutdown();
        }));
        LOGGER.info("Auction scheduler started.");

        try {
            LOGGER.info("Starting HTTP server...");
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            registerHttpRoutes(httpServer);
            ExecutorService httpExecutor = Executors.newFixedThreadPool(50);
            httpServer.setExecutor(httpExecutor);
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                httpServer.stop(0);
                httpExecutor.shutdown();
            }));
            LOGGER.info("HTTP server started on port 8080");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start HTTP server: " + e.getMessage(), e);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(9090)) {
            LOGGER.info("Starting Socket server...");
            LOGGER.info("Socket server started on port 9090");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info(String.format("Client has connected with IP: %s", clientSocket.getInetAddress().getHostAddress()));

                ClientHandler handler = new ClientHandler(clientSocket);
                activeClients.add(handler);

                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start Socket server: " + e.getMessage(), e);
        }

    }

    private static void registerHttpRoutes(HttpServer httpServer) {
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
        httpServer.createContext("/api/wallet/deposit", new WalletHandler.DepositHandler());
        httpServer.createContext("/api/auction/settle", new WalletHandler.SettleHandler());
        httpServer.createContext("/api/wallet/balance", new WalletHandler.BalanceHandler());
    }
}
