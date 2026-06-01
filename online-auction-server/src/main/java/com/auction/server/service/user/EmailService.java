package com.auction.server.service.user;

import com.auction.server.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static EmailService instance;

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private final String username = firstNonBlank(System.getenv("MAIL_USERNAME"), dotenv.get("MAIL_USERNAME"));
    private final String password = firstNonBlank(System.getenv("MAIL_PASSWORD"), dotenv.get("MAIL_PASSWORD"));
    private final Session session;

    private UserRepository userRepository = new UserRepository();


    private EmailService() {
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

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Mail credentials are not configured.");
        }

        try {
            Message message = new MimeMessage(this.session);

            message.setFrom(new InternetAddress(
                    "ducanhng0401@gmail.com",
                    "Online Auction Team 10 - No Reply"
            ));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            try (Transport transport = this.session.getTransport("smtp")) {
                transport.connect("smtp-relay.brevo.com", username, password);
                transport.sendMessage(message, message.getAllRecipients());
            }

            LOGGER.info("Email sent successfully to " + to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email to " + to, e);
            throw new RuntimeException("Failed to send email.", e);
        }
    }


}
