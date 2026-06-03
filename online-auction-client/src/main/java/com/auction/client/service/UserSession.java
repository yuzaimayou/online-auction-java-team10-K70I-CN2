package com.auction.client.service;

import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserSession {

    private static volatile UserSession instance;

    private User loggedInUser;

    private final List<Runnable> listeners =
            new CopyOnWriteArrayList<>();

    private UserSession(){}

    public static UserSession getInstance() {
        if(instance==null){
            synchronized (UserSession.class){
                if(instance==null){
                    instance=new UserSession();
                }
            }
        }
        return instance;
    }

    public void addListener(Runnable listener){
        listeners.add(listener);
    }

    public void removeListener(Runnable listener){
        listeners.remove(listener);
    }

    private void notifyChanged(){

        for(Runnable listener:listeners){
            Platform.runLater(listener);
        }
    }

    public void setLoggedInUser(User user){
        this.loggedInUser=user;
        notifyChanged();
    }

    public User getLoggedInUser(){
        return loggedInUser;
    }

    public String getCurrentUserId(){
        return loggedInUser!=null
                ? loggedInUser.getId()
                : null;
    }

    public boolean isAdmin(){
        return loggedInUser instanceof Admin;
    }

    public Admin getAsAdmin(){
        return loggedInUser instanceof Admin admin
                ? admin
                : null;
    }

    public void cleanUserSession(){
        loggedInUser=null;
        notifyChanged();
    }
}