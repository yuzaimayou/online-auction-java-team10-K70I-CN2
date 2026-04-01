package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.model.account.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class UserRepository {

    public boolean createUser(String username, String password, String role) {

        String sql = "INSERT INTO users(id,username,password,role) VALUES(?,?,?,?)";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, role);

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public User findByUsername(String username) {

        String sql = "SELECT * FROM users WHERE username = ?";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                return new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}