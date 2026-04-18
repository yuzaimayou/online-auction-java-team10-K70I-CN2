package com.auction.server.service;

import com.auction.shared.model.account.User;
import com.auction.server.repository.UserRepository;

public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public boolean register(String username, String password) {

        User existingUser = userRepository.findByUsername(username);

        if (existingUser != null) {
            System.out.println("The username already exists!");
            return false;
        }

        boolean result = userRepository.createUser(username, password, "USER");

        if (result) {
            System.out.println("Registered successfully!");
        }

        return result;
    }

    public User login(String username, String password) {

        User user = userRepository.findByUsername(username);

        if (user == null) {
            System.out.println("Incorrect username or password");
            return null;
        }

        if (user.getPassword().equals(password)) {
            System.out.println("Log in successfully!");
            return user;
        }

        System.out.println("Incorrect username or password");
        return null;
    }
}