package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VerifyService {
    private static final Logger LOGGER = Logger.getLogger(VerifyService.class.getName());
    private static VerifyService instance;

    private static final Dotenv dotenv = Dotenv.load();
    private final String username = dotenv.get("MAIL_USERNAME");
    private final String password = dotenv.get("MAIL_PASSWORD");
    private final Session session;
    private UserRepository userRepository = new UserRepository();

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

    private static final ConcurrentHashMap<String, OtpInfo> otpStorage = new ConcurrentHashMap<>();

    private VerifyService() {
        LOGGER.info("==== KIEM TRA DOC FILE .ENV ====");
        LOGGER.info("Mail User: " + (username == null ? "NULL" : username));
        LOGGER.info("Mail Pass: " + (password == null ? "NULL" : "found password"));
        LOGGER.info("================================");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        props.put("mail.smtp.host", "smtp-relay.brevo.com");
        props.put("mail.smtp.port", "2525");

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        LOGGER.info("Email session initialized successfully.");
    }

    public static VerifyService getInstance() {
        if (instance == null) {
            instance = new VerifyService();
        }
        return instance;
    }

    private static OtpInfo generateOtp() {
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        return new OtpInfo(otp);
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

    public void sendEmail(String email) {
        OtpInfo info = generateOtp();
        otpStorage.put(email, info);
        LOGGER.fine("Generated OTP for email: " + email);

        Transport transport = null;
        try {
            Message message = new MimeMessage(this.session);
            message.setFrom(new InternetAddress("ducanhng0401@gmail.com", "Online Auction Team 10 - No Reply"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));

            String htmlContent =
                    "<div style='font-family: Arial, sans-serif; background-color: #f3f4f6; padding: 40px 20px;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>" +
                            "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<h2 style='color: #1e3a8a; margin: 0; font-size: 24px;'>ONLINE AUCTION</h2>" +
                            "</div>" +
                            "<p style='color: #4b5563; font-size: 16px; line-height: 1.6;'>Hello,</p>" +
                            "<p style='color: #4b5563; font-size: 16px; line-height: 1.6;'>You are registering a new account. To complete the process, please use the verification code (OTP) below:</p>" +
                            "<div style='text-align: center; margin: 40px 0;'>" +
                            "<span style='display: inline-block; font-size: 36px; font-weight: bold; color: #2563eb; letter-spacing: 8px; padding: 15px 30px; background-color: #eff6ff; border-radius: 8px; border: 2px dashed #93c5fd;'>" +
                            info.getOtp() +
                            "</span>" +
                            "</div>" +
                            "<p style='color: #ef4444; font-size: 14px; text-align: center; margin-bottom: 30px;'>" +
                            "â³ This code is valid for <b>15 minutes</b>." +
                            "</p>" +
                            "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;' />" +
                            "<p style='color: #9ca3af; font-size: 13px; text-align: center; line-height: 1.5;'>" +
                            "If you did not request this code, please ignore this email.<br>Never share your OTP with anyone to protect your account." +
                            "</p>" +
                            "</div>" +
                            "</div>";

            message.setSubject("Your OTP Code");
            message.setContent(htmlContent, "text/html; charset=utf-8");

            transport = this.session.getTransport("smtp");
            transport.connect("smtp-relay.brevo.com", username, password);
            transport.sendMessage(message, message.getAllRecipients());

            LOGGER.info("OTP email sent successfully to " + email);
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send OTP email to " + email, e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Failed to prepare OTP email sender for " + email, e);
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    LOGGER.log(Level.WARNING, "Failed to close SMTP transport for " + email, e);
                }
            }
        }
    }
}
