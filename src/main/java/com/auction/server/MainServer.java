package com.auction.server;

import com.auction.server.controller.ClientHandler;
import com.auction.server.service.AuthService;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class MainServer {
    private static final int PORT=8000;

    public static void main(String[] agrs){
        //Khoi tao cac service cot loi
        AuthService authService=new AuthService();

        try (ServerSocket serverSocket=new ServerSocket(PORT)){
            System.out.println("Port has opened!");
            while(true){
                Socket clientSocket=serverSocket.accept();
                System.out.printf("Client has connected with IP: %s%n",clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler=new ClientHandler(clientSocket,authService);
                Thread clientThread= new Thread(handler);

                clientThread.start();
            }
        }catch (IOException e){
            System.err.println(e.getMessage());
        }

    }
}
