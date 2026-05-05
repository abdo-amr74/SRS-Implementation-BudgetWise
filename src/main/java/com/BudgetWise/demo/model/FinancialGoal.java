package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * US 6 — Financial Goals
 *
 * Sequence Diagram 6 methods:
 * initialize() → constructor
 * addContribution() → adds money toward goal, updates DB
 * calculateMonthlySaving() → how much/month needed
 * updateProgress() → recalculates % done
 * save() → INSERT or UPDATE in goals table
 *
 * DB table used (from DatabaseManager):
 * goals (id, userId, name, targetAmount, currentAmount, deadline, status)
 */
public class FinancialGoal {

    private int goalId; // DB primary key (0 = not yet saved)
    private int userId; // FK → users.id
    private String name;
    private double targetAmount;
    private double currentAmount;
    private LocalDate deadline;
    private String status; // "active" | "completed"
    private double progressPercent; // 0.0 – 100.0 (computed, not stored)

    /**
     * Constructor for creating a new Financial Goal.
     * 
     * @param userId The ID of the user.
     * @param name The name of the goal.
     * @param targetAmount The target amount to reach.
     * @param deadline The deadline to achieve the goal.
     * @param initialContribution Initial amount put towards the goal.
     */
    public FinancialGoal(int userId, String name, double targetAmount,
            LocalDate deadline, double initialContribution) {
        this.goalId = 0; // assigned after save()
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = 0.0;
        this.deadline = deadline;
        this.status = "active";

        // Initial contribution provided
        if (initialContribution > 0) {
            this.currentAmount = initialContribution;
        }
        updateProgress();
    }

    /** 
     * Private constructor used when loading rows from the DB. 
     *
     * @param goalId The goal ID.
     * @param userId The user ID.
     * @param name The goal name.
     * @param targetAmount The target amount.
     * @param currentAmount The current amount saved.
     * @param deadline The deadline.
     * @param status The status of the goal.
     */
    private FinancialGoal(int goalId, int userId, String name, double targetAmount,
            double currentAmount, LocalDate deadline, String status) {
        this.goalId = goalId;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.status = status;
        updateProgress();
    }

    // ── Core Methods ──────────────────────────────────────────────────────────

    /**
     * Adds a monetary contribution toward the goal and persists to DB.
     * Automatically calls updateProgress() and checks completion.
     * 
     * @param amount The amount to contribute.
     */
    public void addContribution(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Contribution must be greater than 0.");
        }
        this.currentAmount += amount;
        updateProgress();
        if (this.currentAmount >= this.targetAmount) {
            this.status = "completed";
        }
        // Persist the updated amounts
        updateInDB();
    }

    /**
     * Returns how much needs to be saved per month to hit the target on time.
     * 
     * @return The required monthly saving amount.
     */
    public double calculateMonthlySaving() {
        double remaining = targetAmount - currentAmount;
        if (remaining <= 0)
            return 0.0;
        long monthsLeft = ChronoUnit.MONTHS.between(LocalDate.now(), deadline);
        if (monthsLeft <= 0)
            return remaining;
        return remaining / monthsLeft;
    }

    /**
     * Recalculates progressPercent. Called after every contribution.
     */
    public void updateProgress() {
        if (targetAmount <= 0) {
            this.progressPercent = 0.0;
        } else {
            this.progressPercent = Math.min(100.0, (currentAmount / targetAmount) * 100.0);
        }
    }

    /**
     * Saves a NEW goal to the database (INSERT).
     * Sets goalId from the generated key.
     * (Diagram step: save())
     */
    public void save() {
        String sql = "INSERT INTO goals (userId, name, targetAmount, currentAmount, deadline, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setDouble(3, targetAmount);
            ps.setDouble(4, currentAmount);
            ps.setString(5, deadline.toString());
            ps.setString(6, status);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                this.goalId = keys.getInt(1);

            System.out.println("[DB] Goal saved: " + name + " | id=" + goalId);

        } catch (SQLException e) {
            System.err.println("[DB] Error saving goal: " + e.getMessage());
        }
    }

    /** Updates currentAmount and status for an existing goal row (UPDATE). */
    private void updateInDB() {
        if (goalId == 0) {
            save();
            return;
        }
        String sql = "UPDATE goals SET currentAmount=?, status=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, currentAmount);
            ps.setString(2, status);
            ps.setInt(3, goalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error updating goal: " + e.getMessage());
        }
    }

    // Static DB Queries

    /**
     * Loads all goals for a given user from the database.
     * 
     * @param userId The ID of the user.
     * @return A list of FinancialGoal objects.
     */
    public static List<FinancialGoal> loadForUser(int userId) {
        List<FinancialGoal> list = new ArrayList<>();
        String sql = "SELECT id, userId, name, targetAmount, currentAmount, deadline, status "
                + "FROM goals WHERE userId=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new FinancialGoal(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("name"),
                        rs.getDouble("targetAmount"),
                        rs.getDouble("currentAmount"),
                        LocalDate.parse(rs.getString("deadline")),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error loading goals: " + e.getMessage());
        }
        return list;
    }

    // Display
    @Override
    public String toString() {
        return String.format(
                "  [Goal #%d] %s%n" +
                        "    Progress : %.1f%% (%.2f / %.2f EGP)%n" +
                        "    Deadline : %s%n" +
                        "    Monthly  : %.2f EGP/month needed%n" +
                        "    Status   : %s",
                goalId, name,
                progressPercent, currentAmount, targetAmount,
                deadline,
                calculateMonthlySaving(),
                status);
    }

    // Getters

    /**
     * Gets the goal ID.
     * @return The goal ID.
     */
    public int getGoalId() {
        return goalId;
    }

    /**
     * Gets the user ID associated with the goal.
     * @return The user ID.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the name of the goal.
     * @return The goal name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the target amount for the goal.
     * @return The target amount.
     */
    public double getTargetAmount() {
        return targetAmount;
    }

    /**
     * Gets the current amount saved.
     * @return The current amount.
     */
    public double getCurrentAmount() {
        return currentAmount;
    }

    /**
     * Gets the deadline for the goal.
     * @return The deadline.
     */
    public LocalDate getDeadline() {
        return deadline;
    }

    /**
     * Gets the status of the goal.
     * @return The status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Gets the current progress percentage towards the goal.
     * @return The progress percentage.
     */
    public double getProgressPercent() {
        return progressPercent;
    }
}