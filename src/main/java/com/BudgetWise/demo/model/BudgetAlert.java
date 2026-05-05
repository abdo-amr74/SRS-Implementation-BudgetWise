package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * US #5 — Budget Over-Limit Alert
 *
 * Sequence Diagram 5 methods:
 * generate(budget) → Creates a BudgetAlert when threshold is crossed
 * send() → Persists alert to DB and triggers Notification.send()
 *
 * DB table used (from DatabaseManager):
 * budget_alerts (id, budgetId, userId, percentageReached, triggeredAt, message)
 */
public class BudgetAlert {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Fields (from class diagram)
    private int alertID;
    private int budgetId;
    private int userId;
    private double percentageReached;
    private LocalDateTime triggeredAt;
    private String message;

    /**
     * Private constructor for creating a BudgetAlert instance.
     * 
     * @param budgetId The ID of the budget that triggered the alert.
     * @param userId The ID of the user owning the budget.
     * @param percentageReached The percentage of the budget limit reached.
     * @param message The alert message describing the threshold crossed.
     */
    private BudgetAlert(int budgetId, int userId, double percentageReached, String message) {
        this.alertID = 0;
        this.budgetId = budgetId;
        this.userId = userId;
        this.percentageReached = percentageReached;
        this.triggeredAt = LocalDateTime.now();
        this.message = message;
    }

    /**
     * Generates a BudgetAlert and sends it.
     * SD5: Budget -> BudgetAlert.generate() -> send() -> Notification.send()
     * 
     * @param budget The budget that triggered the alert.
     * @param percentageReached The percentage of the threshold reached (e.g., 0.8 for 80%).
     * @param message The descriptive message for the alert.
     * @return The newly generated BudgetAlert object.
     */
    public static BudgetAlert generate(Budget budget, double percentageReached, String message) {
        BudgetAlert alert = new BudgetAlert(
                budget.getBudgetID(),
                budget.getUserId(),
                percentageReached * 100,
                message);

        System.out.println("[BudgetAlert] Generated: " + message);
        alert.send();
        return alert;
    }

    /**
     * Persists the alert to budget_alerts table and creates a Notification.
     * SD5: BudgetAlert.send() -> Notification.send()
     */
    public void send() {
        String createdAt = triggeredAt.format(FMT);
        String sql = "INSERT INTO budget_alerts (budgetId, userId, percentageReached, triggeredAt, message) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, budgetId);
            ps.setInt(2, userId);
            ps.setDouble(3, percentageReached);
            ps.setString(4, createdAt);
            ps.setString(5, message);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                this.alertID = keys.getInt(1);
            System.out.println("[DB] Budget alert saved: id=" + alertID);
        } catch (SQLException e) {
            System.err.println("[DB] Error saving budget alert: " + e.getMessage());
        }
        // Trigger notification
        Notification.send(userId, message, "Budget", budgetId);
    }

    /**
     * Returns a string representation of the BudgetAlert.
     * 
     * @return A formatted string detailing the alert ID, percentage, message, and trigger time.
     */
    @Override
    public String toString() {
        String time = triggeredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return String.format("  [Alert #%d] %.1f%% - %s  (%s)",
                alertID, percentageReached, message, time);
    }

    // Getters

    /**
     * Gets the unique ID of the alert.
     * 
     * @return The alert ID.
     */
    public int getAlertID() {
        return alertID;
    }

    /**
     * Gets the ID of the associated budget.
     * 
     * @return The budget ID.
     */
    public int getBudgetId() {
        return budgetId;
    }

    /**
     * Gets the ID of the user who owns the budget.
     * 
     * @return The user ID.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the percentage of the budget reached.
     * 
     * @return The percentage reached.
     */
    public double getPercentageReached() {
        return percentageReached;
    }

    /**
     * Gets the timestamp when the alert was triggered.
     * 
     * @return The triggered timestamp.
     */
    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    /**
     * Gets the alert message.
     * 
     * @return The alert message.
     */
    public String getMessage() {
        return message;
    }
}
