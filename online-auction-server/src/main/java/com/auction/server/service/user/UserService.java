package com.auction.server.service.user;

import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.sql.SQLException;

public class UserService {
    private final UserRepository userRepository = new UserRepository();
    private final BidRepository bidRepository = new BidRepository();

    public boolean banUser(String adminId, String targetUserId) {
        if (adminId != null && adminId.equals(targetUserId)) {
            System.out.println("Cannot ban yourself");
            return false;
        }

        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            System.out.println("Target user not found");
            return false;
        }

        if ("Admin".equalsIgnoreCase(targetUser.getRole())) {
            System.out.println("Cannot ban an admin");
            return false;
        }

        if ("banned_user".equalsIgnoreCase(targetUser.getRole())) {
            return true; // no-op
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
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
