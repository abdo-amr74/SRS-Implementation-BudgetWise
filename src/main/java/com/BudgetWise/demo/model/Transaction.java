package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;

public class Transaction {
    private int transactionId;
    private int userId;
    private String type; // "Income" or "Expense"
    private double amount;
    private String category;
    private String note;
    private String date;

    public Transaction(int userId, String type, double amount, String category) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number greater than 0.");
        }
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = "";
        this.date = LocalDate.now().toString(); // "YYYY-MM-DD"
    }

    /**
     * Saves this transaction to the database.
     */
    public void save() {
        String sql = "INSERT INTO transactions (userId, amount, type, category, note, date) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, amount);
            ps.setString(3, type);
            ps.setString(4, category);
            ps.setString(5, note);
            ps.setString(6, date);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) this.transactionId = keys.getInt(1);

            System.out.println("[DB] Transaction saved: " + this + " | id=" + transactionId);
        } catch (SQLException e) {
            System.err.println("[DB] Error saving transaction: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "[" + type + "] " + amount + " EGP - " + category;
    }

    // Getters
    public int getTransactionId() { return transactionId; }
    public int getUserId() { return userId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
}