package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());

    // Tạo tài khoản người dùng mới trong bảng users.
    public boolean createUser(String username, String password, String role, String email) {
        String sql = """
                INSERT INTO users(
                    id,
                    username,
                    password,
                    role,
                    email,
                    status,
                    balance,
                    frozen_balance
                ) VALUES(?,?,?,?,?,?,?,?)
                """;
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.setString(5, email);
            stmt.setString(6, "Active");
            stmt.setDouble(7, 10000);
            stmt.setDouble(8, 0);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create user " + username, e);
            return false;
        }
    }

    // Xác thực tài khoản người dùng theo email.
    public boolean enableUser(String email) {

        String sql = "UPDATE users SET isVerify = true WHERE email = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);

            int rowsUpdated = stmt.executeUpdate();

            return rowsUpdated > 0;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to enable user for email " + email, e);
            return false;
        }
    }

    // Tìm người dùng theo id bằng connection có sẵn.
    public User findById(Connection conn, String userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find user by id " + userId, e);
        }
        return null;
    }

    // Tìm người dùng theo id bằng connection riêng.
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id=?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find user by id " + id, e);
        }
        return null;
    }

    // Tìm người dùng theo email.
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find user by email " + email, e);
        }
        return null;
    }

    // Tìm người dùng theo username.
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find user by username " + username, e);
        }
        return null;
    }

    // Chuyển một dòng ResultSet thành đối tượng User hoặc Admin.
    private User mapRow(ResultSet rs) throws Exception {
        String role = rs.getString("role");

        User user;

        if ("Admin".equalsIgnoreCase(role)) {
            user = new Admin(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getBoolean("isVerify"));
        } else {
            user = new User(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getBoolean("isVerify"));
        }

        user.setBalance(rs.getDouble("balance"));
        user.setFrozenBalance(rs.getDouble("frozen_balance"));

        if (role != null) {
            user.setRole(role);
        }

        try {
            String status = rs.getString("status");
            user.setStatus(status != null ? status : "Active");
        } catch (Exception ignored) {
            user.setStatus("Active");
        }

        return user;
    }

    // Cập nhật role của người dùng bằng connection riêng.
    public boolean updateRole(String userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update role for user " + userId, e);
            return false;
        }
    }

    // Cập nhật role của người dùng trong transaction hiện tại.
    public boolean updateRole(Connection conn, String userId, String newRole) throws Exception {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Cập nhật trạng thái của người dùng trong transaction hiện tại.
    public boolean updateStatus(Connection conn, String userId, String newStatus) throws Exception {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}
