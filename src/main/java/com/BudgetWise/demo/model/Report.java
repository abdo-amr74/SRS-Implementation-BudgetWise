package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * US #7 — View Reports and Analytics
 *
 * Methods (from class diagram):
 * generate() → Queries transactions, computes totals & breakdown
 * getCategoryBreakdown() → Returns Map of category → total expense
 * getIncomeVsExpense() → Returns income and expense totals
 * display() → Prints formatted console report
 *
 * DB tables used:
 * transactions (id, userId, amount, type, category, note, date)
 */
public class Report {
    // Fields (from class diagram)
    private int reportID;
    private int userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private double totalIncome;
    private double totalExpense;
    private Map<String, Double> categoryBreakdown; // category → total expense
    // Constructor

    public Report(int userId, LocalDate startDate, LocalDate endDate) {
        this.reportID = 0;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalIncome = 0;
        this.totalExpense = 0;
        this.categoryBreakdown = new LinkedHashMap<>();
    }

    /**
     * Convenience: create a report for the current month.
     */
    public static Report forCurrentMonth(int userId) {
        YearMonth ym = YearMonth.now();
        return new Report(userId, ym.atDay(1), ym.atEndOfMonth());
    }

    // Core Methods
    /**
     * Generates the report by querying transactions within the date range.
     * US #7 step 3: System fetches all transactions for that period.
     */
    public void generate() {
        computeTotals();
        computeCategoryBreakdown();
        System.out.println("[Report] Generated for period: " + startDate + " to " + endDate);
    }

    /**
     * Computes total income and total expenses for the period.
     */
    private void computeTotals() {
        String sql = "SELECT type, SUM(amount) as total FROM transactions "
                + "WHERE userId=? AND date BETWEEN ? AND ? GROUP BY type";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                double total = rs.getDouble("total");
                if (type.equalsIgnoreCase("Income"))
                    totalIncome = total;
                if (type.equalsIgnoreCase("Expense"))
                    totalExpense = total;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Report totals error: " + e.getMessage());
        }
    }

    /**
     * Computes expense breakdown by category.
     * US #7 step 4: pie chart showing percentage breakdown by category.
     */
    private void computeCategoryBreakdown() {
        categoryBreakdown.clear();
        String sql = "SELECT category, SUM(amount) as total FROM transactions "
                + "WHERE userId=? AND type='Expense' AND date BETWEEN ? AND ? "
                + "GROUP BY category ORDER BY total DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categoryBreakdown.put(rs.getString("category"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Category breakdown error: " + e.getMessage());
        }
    }

    /**
     * Returns the category breakdown map.
     * Class diagram: getCategoryBreakdown()
     */
    public Map<String, Double> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    /**
     * Returns income vs expense data.
     * Class diagram: getIncomeVsExpense()
     */
    public double[] getIncomeVsExpense() {
        return new double[] { totalIncome, totalExpense };
    }

    /**
     * Generates a key insight message based on the data.
     * US #7 step 6: "Food spending is 15% above average."
     */
    private String generateInsight() {
        if (categoryBreakdown.isEmpty()) {
            return "No spending data available for this period.";
        }
        // Find the highest spending category
        String topCategory = "";
        double topAmount = 0;
        for (Map.Entry<String, Double> entry : categoryBreakdown.entrySet()) {
            if (entry.getValue() > topAmount) {
                topAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        double pct = totalExpense > 0 ? (topAmount / totalExpense) * 100 : 0;
        double netSavings = totalIncome - totalExpense;
        StringBuilder insight = new StringBuilder();
        insight.append(
                String.format("Your top spending category is \"%s\" at %.1f%% of total expenses.", topCategory, pct));
        if (netSavings > 0) {
            insight.append(String.format(" You saved %.2f EGP this period.", netSavings));
        } else if (netSavings < 0) {
            insight.append(String.format(" Warning: You overspent by %.2f EGP this period!", Math.abs(netSavings)));
        }

        return insight.toString();
    }

    /**
     * Displays the full report in the console (text-only statistics).
     * US #7 step 4-6: category breakdown, income vs expenses, key insight.
     */
    public void display() {
        System.out.println("\n+======================================+");
        System.out.println("|       FINANCIAL REPORT               |");
        System.out.println("+======================================+");
        System.out.printf("  Period: %s to %s%n%n", startDate, endDate);
        // Income vs Expenses Summary
        System.out.println("  --- Income vs Expenses ---");
        System.out.printf("  Total Income   : %.2f EGP%n", totalIncome);
        System.out.printf("  Total Expenses : %.2f EGP%n", totalExpense);
        System.out.printf("  Net Balance    : %.2f EGP%n%n", totalIncome - totalExpense);
        // Expense Breakdown by Category
        System.out.println("  --- Expense Breakdown by Category ---");
        if (categoryBreakdown.isEmpty()) {
            System.out.println("  No expenses recorded.\n");
        } else {
            int index = 1;
            for (Map.Entry<String, Double> entry : categoryBreakdown.entrySet()) {
                double pct = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100 : 0;
                System.out.printf("  %d. %-15s : %.2f EGP (%.1f%%)%n",
                        index++, entry.getKey(), entry.getValue(), pct);
            }
            System.out.println();
        }
        // Key Insight
        System.out.println("  --- Key Insight ---");
        System.out.println("  " + generateInsight());
        System.out.println("+======================================+");
    }

    // Getters
    public int getReportID() {
        return reportID;
    }

    public int getUserId() {
        return userId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }
}
