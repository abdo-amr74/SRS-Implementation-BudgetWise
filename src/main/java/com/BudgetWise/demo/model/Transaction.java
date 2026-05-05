package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction model — supports record persistence,
 * historical filtering, and budget-linked updates.
 */
public class Transaction {
    private int transactionId;
    private int userId;
    private String type; // "Income" or "Expense"
    private double amount;
    private String category;
    private String note;
    private String date;

    /**
     * Constructor to create a new Transaction.
     * 
     * @param userId The ID of the user.
     * @param type The transaction type ("Income" or "Expense").
     * @param amount The transaction amount.
     * @param category The category of the transaction.
     */
    public Transaction(int userId, String type, double amount, String category) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number greater than 0.");
        }
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = "";
        this.date = LocalDate.now().toString();
    }

    private Transaction(int transactionId, int userId, String type,
            double amount, String category, String note, String date) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.date = date;
    }

    /**
     * Loads transactions for a specific user filtered by category.
     * 
     * @param userId The ID of the user.
     * @param category The category name to filter by.
     * @return A list of matching transactions.
     */
    public static List<Transaction> loadByCategory(int userId, String category) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, userId, type, amount, category, note, date FROM transactions "
                + "WHERE userId=? AND category=? ORDER BY date DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"), rs.getInt("userId"), rs.getString("type"),
                        rs.getDouble("amount"), rs.getString("category"),
                        rs.getString("note"), rs.getString("date")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error filtering transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing transaction and synchronizes changes with linked budgets.
     * 
     * @param newAmount The new transaction amount.
     * @param newCategory The new transaction category.
     * @return true if successful, false otherwise.
     */
    public boolean edit(double newAmount, String newCategory) {
        if (newAmount <= 0)
            return false;
        double oldAmount = this.amount;
        String oldCategory = this.category;

        String sql = "UPDATE transactions SET amount=?, category=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newAmount);
            ps.setString(2, newCategory);
            ps.setInt(3, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            return false;
        }

        this.amount = newAmount;
        this.category = newCategory;

        if (type.equalsIgnoreCase("Expense")) {
            Budget oldB = Budget.findByCategory(userId, oldCategory);
            if (oldB != null)
                oldB.update(-oldAmount);
            Budget newB = Budget.findByCategory(userId, newCategory);
            if (newB != null)
                newB.update(newAmount);
        }
        return true;
    }

    /**
     * Removes a transaction by ID and restores the spent amount to the relevant
     * budget.
     * 
     * @param transactionId The ID of the transaction to delete.
     * @return true if successful, false otherwise.
     */
    public static boolean delete(int transactionId) {
        Transaction t = loadById(transactionId);
        if (t == null)
            return false;

        String sql = "DELETE FROM transactions WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            return false;
        }

        if (t.type.equalsIgnoreCase("Expense")) {
            Budget budget = Budget.findByCategory(t.userId, t.category);
            if (budget != null)
                budget.update(-t.amount);
        }
        return true;
    }

    /**
     * Saves a new transaction to the database.
     */
    public void save() {
        String sql = "INSERT INTO transactions (userId, amount, type, category, note, date) VALUES (?, ?, ?, ?, ?, ?)";
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
            if (keys.next())
                this.transactionId = keys.getInt(1);
        } catch (SQLException e) {
        }
    }

    /**
     * Loads a transaction by its ID.
     * 
     * @param id The transaction ID.
     * @return The Transaction object, or null if not found.
     */
    public static Transaction loadById(int id) {
        String sql = "SELECT * FROM transactions WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return new Transaction(rs.getInt("id"), rs.getInt("userId"), rs.getString("type"),
                        rs.getDouble("amount"), rs.getString("category"), rs.getString("note"), rs.getString("date"));
        } catch (SQLException e) {
        }
        return null;
    }

    /**
     * Loads transactions for a user within a specific date range.
     * 
     * @param userId The user ID.
     * @param startDate The start date string.
     * @param endDate The end date string.
     * @return A list of transactions.
     */
    public static List<Transaction> loadForUser(int userId, String startDate, String endDate) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE userId=? AND date BETWEEN ? AND ? ORDER BY date DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"), rs.getInt("userId"), rs.getString("type"),
                        rs.getDouble("amount"), rs.getString("category"),
                        rs.getString("note"), rs.getString("date")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading report transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Loads all transactions for a user.
     * 
     * @param userId The user ID.
     * @return A list of all transactions.
     */
    public static List<Transaction> loadAllForUser(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE userId=? ORDER BY date DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new Transaction(rs.getInt("id"), rs.getInt("userId"), rs.getString("type"),
                        rs.getDouble("amount"), rs.getString("category"), rs.getString("note"), rs.getString("date")));
        } catch (SQLException e) {
        }
        return list;
    }

    /**
     * Returns a formatted detailed string for the transaction.
     * @return The formatted string.
     */
    public String toDetailString() {
        return String.format("  #%d [%s] %.2f EGP - %s (%s)", transactionId, type, amount, category, date);
    }

    /**
     * Gets the user ID.
     * @return The user ID.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the transaction type.
     * @return The type.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the transaction amount.
     * @return The amount.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Gets the transaction category.
     * @return The category.
     */
    public String getCategory() {
        return category;
    }
}