package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction model — extended with edit() and delete() from Sequence Diagram
 * 7.
 *
 * SD7 methods:
 * edit(newAmount, newCategory) → UPDATE in DB, adjust Budget, checkThreshold
 * delete(transactionId) → DELETE from DB, adjust Budget
 * loadById(id) → SELECT single transaction
 * loadForUser(userId) → SELECT all transactions for a user
 */
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

    // Private constructor for loading from DB
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
     * Saves this transaction to the database.
     * After saving, updates the matching Budget's spentAmount (SD5 integration).
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
            if (keys.next())
                this.transactionId = keys.getInt(1);
            System.out.println("[DB] Transaction saved: " + this + " | id=" + transactionId);
        } catch (SQLException e) {
            System.err.println("[DB] Error saving transaction: " + e.getMessage());
            return;
        }
        // SD5: If this is an Expense, update matching Budget
        if (type.equalsIgnoreCase("Expense")) {
            Budget budget = Budget.findByCategory(userId, category);
            if (budget != null) {
                budget.update(amount); // adds amount to spentAmount, triggers checkThreshold()
            }
        }
    }

    /**
     * Edits this transaction with new amount and/or category.
     * SD7: edit(transactionID, newAmount, newCategory) -> save() -> Budget.update()
     * -> checkThreshold()
     *
     * @return true if edit succeeded, false otherwise
     */
    public boolean edit(double newAmount, String newCategory) {
        if (newAmount <= 0) {
            System.out.println("Error: Amount must be positive.");
            return false;
        }
        double oldAmount = this.amount;
        String oldCategory = this.category;
        // SD7 step 2: save() — UPDATE in DB
        String sql = "UPDATE transactions SET amount=?, category=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newAmount);
            ps.setString(2, newCategory);
            ps.setInt(3, transactionId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                System.out.println("Error: Could not update transaction.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error editing transaction: " + e.getMessage());
            System.out.println("Error: Could not update transaction.");
            return false;
        }
        this.amount = newAmount;
        this.category = newCategory;
        // SD7: If Expense, adjust budgets
        if (type.equalsIgnoreCase("Expense")) {
            // Remove old amount from old category's budget
            Budget oldBudget = Budget.findByCategory(userId, oldCategory);
            if (oldBudget != null) {
                oldBudget.update(-oldAmount);
            }

            // Add new amount to new category's budget
            Budget newBudget = Budget.findByCategory(userId, newCategory);
            if (newBudget != null) {
                newBudget.update(newAmount); // triggers checkThreshold()
            }
        }
        System.out.println("Transaction updated successfully.");
        return true;
    }

    /**
     * Deletes a transaction by ID.
     * SD7: delete(transactionID) -> Transaction.delete() -> Budget.update() ->
     * checkThreshold()
     */
    public static boolean delete(int transactionId) {
        // Load the transaction first to know amount/category for budget adjustment
        Transaction t = loadById(transactionId);
        if (t == null) {
            System.out.println("Error: Transaction not found.");
            return false;
        }
        String sql = "DELETE FROM transactions WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error deleting transaction: " + e.getMessage());
            return false;
        }
        // SD7: If Expense, subtract from budget
        if (t.type.equalsIgnoreCase("Expense")) {
            Budget budget = Budget.findByCategory(t.userId, t.category);
            if (budget != null) {
                budget.update(-t.amount); // reduces spentAmount, triggers checkThreshold()
            }
        }
        System.out.println("Transaction deleted successfully.");
        return true;
    }

    /**
     * Loads a single transaction by ID.
     */
    public static Transaction loadById(int transactionId) {
        String sql = "SELECT id, userId, type, amount, category, note, date FROM transactions WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Transaction(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("note"),
                        rs.getString("date"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading transaction: " + e.getMessage());
        }
        return null;
    }

    /**
     * Loads all transactions for a user, optionally filtered by date range.
     */
    public static List<Transaction> loadForUser(int userId, String startDate, String endDate) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, userId, type, amount, category, note, date FROM transactions "
                + "WHERE userId=? AND date BETWEEN ? AND ? ORDER BY date DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("note"),
                        rs.getString("date")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Loads all transactions for a user (no date filter).
     */
    public static List<Transaction> loadAllForUser(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, userId, type, amount, category, note, date FROM transactions "
                + "WHERE userId=? ORDER BY date DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("note"),
                        rs.getString("date")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading transactions: " + e.getMessage());
        }
        return list;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + amount + " EGP - " + category;
    }

    public String toDetailString() {
        return String.format("  #%d [%s] %.2f EGP - %s  (%s)",
                transactionId, type, amount, category, date);
    }

    // Getters
    public int getTransactionId() {
        return transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }
}