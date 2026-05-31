package com.auction.server.repository;

import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest extends RepositoryTestSupport {

    private void seedUser(String id, String username, String role, String email, boolean verify, double balance, double frozen) throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, email, balance, frozen_balance, isVerify, rating)
                     VALUES(?,?,?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, id);
            stmt.setString(2, username);
            stmt.setString(3, "pass");
            stmt.setString(4, role);
            stmt.setString(5, email);
            stmt.setDouble(6, balance);
            stmt.setDouble(7, frozen);
            stmt.setInt(8, verify ? 1 : 0);
            stmt.setDouble(9, 4.5);
            stmt.executeUpdate();
        }
    }

    @Test
    void createUserShouldInsertDefaultsAndEnableUserShouldSetVerifyTrue() throws Exception {
        UserRepository repo = new UserRepository();

        assertTrue(repo.createUser("alice", "secret", "User", "alice@example.com"));

        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            stmt.setString(1, "alice");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertAll(
                        () -> assertEquals("alice", rs.getString("username")),
                        () -> assertEquals("secret", rs.getString("password")),
                        () -> assertEquals("User", rs.getString("role")),
                        () -> assertEquals("alice@example.com", rs.getString("email")),
                        () -> assertEquals(10000.0, rs.getDouble("balance")),
                        () -> assertEquals(0.0, rs.getDouble("frozen_balance"))
                );
            }
        }

        seedUser("u2", "bob", "User", "bob@example.com", false, 0.0, 0.0);
        assertTrue(repo.enableUser("bob@example.com"));

        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT isVerify FROM users WHERE email = ?")) {
            stmt.setString(1, "bob@example.com");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("isVerify"));
            }
        }
    }

    @Test
    void findMethodsShouldMapUserAndAdminAndUpdateRole() throws Exception {
        UserRepository repo = new UserRepository();
        seedUser("u1", "alice", "User", "alice@example.com", true, 2500.0, 250.0);
        seedUser("a1", "root", "Admin", "root@example.com", true, 5000.0, 0.0);

        try (Connection conn = openConnection()) {
            User user = repo.findById(conn, "u1");
            User admin = repo.findByUsername("root");

            assertAll(
                    () -> assertNotNull(user),
                    () -> assertFalse(user instanceof Admin),
                    () -> assertEquals("alice", user.getUsername()),
                    () -> assertEquals("alice@example.com", user.getEmail()),
                    () -> assertTrue(user.isVerify()),
                    () -> assertEquals(2500.0, user.getBalance()),
                    () -> assertEquals(250.0, user.getFrozenBalance()),
                    () -> assertNotNull(admin),
                    () -> assertTrue(admin instanceof Admin),
                    () -> assertEquals("root", admin.getUsername()),
                    () -> assertEquals("Admin", admin.getRole())
            );
        }

        assertTrue(repo.updateRole("u1", "MODERATOR"));
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT role FROM users WHERE id = ?")) {
            stmt.setString(1, "u1");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("MODERATOR", rs.getString("role"));
            }
        }
    }
}

