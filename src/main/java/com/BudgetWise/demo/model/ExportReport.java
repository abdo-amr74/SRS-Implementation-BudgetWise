package com.BudgetWise.demo.model;
import java.util.List;

public class ExportReport {
    private String format;
    private int userId;
    private List<Transaction> transactions;

    public ExportReport(int userId) {
        this.userId = userId;
        this.transactions = Transaction.loadAllForUser(userId); 
    }

    // validateRequest()
    public boolean validateRequest(String format) {
        if (!format.equalsIgnoreCase("PDF") &&
            !format.equalsIgnoreCase("CSV") &&
            !format.equalsIgnoreCase("Excel")) {
            System.out.println("Error: Invalid format. Choose PDF, CSV, or Excel.");
            return false;
        }
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("Error: No transactions to export.");
            return false;
        }
        this.format = format;
        return true;
    }

    //  formatData()
    public void formatData() {
        System.out.println("Formatting " + transactions.size()
                         + " transactions as " + format + "...");
        for (Transaction t : transactions) {
            System.out.println("  | " + t.toDetailString());
        }
    }

    //   download()
    public void download() {
        System.out.println("File 'report." + format.toLowerCase()
                         + "' downloaded successfully!");
    }
}