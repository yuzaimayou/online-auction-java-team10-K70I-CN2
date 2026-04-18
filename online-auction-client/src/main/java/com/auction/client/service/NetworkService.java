package com.auction.client.service;

import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static com.auction.client.util.AppConfig.ServerIp;
import static com.auction.client.util.AppConfig.ServerPort;

public class NetworkService {
    private static NetworkService instance;


    private Gson gson = new Gson();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private NetworkService() {
        try {
            socket = new Socket(ServerIp, ServerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to the server");
        }
    }

    public boolean connectToServer() {
        try {
            if (socket == null || socket.isClosed()) {

                socket = new Socket(ServerIp, ServerPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to the server");
            return false;
        }


    }

    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
        }
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }


    public ResponseMessage sendRequest(RequestMessage req) {

        if (!connectToServer()) {
            return new ResponseMessage("ERROR", "Unable to connect to the server", null);
        }

        try {
            //Convert request thanh json va gui di
            String jsonRequest = gson.toJson(req);
            out.println(jsonRequest);
            System.out.println("Client was sent message: " + jsonRequest);

            //cho va doc phan hoi
            String jsonRes = in.readLine();
            if (jsonRes == null) {
                System.out.println("Response is null");
                return new ResponseMessage("ERROR", "Server was closed", null);
            }
            System.out.println("Client was received message");
            System.out.println("Close connection");
            closeConnection();

            return gson.fromJson(jsonRes, ResponseMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
            return new ResponseMessage("ERROR", "Unable to connect to the server", null);
        } finally {

        }
    }

}
