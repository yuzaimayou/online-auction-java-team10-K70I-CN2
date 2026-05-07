package com.auction.server.repository;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class UserRepository {

    public boolean createUser(String username, String password, String role, String email) {

        String sql = "INSERT INTO users(id,username,password,role,email) VALUES(?,?,?,?,?)";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.setString(5, email);

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
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
            return rowsUpdated > 0; // Return true if at least one row was updated
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User findByEmail(String email) {
        String sqp = "SELECT * FROM users WHERE email=?";
        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqp)
        ) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getBoolean("isVerify")
                );
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

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                return new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getBoolean("isVerify")
                );
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

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                return new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getBoolean("isVerify")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}