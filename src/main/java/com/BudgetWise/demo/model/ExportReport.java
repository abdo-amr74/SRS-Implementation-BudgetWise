package com.BudgetWise.demo.model;

import java.time.LocalDate;
import java.util.List;

/**
 * US #12 / SD12 — Export Financial Data
 *
 * Class Diagram attributes:
 *   format : String, startDate : Date, endDate : Date,
 *   includeTransactions : bool, includeBudgets : bool, includeGoals : bool
 *
 * Class Diagram methods:
 *   validateRequest() : void, generate() : void,
 *   formatData() : File, download() : void
 *
 * SD12 flow:
 *   User → set format, dateRange, inclusions
 *   User → click "Generate & Download"
 *   validateRequest() → generate() → Report.generate() →
 *   Report fetches from DB → reportData → formatData() → download() → User
 */
public class ExportReport {
    private String format;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean includeTransactions;
    private boolean includeBudgets;
    private boolean includeGoals;
    private int userId;

    // Report instance used for data fetching (SD12 steps 4–7)
    private Report report;
    private List<Transaction> transactions;

    public ExportReport(int userId, LocalDate startDate, LocalDate endDate,
                        boolean includeTransactions, boolean includeBudgets,
                        boolean includeGoals) {
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.includeTransactions = includeTransactions;
        this.includeBudgets = includeBudgets;
        this.includeGoals = includeGoals;
    }

    // SD12 step 3: validateRequest() — validates format and data availability
    public boolean validateRequest(String format) {
        if (!format.equalsIgnoreCase("PDF") &&
            !format.equalsIgnoreCase("CSV") &&
            !format.equalsIgnoreCase("Excel")) {
            System.out.println("Error: Invalid format. Choose PDF, CSV, or Excel.");
            return false;
        }
        this.format = format;

        if (!includeTransactions && !includeBudgets && !includeGoals) {
            System.out.println("Error: At least one data type must be selected.");
            return false;
        }
        return true;
    }

    // SD12 steps 4–7: generate() → Report.generate() → fetchTransactions(period) → reportData
    public void generate() {
        // SD12 step 4: ExportReport calls Report.generate()
        this.report = new Report(userId, startDate, endDate);
        report.generate();

        // SD12 steps 5–7: Report fetches from DB, returns reportData
        this.transactions = report.getTransactions();

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("Warning: No transactions found for the selected period.");
        } else {
            System.out.println("Report data loaded: " + transactions.size() + " transactions.");
        }
    }

    // SD12 step 8: formatData() — formats to PDF/CSV/Excel
    public void formatData() {
        System.out.println("\n--- Exported Report (" + format.toUpperCase() + ") ---");
        System.out.println("Period: " + startDate + " to " + endDate);

        // Include transactions if selected
        if (includeTransactions && transactions != null && !transactions.isEmpty()) {
            System.out.println("\n[Transactions]");
            System.out.println("Formatting " + transactions.size()
                             + " transactions as " + format + "...");
            for (Transaction t : transactions) {
                System.out.println("  | " + t.toDetailString());
            }
        }

        // Include budgets if selected
        if (includeBudgets) {
            List<Budget> budgets = Budget.loadForUser(userId);
            if (!budgets.isEmpty()) {
                System.out.println("\n[Budgets]");
                for (Budget b : budgets) {
                    System.out.println("  | " + b);
                }
            }
        }

        // Include goals if selected
        if (includeGoals) {
            List<FinancialGoal> goals = FinancialGoal.loadForUser(userId);
            if (!goals.isEmpty()) {
                System.out.println("\n[Financial Goals]");
                for (FinancialGoal g : goals) {
                    System.out.println("  | " + g);
                }
            }
        }
    }

    // SD12 steps 9–10: download() — initiates file download
    public void download() {
        System.out.println("\nFile 'report." + format.toLowerCase()
                         + "' downloaded successfully!");
    }

    // Getters
    public String getFormat() {
        return format;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isIncludeTransactions() {
        return includeTransactions;
    }

    public boolean isIncludeBudgets() {
        return includeBudgets;
    }

    public boolean isIncludeGoals() {
        return includeGoals;
    }
}