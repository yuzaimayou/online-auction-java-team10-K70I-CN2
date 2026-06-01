package com.auction.server.service.user;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService {
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    private final UserRepository userRepository = new UserRepository();
    private final BidRepository bidRepository = new BidRepository();

    public boolean banUser(String adminId, String targetUserId) {
        if (adminId != null && adminId.equals(targetUserId)) {
            LOGGER.info("Cannot ban yourself");
            return false;
        }

        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            LOGGER.info("Target user not found");
            return false;
        }

        if ("Admin".equalsIgnoreCase(targetUser.getRole())) {
            LOGGER.info("Cannot ban an admin");
            return false;
        }

        if ("Suspended".equalsIgnoreCase(targetUser.getStatus())) {
            return true;
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean statusUpdated = userRepository.updateStatus(conn, targetUserId, "Suspended");
                if (!statusUpdated) {
                    conn.rollback();
                    return false;
                }

                bidRepository.deactivateAllAutoBidsForUser(conn, targetUserId);

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to ban user " + targetUserId, e);
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to open connection while banning user " + targetUserId, e);
            return false;
        }
    }
}
