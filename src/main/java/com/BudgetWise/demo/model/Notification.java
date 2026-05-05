package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * US 11 — Notification System
 *
 * Sequence Diagram 11 methods:
 * send() → INSERT new notification into DB
 * markAsRead() → UPDATE isRead=1 in DB
 * fetchForUser() → SELECT all notifications for a user
 *
 * DB table used (from DatabaseManager):
 * notifications (id, userId, type, message, isRead, createdAt)
 */
public class Notification {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Fields
    private int notificationId;
    private int userId;
    private String message;
    private boolean isRead;
    private LocalDateTime timestamp;
    private String sourceType; // stored in DB "type" column: "Goal"|"Budget"|"System"
    private int sourceId; // not in DB schema — encoded into message if needed

    // Private constructor (used when loading from DB)
    private Notification(int notificationId, int userId, String sourceType,
            String message, boolean isRead, LocalDateTime timestamp) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceId = 0;
        this.message = message;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    // Core Methods (from Sequence Diagram 11)

    /**
     * Creates and persists a new notification for a user.
     * (Diagram: send("Goal created successfully") → INSERT into DB)
     *
     * @param userId     Target user's DB id
     * @param message    Notification text
     * @param sourceType "Goal" | "Budget" | "System" | "Transaction"
     * @param sourceId   ID of the originating entity
     */
    public static Notification send(int userId, String message, String sourceType, int sourceId) {
        String createdAt = LocalDateTime.now().format(FMT);
        String sql = "INSERT INTO notifications (userId, type, message, isRead, createdAt) "
                + "VALUES (?, ?, ?, 0, ?)";
        int generatedId = 0;
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, sourceType);
            ps.setString(3, message);
            ps.setString(4, createdAt);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                generatedId = keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB] Error saving notification: " + e.getMessage());
        }

        Notification n = new Notification(generatedId, userId, sourceType,
                message, false, LocalDateTime.parse(createdAt, FMT));
        n.sourceId = sourceId;
        System.out.println("[Notification] Sent to userId=" + userId + ": " + message);
        return n;
    }

    /** 
     * Convenience method: sends a system notification with no specific source. 
     *
     * @param userId The user ID.
     * @param message The notification message.
     * @return The generated Notification.
     */
    public static Notification send(int userId, String message) {
        return send(userId, message, "System", 0);
    }

    /**
     * Marks this notification as read and updates the DB.
     * (Diagram step 6→7: markAsRead() → Update isRead = true)
     */
    public void markAsRead() {
        this.isRead = true;
        String sql = "UPDATE notifications SET isRead=1 WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
            System.out.println("[Notification #" + notificationId + "] Marked as read.");
        } catch (SQLException e) {
            System.err.println("[DB] Error marking notification as read: " + e.getMessage());
        }
    }

    /**
     * Fetches all notifications for a given user from the DB.
     * (Diagram step 2→3: Fetch → List of Notification Objects)
     */
    public static List<Notification> fetchForUser(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, userId, type, message, isRead, createdAt "
                + "FROM notifications WHERE userId=? ORDER BY createdAt DESC";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Notification(
                        rs.getInt("id"),
                        rs.getInt("userId"),
                        rs.getString("type"),
                        rs.getString("message"),
                        rs.getInt("isRead") == 1,
                        LocalDateTime.parse(rs.getString("createdAt"), FMT)));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error fetching notifications: " + e.getMessage());
        }
        return list;
    }

    /** 
     * Counts unread notifications for a user (for badge display). 
     *
     * @param userId The user ID.
     * @return The count of unread notifications.
     */
    public static int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE userId=? AND isRead=0";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB] Error counting notifications: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Returns the navigation destination for this notification.
     * (Diagram step 8: Navigate to source)
     */
    public String getNavigationTarget() {
        return sourceType + (sourceId > 0 ? " #" + sourceId : "");
    }

    // Display
    @Override
    public String toString() {
        String statusLabel = isRead ? "[READ]  " : "[UNREAD]";
        String time = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return String.format("  #%d %s %s  (%s)", notificationId, statusLabel, message, time);
    }

    // Getters

    /**
     * Gets the notification ID.
     * @return The notification ID.
     */
    public int getNotificationId() {
        return notificationId;
    }

    /**
     * Gets the user ID.
     * @return The user ID.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the notification message.
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Checks if the notification is read.
     * @return true if read.
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Gets the timestamp of the notification.
     * @return The timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the source type of the notification.
     * @return The source type.
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * Gets the source ID of the notification.
     * @return The source ID.
     */
    public int getSourceId() {
        return sourceId;
    }
}