package com.auction.client.util;

import com.auction.shared.model.account.User;

public class UserSession {
    private static UserSession instance;
    private User loggedInUser;

    private UserSession() {
    }


    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setLoggedInUser(User user) {
        loggedInUser = user;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void cleanUserSession() {
        loggedInUser = null;
    }


}
