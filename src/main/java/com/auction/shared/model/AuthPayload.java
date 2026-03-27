package com.auction.shared.model;

public class AuthPayload {
    public String username,password;

    public AuthPayload(String username, String password){
        this.username=username;
        this.password=password;
    }
    public String getUsername(){return username;}
    public String getPassword(){return password;}

    public void setUsername(String username){this.username=username;}
    public void setPassword(String password){this.password=password;}

}
