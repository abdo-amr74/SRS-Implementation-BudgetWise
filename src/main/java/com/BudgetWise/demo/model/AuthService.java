package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service class handling user authentication including sign-up and login functionalities.
 * It interacts with the DatabaseManager to persist and verify user credentials.
 */
public class AuthService {

    /**
     * User Story #1: Sign-Up — registers a new user in the database.
     * 
     * @param name The full name of the user.
     * @param email The email address of the user.
     * @param pass The chosen password.
     * @param confirmPass The password confirmation to ensure they match.
     * @return A success message if registered, or an error message if validation fails or email exists.
     */
    public String signUp(String name, String email, String pass, String confirmPass) {
        if (!pass.equals(confirmPass)) {
            return "Error: Passwords do not match.";
        }
        if (pass.length() < 8) {
            return "Error: Password must be at least 8 characters.";
        }

        // Check if email already exists
        String checkSql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "Error: Email already registered.";
            }
        } catch (SQLException e) {
            return "Error: Database error — " + e.getMessage();
        }

        // Insert the new user
        String insertSql = "INSERT INTO users (fullName, email, password, createdAt) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pass);
            ps.setString(4, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.executeUpdate();
            System.out.println("[DB] User registered: " + name);
        } catch (SQLException e) {
            return "Error: Database error — " + e.getMessage();
        }

        return "Account created successfully for " + name;
    }

    /**
     * User Story #2: Login — authenticates a user against the database.
     * 
     * @param email The user's email address.
     * @param pass The user's password.
     * @return The authenticated User object if credentials are correct, null otherwise.
     */
    public User login(String email, String pass) {
        String sql = "SELECT id, fullName, email, password FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("fullName"),
                        rs.getString("email"),
                        rs.getString("password")
                );
            }
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
        }
        System.out.println("Error: Invalid email or password. Please try again.");
        return null;
    }
}
