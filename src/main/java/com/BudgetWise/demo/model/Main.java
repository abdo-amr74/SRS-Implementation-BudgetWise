package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();
        AuthService auth = new AuthService();
        Scanner sc = new Scanner(System.in);
        User currentUser = null;
        while (true) {
            System.out.println("\n=== BUDGET WISE SYSTEM ===");
            if (currentUser == null) {
                System.out.println("1. Sign Up");
                System.out.println("2. Login");
                System.out.println("3. Exit");
            } else {
                System.out.println("Logged in as: " + currentUser.getFullName());
                System.out.println("3. Exit");
                System.out.println("4. Add Transaction");
                System.out.println("5. Logout");
                System.out.println("6. Add Financial Goal");
                System.out.println("7. View My Goals");
                System.out.println("8. Dashboard");
                System.out.println("9. Notifications");
                System.out.println("10. Create Budget");
                System.out.println("11. View/Edit Budgets");
                System.out.println("12. View Reports");      
                System.out.println("13. Edit Transaction");
                System.out.println("14. Delete Transaction");
                System.out.println("15. Export Report");      
                System.out.println("16. Sync Bank Account"); 
            }
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            // 1. SIGN UP
            if (choice.equals("1")) {
                System.out.print("Full Name: ");
                String name = sc.nextLine();
                System.out.print("Email: ");
                String email = sc.nextLine();
                System.out.print("Password: ");
                String p1 = sc.nextLine();
                System.out.print("Confirm Password: ");
                String p2 = sc.nextLine();
                String result = auth.signUp(name, email, p1, p2);
                System.out.println(result);
                if (result.toLowerCase().contains("success")) {
                    currentUser = auth.login(email, p1);
                    if (currentUser != null) {
                        System.out.println("Welcome " + currentUser.getFullName() + "!");
                        Dashboard d = new Dashboard(
                                currentUser.getUserId(),
                                currentUser.getFullName());
                        d.load();
                        d.display();
                    }
                }

            // 2. LOGIN
            } else if (choice.equals("2")) {
                System.out.print("Email: ");
                String lemail = sc.nextLine();
                System.out.print("Pass: ");
                String lpass = sc.nextLine();
                currentUser = auth.login(lemail, lpass);
                if (currentUser != null)
                    System.out.println("Login Successful!");

            // 3. EXIT
            } else if (choice.equals("3")) {
                break;

            // 4. ADD TRANSACTION
            } else if (choice.equals("4") && currentUser != null) {
                System.out.print("Transaction Type (Income/Expense): ");
                String type = sc.nextLine();
                System.out.print("Amount: ");
                double amt = sc.nextDouble();
                sc.nextLine();
                System.out.print("Category: ");
                String cat = sc.nextLine();
                try {
                    Transaction t = new Transaction(currentUser.getUserId(), type, amt, cat);
                    t.save();
                    currentUser.updateBalance(amt, type);
                    System.out.println("Saved: " + t.toString());
                    System.out.println("New Balance: " + currentUser.getNetBalance() + " EGP");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

            // 5. LOGOUT
            } else if (choice.equals("5")) {
                currentUser = null;
                System.out.println("Logged out.");

            // 6. ADD FINANCIAL GOAL
            } else if (choice.equals("6") && currentUser != null) {
                try {
                    System.out.print("Goal Name: ");
                    String name = sc.nextLine();
                    System.out.print("Target Amount: ");
                    double target = sc.nextDouble();
                    System.out.print("Initial Contribution: ");
                    double init = sc.nextDouble();
                    sc.nextLine();
                    System.out.print("Deadline (YYYY-MM-DD): ");
                    String d = sc.nextLine();
                    FinancialGoal goal = new FinancialGoal(
                            currentUser.getUserId(),
                            name,
                            target,
                            java.time.LocalDate.parse(d),
                            init);
                    goal.save();
                    Notification.send(currentUser.getUserId(),
                            "New goal created: " + name,
                            "Goal",
                            goal.getGoalId());
                    System.out.println("Goal Created Successfully!");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

            // 7. VIEW GOALS
            } else if (choice.equals("7") && currentUser != null) {
                var goals = FinancialGoal.loadForUser(currentUser.getUserId());
                if (goals.isEmpty()) {
                    System.out.println("No goals found.");
                } else {
                    goals.forEach(g -> System.out.println(g));
                }

            // 8. DASHBOARD
            } else if (choice.equals("8") && currentUser != null) {
                Dashboard d = new Dashboard(
                        currentUser.getUserId(),
                        currentUser.getFullName());
                d.load();
                d.display();

            // 9. NOTIFICATIONS
            } else if (choice.equals("9") && currentUser != null) {
                var list = Notification.fetchForUser(currentUser.getUserId());
                if (list.isEmpty()) {
                    System.out.println("No notifications.");
                } else {
                    for (Notification n : list) {
                        System.out.println(n);
                    }
                    System.out.print("Mark all as read? (y/n): ");
                    String ans = sc.nextLine();
                    if (ans.equalsIgnoreCase("y")) {
                        for (Notification n : list) {
                            if (!n.isRead())
                                n.markAsRead();
                        }
                    }
                }

            // 10. CREATE BUDGET
            } else if (choice.equals("10") && currentUser != null) {
                try {
                    System.out.print("Budget Category (e.g., Food & Dining, Transport): ");
                    String budgetName = sc.nextLine();
                    System.out.print("Budget Amount (EGP): ");
                    double budgetAmount = sc.nextDouble();
                    sc.nextLine();
                    System.out.print("Start Date (YYYY-MM-DD) [Enter for 1st of current month]: ");
                    String startInput = sc.nextLine().trim();
                    LocalDate startDate = startInput.isEmpty()
                            ? YearMonth.now().atDay(1)
                            : LocalDate.parse(startInput);
                    System.out.print("End Date (YYYY-MM-DD) [Enter for end of current month]: ");
                    String endInput = sc.nextLine().trim();
                    LocalDate endDate = endInput.isEmpty()
                            ? YearMonth.now().atEndOfMonth()
                            : LocalDate.parse(endInput);
                    System.out.print("Alert Threshold % (e.g., 90 for 90%): ");
                    double threshold = sc.nextDouble() / 100.0;
                    sc.nextLine();
                    Budget budget = new Budget(
                            currentUser.getUserId(),
                            budgetName,
                            budgetAmount,
                            startDate,
                            endDate,
                            threshold);
                    boolean created = budget.create();
                    if (created) {
                        System.out.println("Budget created successfully!");
                        System.out.println(budget);
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    sc.nextLine();
                }

            // 11. VIEW/EDIT BUDGETS
            } else if (choice.equals("11") && currentUser != null) {
                List<Budget> budgets = Budget.loadForUser(currentUser.getUserId());
                if (budgets.isEmpty()) {
                    System.out.println("No budgets found. Create one first (option 10).");
                } else {
                    System.out.println("\n--- Your Budgets ---");
                    for (Budget b : budgets) {
                        System.out.println(b);
                        System.out.println();
                    }
                    System.out.print("Edit a budget? Enter Budget ID (or 0 to cancel): ");
                    int editId = sc.nextInt();
                    sc.nextLine();
                    if (editId > 0) {
                        Budget toEdit = Budget.findById(editId);
                        if (toEdit == null || toEdit.getUserId() != currentUser.getUserId()) {
                            System.out.println("Budget not found.");
                        } else {
                            System.out.println("Editing: " + toEdit.getName());
                            System.out.print("New Category Name [Enter to keep \"" + toEdit.getName() + "\"]: ");
                            String newName = sc.nextLine().trim();
                            if (newName.isEmpty()) newName = toEdit.getName();
                            System.out.print("New Limit Amount [Enter to keep " + toEdit.getLimitAmount() + "]: ");
                            String newLimitStr = sc.nextLine().trim();
                            double newLimit = newLimitStr.isEmpty()
                                    ? toEdit.getLimitAmount()
                                    : Double.parseDouble(newLimitStr);
                            System.out.print("New Start Date [Enter to keep " + toEdit.getStartDate() + "]: ");
                            String newStartStr = sc.nextLine().trim();
                            LocalDate newStart = newStartStr.isEmpty()
                                    ? toEdit.getStartDate()
                                    : LocalDate.parse(newStartStr);
                            System.out.print("New End Date [Enter to keep " + toEdit.getEndDate() + "]: ");
                            String newEndStr = sc.nextLine().trim();
                            LocalDate newEnd = newEndStr.isEmpty()
                                    ? toEdit.getEndDate()
                                    : LocalDate.parse(newEndStr);
                            System.out.print("New Alert Threshold % [Enter to keep "
                                    + (toEdit.getAlertThreshold() * 100) + "%]: ");
                            String newThreshStr = sc.nextLine().trim();
                            double newThresh = newThreshStr.isEmpty()
                                    ? toEdit.getAlertThreshold()
                                    : Double.parseDouble(newThreshStr) / 100.0;
                            toEdit.edit(newName, newLimit, newStart, newEnd, newThresh);
                            System.out.println("Budget updated!");
                            System.out.println(toEdit);
                        }
                    }
                }

            // 12. VIEW REPORTS (US8)
            } else if (choice.equals("12") && currentUser != null) {
                try {
                    System.out.println("\n--- Financial Report ---");
                    System.out.print("Start Date (YYYY-MM-DD) [Enter for 1st of current month]: ");
                    String rStartInput = sc.nextLine().trim();
                    LocalDate rStart = rStartInput.isEmpty()
                            ? YearMonth.now().atDay(1)
                            : LocalDate.parse(rStartInput);
                    System.out.print("End Date (YYYY-MM-DD) [Enter for end of current month]: ");
                    String rEndInput = sc.nextLine().trim();
                    LocalDate rEnd = rEndInput.isEmpty()
                            ? YearMonth.now().atEndOfMonth()
                            : LocalDate.parse(rEndInput);
                    Report report = new Report(currentUser.getUserId(), rStart, rEnd);
                    report.generate();
                    report.display();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

            // 13. EDIT TRANSACTION
            } else if (choice.equals("13") && currentUser != null) {
                try {
                    List<Transaction> txns = Transaction.loadAllForUser(currentUser.getUserId());
                    if (txns.isEmpty()) {
                        System.out.println("No transactions found.");
                    } else {
                        System.out.println("\n--- Your Transactions ---");
                        for (Transaction t : txns) {
                            System.out.println(t.toDetailString());
                        }
                        System.out.print("Enter Transaction ID to edit (or 0 to cancel): ");
                        int txId = sc.nextInt();
                        sc.nextLine();
                        if (txId > 0) {
                            Transaction t = Transaction.loadById(txId);
                            if (t == null || t.getUserId() != currentUser.getUserId()) {
                                System.out.println("Transaction not found.");
                            } else {
                                System.out.print("New Amount [Enter to keep " + t.getAmount() + "]: ");
                                String newAmtStr = sc.nextLine().trim();
                                double newAmt = newAmtStr.isEmpty()
                                        ? t.getAmount()
                                        : Double.parseDouble(newAmtStr);
                                System.out.print("New Category [Enter to keep \"" + t.getCategory() + "\"]: ");
                                String newCat = sc.nextLine().trim();
                                if (newCat.isEmpty()) newCat = t.getCategory();
                                t.edit(newAmt, newCat);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    sc.nextLine();
                }

            // 14. DELETE TRANSACTION
            } else if (choice.equals("14") && currentUser != null) {
                try {
                    List<Transaction> txns = Transaction.loadAllForUser(currentUser.getUserId());
                    if (txns.isEmpty()) {
                        System.out.println("No transactions found.");
                    } else {
                        System.out.println("\n--- Your Transactions ---");
                        for (Transaction t : txns) {
                            System.out.println(t.toDetailString());
                        }
                        System.out.print("Enter Transaction ID to delete (or 0 to cancel): ");
                        int txId = sc.nextInt();
                        sc.nextLine();
                        if (txId > 0) {
                            Transaction t = Transaction.loadById(txId);
                            if (t == null || t.getUserId() != currentUser.getUserId()) {
                                System.out.println("Transaction not found.");
                            } else {
                                System.out.print("Are you sure? (y/n): ");
                                String confirm = sc.nextLine();
                                if (confirm.equalsIgnoreCase("y")) {
                                    Transaction.delete(txId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    sc.nextLine();
                }

            // 15. EXPORT REPORT 
            } else if (choice.equals("15") && currentUser != null) {
                try {
                    System.out.print("Export format (PDF/CSV/Excel): ");
                    String fmt = sc.nextLine();
                    ExportReport er = new ExportReport(currentUser.getUserId());
                    if (er.validateRequest(fmt)) {
                        er.formatData();
                        er.download();
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

            // 16. SYNC BANK ACCOUNT  
            } else if (choice.equals("16") && currentUser != null) {
                try {
                    System.out.print("Bank name: ");
                    String bank = sc.nextLine();
                    System.out.print("Access token: ");
                    String token = sc.nextLine();
                    BankAccount ba = new BankAccount(
                            currentUser.getUserId(),
                            bank,
                            "http://bank-api.example.com");
                    ba.syncTransactions(token, currentUser);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

            } else {
                System.out.println("Invalid option.");
            }
        }
        System.out.println("Program Closed.");
        sc.close();
    }
}