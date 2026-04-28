package com.BudgetWise.demo.model;
import java.util.Date;

public class Transaction {
    private String type; // "Income" or "Expense"
    private double amount;
    private String category;
    private Date date;

    public Transaction(String type, double amount, String category) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number greater than 0."); //
        }
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = new Date(); // Defaults to current date [cite: 129]
    }

    @Override
    public String toString() {
        return "[" + type + "] " + amount + " EGP - " + category;
    }
}