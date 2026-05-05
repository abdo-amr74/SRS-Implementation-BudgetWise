package com.BudgetWise.demo.model;
import java.util.ArrayList;
import java.util.List;

public class BankAccount {
    private String bankName;
    private String accountURL;
    private int userId;

    public BankAccount(int userId, String bankName, String accountURL) {
        this.userId = userId;
        this.bankName = bankName;
        this.accountURL = accountURL;
    }

    //  syncTransactions(accessToken)
    public void syncTransactions(String accessToken, User user) {
        System.out.println("Connecting to " + bankName + "...");

        // Simulated bank API response
        List<Transaction> bankTransactions = new ArrayList<>();
        bankTransactions.add(new Transaction(userId, "Income",  5000, "Salary"));
        bankTransactions.add(new Transaction(userId, "Expense", 300,  "Groceries"));
        bankTransactions.add(new Transaction(userId, "Expense", 150,  "Transport"));

        //  check if sync failed
        if (bankTransactions.isEmpty()) {
            System.out.println("Sync Failed. Could not reach bank API.");
            return;
        }

        //  loop → for each transaction → save() to DB
        System.out.println("Syncing " + bankTransactions.size() + " transactions...");
        for (Transaction t : bankTransactions) {
            t.save();                              // saves to DB
            user.updateBalance(t.getAmount(), t.getType());
            System.out.println("  Saved: " + t);
        }

        System.out.println("Bank account synced successfully!");
    }
}