package com.auction.client.util;

import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

public class UserSession {
    private static volatile UserSession instance;
    private User loggedInUser;
    private final List<Runnable> listeners = new ArrayList<>();

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }
    public void addListener(Runnable r) {
        listeners.add(r);
    }

    private void notifyChanged() {
        for (Runnable r : new ArrayList<>(listeners)) {
            Platform.runLater(r); // 🔥 QUAN TRỌNG NHẤT
        }
    }
    public void setLoggedInUser(User user) {
        loggedInUser = user;
        notifyChanged();
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }
    public String getCurrentUserId() {
        return loggedInUser != null ? loggedInUser.getId() : null;
    }

    public boolean isAdmin() {
        return loggedInUser instanceof Admin;
    }

    /**
     * Trả về Admin object nếu người đăng nhập là Admin, null nếu không phải.
     * Dùng khi cần gọi Admin-specific actions (deleteUser, suspendUser...).
     */
    public Admin getAsAdmin() {
        if (loggedInUser instanceof Admin admin) return admin;
        return null;
    }

    public void cleanUserSession() {
        loggedInUser = null;
    }
}