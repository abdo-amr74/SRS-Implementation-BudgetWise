package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;

/**
 * Represents a user of the BudgetWise application.
 */
public class User {
    private int userId; // Added userId field for DB reference
    private String fullName;
    private String email;
    private String password;
    private double balance; // Added balance field

    /**
     * Constructor for creating a new user.
     * 
     * @param fullName The full name of the user.
     * @param email The user's email.
     * @param password The user's password.
     */
    public User(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.balance = 0.0; // Starts at zero
    }

    /**
     * Constructor for loading an existing user from the database.
     * 
     * @param userId The unique user ID.
     * @param fullName The full name of the user.
     * @param email The user's email.
     * @param password The user's password.
     */
    public User(int userId, String fullName, String email, String password) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.balance = loadNetBalanceFromDB();
    }

    /**
     * Queries the DB for (total income - total expenses) so balance
     * is accurate across sessions and not just in-memory.
     */
    private double loadNetBalanceFromDB() {
        double income = 0, expense = 0;
        String sql = "SELECT type, SUM(amount) as total FROM transactions WHERE userId=? GROUP BY type";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                double total = rs.getDouble("total");
                if (type.equalsIgnoreCase("Income"))
                    income = total;
                if (type.equalsIgnoreCase("Expense"))
                    expense = total;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading balance: " + e.getMessage());
        }
        return income - expense;
    }

    /**
     * Returns the actual net balance from DB (total income - total expenses).
     * Used to check if an expense would cause a negative balance.
     */
    public double getNetBalance() {
        return loadNetBalanceFromDB();
    }

    // Getters and Setters

    /**
     * Gets the user ID.
     * @return The user ID.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the user's full name.
     * @return The full name.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Gets the user's email.
     * @return The email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the user's password.
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the in-memory balance.
     * @return The balance.
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Updates the in-memory balance.
     * 
     * @param amount The amount to add or subtract.
     * @param type The type ("Income" or "Expense").
     */
    public void updateBalance(double amount, String type) {
        if (type.equalsIgnoreCase("Income")) {
            this.balance += amount;
        } else if (type.equalsIgnoreCase("Expense")) {
            this.balance -= amount;
        }
    }
}