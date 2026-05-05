package com.BudgetWise.demo.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Report {
    private int userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Transaction> transactions;

    public Report(int userId, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * SD8: generate() loads transactions using the correct 3-parameter method.
     */
    public void generate() {
        // loadForUser(userId, start, end) exists in your Transaction class
        this.transactions = Transaction.loadForUser(
                userId,
                startDate.toString(),
                endDate.toString());
        System.out.println("Report generated for period: " + startDate + " to " + endDate);
    }

    /**
     * SD8: display() uses public getters from the Transaction class.
     */
    public void display() {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("No transactions found for this period.");
            return;
        }

        double totalIncome = 0;
        double totalExpenses = 0;
        Map<String, Double> breakdown = new HashMap<>();

        for (Transaction t : transactions) {
            // These getters must be public in Transaction.java
            String type = t.getType();
            double amount = t.getAmount();
            String category = t.getCategory();

            if (type.equalsIgnoreCase("Income")) {
                totalIncome += amount;
            } else {
                totalExpenses += amount;
            }

            breakdown.put(category, breakdown.getOrDefault(category, 0.0) + amount);
        }

        System.out.println("\n--- Income & Expense Report ---");
        System.out.println("Total Income:   " + totalIncome + " EGP");
        System.out.println("Total Expenses: " + totalExpenses + " EGP");
        System.out.println("Net Savings:    " + (totalIncome - totalExpenses) + " EGP");

        System.out.println("\n--- Category Breakdown ---");
        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " EGP");
        }
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}