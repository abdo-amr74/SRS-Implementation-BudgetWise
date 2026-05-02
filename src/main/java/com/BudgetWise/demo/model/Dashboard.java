package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * US 10 — Dashboard
 */
public class Dashboard {

    private int userId;
    private String userFullName;

    // Computed stats (filled by load())
    private double totalIncome;
    private double totalExpenses;
    private double netBalance;
    private List<String> recentTransactionLines = new ArrayList<>();
    private List<String> budgetWarnings = new ArrayList<>();

    // Constructor
    public Dashboard(int userId, String userFullName) {
        this.userId = userId;
        this.userFullName = userFullName;
    }

    public void load() {
        System.out.println("\n[Dashboard] Loading data for: " + userFullName);
        filterByCurrentMonth(); // step 4
        computeIncomeAndExpense(); // step 5
        loadRecentTransactions(); // step 6
        checkBudgetWarnings(); // step 7
    }

    /**
     * Current month filter — used as the date boundary for DB queries.
     */
    private void filterByCurrentMonth() {
        System.out.println("[Dashboard] Filtering for current month: "
                + LocalDate.now().getMonth() + " " + LocalDate.now().getYear());
    }

    /**
     * Queries transactions table to compute income, expenses, and net.
     * (Diagram step 5: getIncomeAndExpense())
     */
    private void computeIncomeAndExpense() {
        String monthPrefix = LocalDate.now().toString().substring(0, 7); // "YYYY-MM"
        String sql = "SELECT type, SUM(amount) as total FROM transactions "
                + "WHERE userId=? AND date LIKE ? GROUP BY type";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, monthPrefix + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                double total = rs.getDouble("total");
                if (type.equalsIgnoreCase("Income"))
                    this.totalIncome = total;
                if (type.equalsIgnoreCase("Expense"))
                    this.totalExpenses = total;
            }
            this.netBalance = totalIncome - totalExpenses;
        } catch (SQLException e) {
            System.err.println("[DB] Dashboard stats error: " + e.getMessage());
        }
    }

    /**
     * Returns summary stats string.
     * (Diagram step 3: getStats())
     */
    public String getStats() {
        return String.format(
                "  Total Income    : %.2f EGP%n" +
                        "  Total Expenses  : %.2f EGP%n" +
                        "  Net Balance     : %.2f EGP",
                totalIncome, totalExpenses, netBalance);
    }

    /**
     * Loads the 5 most recent transactions from DB.
     * (Diagram step 6: getRecentTransactions())
     */
    private void loadRecentTransactions() {
        recentTransactionLines.clear();
        String sql = "SELECT type, amount, category, date FROM transactions "
                + "WHERE userId=? ORDER BY date DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                recentTransactionLines.add(String.format("[%s] %.2f EGP - %s  (%s)",
                        rs.getString("type"), rs.getDouble("amount"),
                        rs.getString("category"), rs.getString("date")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Dashboard recent tx error: " + e.getMessage());
        }
    }

    /**
     * Checks all budgets and collects warnings when spentAmount ≥ alertThreshold%.
     * (Diagram step 7: checkBudgetWarnings())
     */
    public void checkBudgetWarnings() {
        budgetWarnings.clear();
        String sql = "SELECT name, amount, spentAmount, alertThreshold FROM budgets WHERE userId=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double limit = rs.getDouble("amount");
                double spent = rs.getDouble("spentAmount");
                double threshold = rs.getDouble("alertThreshold");
                if (limit > 0 && (spent / limit) >= threshold) {
                    budgetWarnings.add(String.format("⚠ Budget '%s': %.0f%% used (%.2f / %.2f EGP)",
                            rs.getString("name"), (spent / limit) * 100, spent, limit));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Budget warnings error: " + e.getMessage());
        }
        if (budgetWarnings.isEmpty()) {
            System.out.println("[Dashboard] No budget warnings.");
        }
    }

    /**
     * Prints the full dashboard summary to the console.
     * Called by Main after load(). (Diagram step 8: Display Dashboard UI)
     */
    public void display() {
        System.out.println("\n╔══════════════════════════════╗");
        System.out.println("║        DASHBOARD SUMMARY     ║");
        System.out.println("╚══════════════════════════════╝");
        System.out.println(getStats());

        System.out.println("\n  Recent Transactions:");
        if (recentTransactionLines.isEmpty()) {
            System.out.println("    No transactions yet.");
        } else {
            recentTransactionLines.forEach(line -> System.out.println("    " + line));
        }

        System.out.println("\n  Budget Warnings:");
        if (budgetWarnings.isEmpty()) {
            System.out.println("    All budgets are healthy.");
        } else {
            budgetWarnings.forEach(w -> System.out.println("    " + w));
        }

        int unread = Notification.countUnread(userId);
        System.out.println("\n  Unread Notifications: " + unread);
        System.out.println("══════════════════════════════════");
    }
}