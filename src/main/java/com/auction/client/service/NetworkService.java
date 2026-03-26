package com.auction.client.service;

import com.auction.shared.constant.ActionType;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class NetworkService {
    private static NetworkService instance;
    private final String serverIP="127.0.0.1";
    private final int serverPort=8000;

    private Gson gson=new Gson();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private NetworkService(){
        try{
            socket=new Socket(serverIP,serverPort);
            out=new PrintWriter(socket.getOutputStream(),true);
            in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch(IOException e){
            e.printStackTrace();
            System.err.println("Unable to connect to the server");
        }
    }

    public static NetworkService getInstance(){
        if(instance==null){
            instance=new NetworkService();
        }
        return instance;
    }

    public ResponseMessage sendRequest(RequestMessage req){

        if (socket==null || socket.isClosed()){
            return new ResponseMessage("ERROR","Unable to connect to the server",null);
        }

        try {
            //Convert request thanh json va gui di
            String jsonRequest=gson.toJson(req);
            out.println(jsonRequest);
            System.out.println("Client was sent message: "+jsonRequest);

            //cho va doc phan hoi
            String jsonRes=in.readLine();
            if (jsonRes==null){
                return new ResponseMessage("ERROR","Server was closed",null);
            }
            System.out.println("Client was received message");

            return gson.fromJson(jsonRes,ResponseMessage.class);
        }
        catch (IOException e){
            e.printStackTrace();
            return new ResponseMessage("ERROR","Unable to connect to the server",null);
        }
    }

}
