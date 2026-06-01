package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;

import java.util.logging.Logger;

public class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    public enum RegisterResult {
        SUCCESS,
        USERNAME_EXISTS,
        EMAIL_EXISTS,
        FAILED
    }

    private final UserRepository userRepository;
    private final VerifyService verifyService;

    public AuthService() {
        this(new UserRepository(), null);
    }

    AuthService(UserRepository userRepository, VerifyService verifyService) {
        this.userRepository = userRepository;
        this.verifyService = verifyService;
    }

    public RegisterResult register(String username, String password, String email) {
        //  Check cả username lẫn email trùng
        if (userRepository.findByUsername(username) != null) {
            LOGGER.info("Username already exists: " + username);
            return RegisterResult.USERNAME_EXISTS;
        }

        if (userRepository.findByEmail(email) != null) {
            LOGGER.info("Email already registered: " + email);
            return RegisterResult.EMAIL_EXISTS;
        }

        boolean created = userRepository.createUser(username, password, "USER", email);
        if (!created) {
            return RegisterResult.FAILED;
        }

        // Gửi OTP async, không block register flow
        new Thread(() -> {
            VerifyService emailService = verifyService != null ? verifyService : VerifyService.getInstance();
            emailService.sendEmail(email);
            LOGGER.info("Đã gửi email OTP tới: " + email);
        }).start();

        LOGGER.info("Registered successfully: " + username);
        return RegisterResult.SUCCESS;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            LOGGER.info("Incorrect username or password");
            return null;
        }

        if (user.getPassword().equals(password)) {
            LOGGER.info("Log in successfully: " + username);
            return user;
        }

        LOGGER.info("Incorrect username or password");
        return null;
    }
}
