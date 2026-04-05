package com.auction.server;

import com.auction.server.controller.ClientHandler;
import com.auction.server.database.DatabaseInit;
import com.auction.server.service.AuthService;
import com.auction.server.service.ProductService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    private static final int PORT = 8000;

    public static void main(String[] agrs) {
        // tao database
        DatabaseInit.init();
        //Khoi tao cac service cot loi
        AuthService authService = new AuthService();
        ProductService productService = new ProductService();
        authService.register("admin", "admin");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            //server load image
            StaticFileServer fileServer = new StaticFileServer();
            fileServer.startServer();

            System.out.println("Port has opened!");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Client has connected with IP: %s%n", clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, authService, productService);
                Thread clientThread = new Thread(handler);

                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }
}
