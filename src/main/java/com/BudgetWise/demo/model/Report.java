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

    // This constructor matches what Main.java uses:
    // new Report(currentUser.getUserId(), rStart, rEnd)
    public Report(int userId, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // SD8: generate() → loads transactions from DB
    public void generate() {
        this.transactions = Transaction.loadForUser(
            userId,
            startDate.toString(),
            endDate.toString()
        );
        System.out.println("Report generated for period: "
            + startDate + " to " + endDate);
    }

    // SD8: display() → prints income, expenses, breakdown
    public void display() {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("No transactions found for this period.");
            return;
        }

        double totalIncome = 0;
        double totalExpenses = 0;
        Map<String, Double> breakdown = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getType().equalsIgnoreCase("Income")) {
                totalIncome += t.getAmount();
            } else {
                totalExpenses += t.getAmount();
            }
            // Category breakdown
            String cat = t.getCategory();
            breakdown.put(cat, breakdown.getOrDefault(cat, 0.0) + t.getAmount());
        }

        // SD8: getIncomeAndExpense()
        System.out.println("\n--- Income & Expense Report ---");
        System.out.println("Total Income:   " + totalIncome + " EGP");
        System.out.println("Total Expenses: " + totalExpenses + " EGP");
        System.out.println("Net:            " + (totalIncome - totalExpenses) + " EGP");

        // SD8: getCategoryBreakdown()
        System.out.println("\n--- Category Breakdown ---");
        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " EGP");
        }
    }
}