package com.BudgetWise.demo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database connections and initialization.
 */
public class DatabaseManager {
        private static final String DB_URL = "jdbc:sqlite:budgetwise.db";
        private static Connection connection;

        /**
         * Gets a connection to the SQLite database.
         * 
         * @return The database Connection.
         * @throws SQLException If a database access error occurs.
         */
        public static Connection getConnection() throws SQLException {
                if (connection == null || connection.isClosed()) {
                        connection = DriverManager.getConnection(DB_URL);
                }
                return connection;
        }

        /**
         * Initializes the database schema, creating necessary tables if they don't exist.
         */
        public static void initializeDatabase() {
                try (Connection conn = getConnection();
                                Statement stmt = conn.createStatement()) {

                        // Create users table with currency column[cite: 6]
                        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "fullName TEXT NOT NULL," +
                                        "email TEXT UNIQUE NOT NULL," +
                                        "password TEXT NOT NULL," +
                                        "currency TEXT DEFAULT 'EGP'," +
                                        "createdAt TEXT NOT NULL)");

                        // MIGRATION: Add currency column if the table already existed[cite: 6]
                        try {
                                stmt.execute("ALTER TABLE users ADD COLUMN currency TEXT DEFAULT 'EGP'");
                        } catch (SQLException e) {
                                // Column already exists, safe to ignore[cite: 6]
                        }

                        stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "userId INTEGER NOT NULL," +
                                        "amount REAL NOT NULL," +
                                        "type TEXT NOT NULL," +
                                        "category TEXT NOT NULL," +
                                        "note TEXT," +
                                        "date TEXT NOT NULL," +
                                        "FOREIGN KEY (userId) REFERENCES users(id))");

                        stmt.execute("CREATE TABLE IF NOT EXISTS budgets (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "userId INTEGER NOT NULL," +
                                        "name TEXT NOT NULL," +
                                        "amount REAL NOT NULL," +
                                        "spentAmount REAL DEFAULT 0," +
                                        "startDate TEXT NOT NULL," +
                                        "endDate TEXT NOT NULL," +
                                        "alertThreshold REAL NOT NULL," +
                                        "FOREIGN KEY (userId) REFERENCES users(id))");

                        stmt.execute("CREATE TABLE IF NOT EXISTS goals (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "userId INTEGER NOT NULL," +
                                        "name TEXT NOT NULL," +
                                        "targetAmount REAL NOT NULL," +
                                        "currentAmount REAL DEFAULT 0," +
                                        "deadline TEXT NOT NULL," +
                                        "status TEXT DEFAULT 'active'," +
                                        "FOREIGN KEY (userId) REFERENCES users(id))");

                        stmt.execute("CREATE TABLE IF NOT EXISTS budget_alerts (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "budgetId INTEGER NOT NULL," +
                                        "userId INTEGER NOT NULL," +
                                        "percentageReached REAL NOT NULL," +
                                        "triggeredAt TEXT NOT NULL," +
                                        "message TEXT NOT NULL," +
                                        "FOREIGN KEY (budgetId) REFERENCES budgets(id)," +
                                        "FOREIGN KEY (userId) REFERENCES users(id))");

                        stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "userId INTEGER NOT NULL," +
                                        "type TEXT NOT NULL," +
                                        "message TEXT NOT NULL," +
                                        "isRead INTEGER DEFAULT 0," +
                                        "createdAt TEXT NOT NULL," +
                                        "FOREIGN KEY (userId) REFERENCES users(id))");

                        System.out.println("Database initialized successfully.");

                } catch (SQLException e) {
                        System.err.println("DB init error: " + e.getMessage());
                }
        }
}