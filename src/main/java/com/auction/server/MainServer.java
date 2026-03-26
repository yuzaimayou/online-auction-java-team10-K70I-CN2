package com.auction.server;

import com.auction.server.controller.ClientHandler;
import com.auction.server.service.AuthService;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    public static void main(String[] args) {
        AuthService authService = new AuthService();
        int port = 8000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port + "...");

            while (true) {
                Socket socket = serverSocket.accept(); // Đợi có máy Client kết nối
                System.out.println("New client connected!");

                new Thread(new ClientHandler(socket, authService)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}