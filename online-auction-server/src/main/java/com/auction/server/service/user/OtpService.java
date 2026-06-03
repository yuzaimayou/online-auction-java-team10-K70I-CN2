package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class OtpService {
    private static OtpService instance;
    private final EmailService emailService;
    private final UserRepository userRepository;

    private OtpService() {
        this(EmailService.getInstance(), new UserRepository());
    }

    OtpService(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    private static class OtpInfo {
        private String otp;
        private LocalDateTime expiryTime;

        public OtpInfo(String otp) {
            this.otp = otp;
            this.expiryTime = LocalDateTime.now().plusMinutes(15);
        }

        public String getOtp() {
            return otp;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    public static synchronized OtpService getInstance() {
        if (instance == null) {
            instance = new OtpService();
        }
        return instance;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpInfo info = otpStorage.get(email);

        if (info == null) {
            return false;
        }
        if (info.isExpired()) {
            otpStorage.remove(email);
            return false;
        }
        if (!info.getOtp().equals(otp)) {
            return false;
        }

        otpStorage.remove(email);
        userRepository.enableUser(email);
        return true;
    }

    private static final ConcurrentHashMap<String, OtpInfo> otpStorage = new ConcurrentHashMap<>();

    public void sendOtpEmail(String email) {
        OtpInfo info = generateOtp();
        otpStorage.put(email, info);

        String htmlContent = buildOtpHtml(info.getOtp());

        emailService.sendHtmlEmail(
                email,
                "Your OTP Code",
                htmlContent
        );
    }

    private static OtpInfo generateOtp() {
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        return new OtpInfo(otp);
    }

    private String buildOtpHtml(String otp) {
        return """
                <div style='font-family: Arial, sans-serif; background-color: #f3f4f6; padding: 40px 20px;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 12px;'>
                        <h2 style='color: #1e3a8a; text-align: center;'>ONLINE AUCTION</h2>
                        <p>Hello,</p>
                        <p>You are registering a new account. Please use the OTP below:</p>
                        <div style='text-align: center; margin: 40px 0;'>
                            <span style='font-size: 36px; font-weight: bold; color: #2563eb; letter-spacing: 8px;'>
                                %s
                            </span>
                        </div>
                        <p style='color: #ef4444; text-align: center;'>This code is valid for <b>15 minutes</b>.</p>
                    </div>
                </div>
                """.formatted(otp);
    }
}
