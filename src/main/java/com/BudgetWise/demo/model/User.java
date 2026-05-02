package com.BudgetWise.demo.model;

public class User {
    private int userId; // Added userId field for DB reference
    private String fullName;
    private String email;
    private String password;
    private double balance; // Added balance field

    public User(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.balance = 0.0; // Starts at zero
    }

    public User(int userId, String fullName, String email, String password) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.balance = 0.0; // Starts at zero
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }

    public void updateBalance(double amount, String type) {
        if (type.equalsIgnoreCase("Income")) {
            this.balance += amount;
        } else if (type.equalsIgnoreCase("Expense")) {
            this.balance -= amount;
        }
    }
}