package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;

/**
 * Represents a user's profile and settings.
 */
public class Userprofile {
    private int userId;
    private String displayName;
    private String currency;

    /**
     * Constructor for creating a Userprofile instance.
     * 
     * @param userId The ID of the user.
     * @param displayName The display name.
     * @param currency The preferred currency.
     */
    public Userprofile(int userId, String displayName, String currency) {
        this.userId = userId;
        this.displayName = displayName;
        this.currency = currency;
    }

    /**
     * Updates the user's profile in the database.
     */
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

    /**
     * Retrieves the profile for a given user from the database.
     * 
     * @param userId The user ID.
     * @return The Userprofile object, or null if not found.
     */
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

    /**
     * Gets the display name.
     * @return The display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the user's preferred currency.
     * @return The currency.
     */
    public String getCurrency() {
        return currency;
    }
}