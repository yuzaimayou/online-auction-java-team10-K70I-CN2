package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class UserRepository {

    // Tạo auth mới với số dư mặc định
    public boolean createUser(String username, String password, String role, String email) {

        String sql = """
                INSERT INTO users(
                    id,
                    username,
                    password,
                    role,
                    email,
                    balance,
                    frozen_balance
                ) VALUES(?,?,?,?,?,?,?)
                """;

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.setString(5, email);

            // Số dư mặc định
            stmt.setDouble(6, 10000);
            // Chưa có tiền bị khóa
            stmt.setDouble(7, 0);

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enableUser(String email) {

        String sql = "UPDATE users SET isVerify = true WHERE email = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, email);

            int rowsUpdated = stmt.executeUpdate();

            return rowsUpdated > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Tìm auth theo id (dùng trong transaction ví)
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
            e.printStackTrace();
        }
        return null;
    }

    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id=?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private User mapRow(ResultSet rs) throws Exception {
        String role = rs.getString("role"); // Đọc role từ DB

        User user;

        if ("Admin".equalsIgnoreCase(role)) {
            Admin admin = new Admin(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getBoolean("isVerify")
            );
            user = admin;
        } else {
            user = new User(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getBoolean("isVerify")
            );
        }

        user.setBalance(rs.getDouble("balance"));
        user.setFrozenBalance(rs.getDouble("frozen_balance"));
        if (role != null) {
            user.setRole(role);
        }

        return user;
    }
}
