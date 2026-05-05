package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * US #4 — Create/Edit Budget
 * Sequence Diagram 4 methods:
 * create() -> INSERT budget into DB (checks for duplicates)
 * edit() -> UPDATE budget details in DB
 * update(amount) -> adds expense amount to spentAmount, calls checkThreshold()
 * checkThreshold() -> compares spentAmount/limitAmount against alertThreshold
 * calcRemaining() -> returns limitAmount - spentAmount
 *
 * DB table used (from DatabaseManager):
 * budgets (id, userId, name, amount, spentAmount, startDate, endDate,
 * alertThreshold)
 */
public class Budget {
    // Fields (from class diagram)
    private int budgetID;
    private int userId;
    private String name; // category name (example: "Food & Dining")
    private double limitAmount; // budget cap
    private double spentAmount; // running total of expenses
    private LocalDate startDate;
    private LocalDate endDate;
    private double alertThreshold; // e.g. 0.90 for 90%

    /**
     * Constructor for creating a new budget.
     * 
     * @param userId The ID of the user creating the budget.
     * @param name The category name of the budget (e.g., "Food").
     * @param limitAmount The maximum amount allowed for this budget.
     * @param startDate The start date of the budget period.
     * @param endDate The end date of the budget period.
     * @param alertThreshold The percentage threshold to trigger an alert (e.g., 0.90).
     */
    public Budget(int userId, String name, double limitAmount,
            LocalDate startDate, LocalDate endDate, double alertThreshold) {
        this.budgetID = 0;
        this.userId = userId;
        this.name = name;
        this.limitAmount = limitAmount;
        this.spentAmount = 0.0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.alertThreshold = alertThreshold;
    }

    // Private constructor used when loading from DB
    private Budget(int budgetID, int userId, String name, double limitAmount,
            double spentAmount, LocalDate startDate, LocalDate endDate,
            double alertThreshold) {
        this.budgetID = budgetID;
        this.userId = userId;
        this.name = name;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.alertThreshold = alertThreshold;
    }

    // Core Methods (SD4)
    /**
     * Creates a new budget in the database.
     * Checks for duplicate budget (same user, same category, overlapping period).
     * SD4 step 2: Budget → create()
     *
     * @return true if created successfully, false if conflict or error
     */
    public boolean create() {
        // Check for existing budget with same category in same month
        String checkSql = "SELECT id FROM budgets WHERE userId=? AND name=? " +
                "AND startDate=? AND endDate=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, startDate.toString());
            ps.setString(4, endDate.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Error: A budget for this category already exists for this month.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error checking budget conflict: " + e.getMessage());
            return false;
        }
        // Insert the new budget
        String sql = "INSERT INTO budgets (userId, name, amount, spentAmount, startDate, endDate, alertThreshold) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setDouble(3, limitAmount);
            ps.setDouble(4, spentAmount);
            ps.setString(5, startDate.toString());
            ps.setString(6, endDate.toString());
            ps.setDouble(7, alertThreshold);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                this.budgetID = keys.getInt(1);
            System.out.println("[DB] Budget created: " + name + " | id=" + budgetID);
        } catch (SQLException e) {
            System.err.println("[DB] Error creating budget: " + e.getMessage());
            return false;
        }
        // Compute initial spentAmount from existing transactions in this period
        computeSpentFromTransactions();
        // SD4 step 4: checkThreshold()
        checkThreshold();
        return true;
    }

    /**
     * Edits an existing budget's details and persists to DB.
     * SD4: edit()
     * 
     * @param newName The new category name.
     * @param newLimit The new budget limit.
     * @param newStart The new start date.
     * @param newEnd The new end date.
     * @param newThreshold The new alert threshold percentage.
     */
    public void edit(String newName, double newLimit, LocalDate newStart,
            LocalDate newEnd, double newThreshold) {
        this.name = newName;
        this.limitAmount = newLimit;
        this.startDate = newStart;
        this.endDate = newEnd;
        this.alertThreshold = newThreshold;
        String sql = "UPDATE budgets SET name=?, amount=?, startDate=?, endDate=?, alertThreshold=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, limitAmount);
            ps.setString(3, startDate.toString());
            ps.setString(4, endDate.toString());
            ps.setDouble(5, alertThreshold);
            ps.setInt(6, budgetID);
            ps.executeUpdate();
            System.out.println("[DB] Budget updated: " + name);
        } catch (SQLException e) {
            System.err.println("[DB] Error updating budget: " + e.getMessage());
        }
        // Recompute spent and re-check
        computeSpentFromTransactions();
        checkThreshold();
    }

    /**
     * Adds an expense amount to spentAmount and persists. Called after a
     * transaction is saved.
     * SD5 step 2: Budget -> update(amount)
     *
     * @param amount the expense amount to add (positive = add, negative = subtract)
     */
    public void update(double amount) {
        this.spentAmount += amount;
        if (this.spentAmount < 0)
            this.spentAmount = 0;
        String sql = "UPDATE budgets SET spentAmount=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, spentAmount);
            ps.setInt(2, budgetID);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error updating budget spent: " + e.getMessage());
        }
        // SD5 step 3: checkThreshold()
        checkThreshold();
    }

    /**
     * Checks if spending has crossed the alert threshold or exceeded the budget.
     * SD4/SD5: checkThreshold() — self-call
     *
     * Two alert levels:
     * 1 Spending ≥ alertThreshold% → "Budget Alert" notification
     * 2 Spending ≥ 100% → "Budget Exceeded" notification
     */
    public void checkThreshold() {
        if (limitAmount <= 0)
            return;
        double percentage = spentAmount / limitAmount;
        if (percentage >= 1.0) {
            // Budget EXCEEDED
            double overage = spentAmount - limitAmount;
            BudgetAlert.generate(this, percentage,
                    String.format("Budget Exceeded — %s! You've exceeded your %.2f EGP budget by %.2f EGP.",
                            name, limitAmount, overage));
        } else if (percentage >= alertThreshold) {
            // Near threshold
            BudgetAlert.generate(this, percentage,
                    String.format("Budget Alert — %s: You've used %.1f%% of your %s budget.",
                            name, percentage * 100, name));
        }
    }

    /**
     * Calculates the remaining budget amount.
     * Class diagram: calcRemaining()
     * 
     * @return The remaining amount.
     */
    public double calcRemaining() {
        return limitAmount - spentAmount;
    }

    /**
     * Computes spentAmount from existing expense transactions within the budget
     * period.
     */
    private void computeSpentFromTransactions() {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM transactions " +
                "WHERE userId=? AND category=? AND type='Expense' AND date BETWEEN ? AND ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, startDate.toString());
            ps.setString(4, endDate.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.spentAmount = rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error computing spent: " + e.getMessage());
        }
        // Persist the computed spentAmount
        String updateSql = "UPDATE budgets SET spentAmount=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setDouble(1, spentAmount);
            ps.setInt(2, budgetID);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error persisting spent: " + e.getMessage());
        }
    }

    // Static DB Queries
    /**
     * Loads all budgets for a given user.
     * 
     * @param userId The ID of the user.
     * @return A list of Budget objects belonging to the user.
     */
    public static List<Budget> loadForUser(int userId) {
        List<Budget> list = new ArrayList<>();
        String sql = "SELECT id, userId, name, amount, spentAmount, startDate, endDate, alertThreshold "
                + "FROM budgets WHERE userId=? ORDER BY startDate DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Budget(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        rs.getDouble("spentAmount"),
                        LocalDate.parse(rs.getString("startDate")),
                        LocalDate.parse(rs.getString("endDate")),
                        rs.getDouble("alertThreshold")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading budgets: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a budget matching a category name for the current date range.
     * Used by Transaction.save() to auto-update budget spentAmount (SD5).
     * 
     * @param userId The ID of the user.
     * @param category The category name to search for.
     * @return The Budget object if found, null otherwise.
     */
    public static Budget findByCategory(int userId, String category) {
        String today = LocalDate.now().toString();
        String sql = "SELECT id, userId, name, amount, spentAmount, startDate, endDate, alertThreshold "
                + "FROM budgets WHERE userId=? AND name=? AND startDate<=? AND endDate>=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setString(3, today);
            ps.setString(4, today);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Budget(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        rs.getDouble("spentAmount"),
                        LocalDate.parse(rs.getString("startDate")),
                        LocalDate.parse(rs.getString("endDate")),
                        rs.getDouble("alertThreshold"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error finding budget: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a budget by its ID.
     * 
     * @param budgetId The ID of the budget.
     * @return The Budget object if found, null otherwise.
     */
    public static Budget findById(int budgetId) {
        String sql = "SELECT id, userId, name, amount, spentAmount, startDate, endDate, alertThreshold "
                + "FROM budgets WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Budget(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        rs.getDouble("spentAmount"),
                        LocalDate.parse(rs.getString("startDate")),
                        LocalDate.parse(rs.getString("endDate")),
                        rs.getDouble("alertThreshold"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error finding budget by id: " + e.getMessage());
        }
        return null;
    }

    // Display
    @Override
    public String toString() {
        double pct = limitAmount > 0 ? (spentAmount / limitAmount) * 100 : 0;
        String bar = buildProgressBar(pct);
        String status;
        if (pct >= 100) {
            status = "EXCEEDED";
        } else if (pct >= alertThreshold * 100) {
            status = "WARNING";
        } else {
            status = "OK";
        }
        return String.format(
                "  [Budget #%d] %s%n" +
                        "    Limit     : %.2f EGP%n" +
                        "    Spent     : %.2f EGP%n" +
                        "    Remaining : %.2f EGP%n" +
                        "    Progress  : %s %.1f%%%n" +
                        "    Period    : %s to %s%n" +
                        "    Threshold : %.0f%%%n" +
                        "    Status    : %s",
                budgetID, name,
                limitAmount,
                spentAmount,
                calcRemaining(),
                bar, pct,
                startDate, endDate,
                alertThreshold * 100,
                status);
    }

    /**
     * Builds a text-based progress bar for console display.
     * 
     * @param pct The percentage of the progress.
     * @return A string representing the progress bar.
     */
    private String buildProgressBar(double pct) {
        int filled = (int) Math.min(20, (pct / 100) * 20);
        int empty = 20 - filled;
        String color;
        if (pct >= 100) {
            color = "█"; // exceeded
        } else if (pct >= 90) {
            color = "▓"; // danger
        } else if (pct >= 70) {
            color = "▒"; // warning
        } else {
            color = "░"; // safe
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filled; i++)
            sb.append(color);
        for (int i = 0; i < empty; i++)
            sb.append(" ");
        sb.append("]");
        return sb.toString();
    }

    // Getters

    /**
     * Gets the unique ID of the budget.
     * @return The budget ID.
     */
    public int getBudgetID() {
        return budgetID;
    }

    /**
     * Gets the ID of the user owning the budget.
     * @return The user ID.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the category name of the budget.
     * @return The category name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the budget limit amount.
     * @return The limit amount.
     */
    public double getLimitAmount() {
        return limitAmount;
    }

    /**
     * Gets the total amount spent within the budget period.
     * @return The spent amount.
     */
    public double getSpentAmount() {
        return spentAmount;
    }

    /**
     * Gets the start date of the budget.
     * @return The start date.
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Gets the end date of the budget.
     * @return The end date.
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Gets the alert threshold percentage.
     * @return The alert threshold.
     */
    public double getAlertThreshold() {
        return alertThreshold;
    }
}
