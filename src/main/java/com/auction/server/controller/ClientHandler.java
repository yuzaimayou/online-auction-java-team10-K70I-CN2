package com.auction.server.controller;

import com.auction.server.service.AuthService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.message.ResponseMessage;
import com.google.gson.Gson;
import com.auction.shared.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private AuthService authService;
    private Gson gson= new Gson();

    public ClientHandler(Socket socket, AuthService authService){
        this.clientSocket=socket;
        this.authService=authService;
    }
    private void loginAction(String payload,ResponseMessage response){
        String[] credentials=payload.split(",");
        String username=credentials[0];
        String password=credentials[1];

        User loggedInuser=authService.login(username,password);
        if(loggedInuser!=null){
            response.setStatus("SUCCESS");
            response.setMessage("Log in successful");
            response.setData(gson.toJson(loggedInuser));
            System.out.println("success");
        } else{
            System.out.println("Incorrect");
            response.setStatus("FAIL");
            response.setMessage("Incorrect username or password");
        }

    }
    @Override
    public void run(){
        try(BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true)
        ){
            String jsonRequest;
            while ((jsonRequest=in.readLine()) !=null){
                RequestMessage request =gson.fromJson(jsonRequest,RequestMessage.class);
                ResponseMessage response= new ResponseMessage();
                switch (request.getAction()){
                    case LOGIN -> loginAction(request.getPayload(),response);
                    case REGISTER -> {

                    }
                    default -> {
                        System.out.println("Invalid action");
                        response.setStatus("ERROR");
                        response.setMessage("Invalid action");
                    }
                }
                String jsonResponse=gson.toJson(response);
                out.println(jsonResponse);
                System.out.println("Server was sent: "+jsonResponse);
            }

        } catch (IOException e){
            System.out.println("Failed to connect to server: " + e.getMessage());
        }
    }
}
