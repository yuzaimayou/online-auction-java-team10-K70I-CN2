package com.auction.client.service;

import com.auction.shared.constant.ActionType;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class NetworkService {
    private final String serverIP="127.0.0.1";
    private final int serverPort=8000;
    private Gson gson=new Gson();

    public ResponseMessage sendRegisterMessage(String username, String password, String role) {
        // Tạo payload theo format
        String payload = username + "," + password + "," + role;
        RequestMessage req = new RequestMessage(ActionType.REGISTER, payload);

        return executeRequest(req);
    }

    public ResponseMessage sendLoginMessage(String username,String password) {
        String payload = username + "," + password;
        RequestMessage req = new RequestMessage(ActionType.LOGIN, payload);
        return executeRequest(req);
    }

    private ResponseMessage executeRequest(RequestMessage req) {
        try (
            Socket socket = new Socket(serverIP, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ){
            String jsonRequest=gson.toJson(req);
            out.println(jsonRequest);
            System.out.println("Client was sent message: "+jsonRequest);

            String jsonRes=in.readLine();
            System.out.println("Client was received message");

            return gson.fromJson(jsonRes,ResponseMessage.class);
        }
        catch (IOException e){
            e.printStackTrace();
            return new ResponseMessage("ERROR","Unable to connect to the server",null);
        }
    }
}
