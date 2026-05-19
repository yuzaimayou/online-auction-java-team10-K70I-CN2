package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;

public class AuthService {

    private final UserRepository userRepository;
    private VerifyService verifyService = VerifyService.getInstance();

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public AuthService(UserRepository userRepository, VerifyService verifyService) {
        this.userRepository = userRepository;
        this.verifyService = verifyService;
    }

    public boolean register(String username, String password, String email) {

        User existingUsername = userRepository.findByUsername(username);


        if (existingUsername != null) {
            System.out.println("The username already exists!");
            return false;
        }


        new Thread(() -> {
            verifyService.sendEmail(email);
            System.out.println("Da gui email");
        }).start();

        boolean result = userRepository.createUser(username, password, "USER", email);

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