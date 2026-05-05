package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;

public class Userprofile {
    private int userId;
    private String displayName;
    private String currency;

    public Userprofile(int userId, String displayName, String currency) {
        this.userId = userId;
        this.displayName = displayName;
        this.currency = currency;
    }

    public void update() {
        String sql = "UPDATE users SET fullName = ?, currency = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, currency);
            ps.setInt(3, userId);
            ps.executeUpdate();
            System.out.println("Profile updated successfully in database.");
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
        }
    }

    // Getters and static loader
    public static Userprofile getProfile(int userId) {
        String sql = "SELECT fullName, currency FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Userprofile(userId, rs.getString("fullName"), rs.getString("currency"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching profile: " + e.getMessage());
        }
        return null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCurrency() {
        return currency;
    }
}