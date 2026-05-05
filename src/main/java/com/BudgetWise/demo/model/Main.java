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
                System.out.println("17. Edit Profile & Settings");
                System.out.println("18. Transaction History");
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
                try {
                    System.out.print("Type (Income/Expense): ");
                    String type = sc.nextLine();
                    System.out.print("Amount: ");
                    double amt = Double.parseDouble(sc.nextLine());
                    System.out.print("Category: ");
                    String cat = sc.nextLine();
                    Transaction t = new Transaction(currentUser.getUserId(), type, amt, cat);
                    t.save();
                    currentUser.updateBalance(amt, type);
                    System.out.println("Saved! New Balance: " + currentUser.getNetBalance() + " EGP");
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
                    String gname = sc.nextLine();
                    System.out.print("Target: ");
                    double target = Double.parseDouble(sc.nextLine());
                    System.out.print("Initial Contribution: ");
                    double init = Double.parseDouble(sc.nextLine());
                    System.out.print("Deadline (YYYY-MM-DD): ");
                    String dead = sc.nextLine();
                    FinancialGoal goal = new FinancialGoal(currentUser.getUserId(), gname, target,
                            LocalDate.parse(dead), init);
                    goal.save();
                    System.out.println("Goal Created!");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                // 7. VIEW GOALS
            } else if (choice.equals("7") && currentUser != null) {
                List<FinancialGoal> goals = FinancialGoal.loadForUser(currentUser.getUserId());
                if (goals.isEmpty())
                    System.out.println("No goals found.");
                else
                    goals.forEach(System.out::println);

                // 8. DASHBOARD
            } else if (choice.equals("8") && currentUser != null) {
                Dashboard d = new Dashboard(currentUser.getUserId(), currentUser.getFullName());
                d.load();
                d.display();

                // 9. NOTIFICATIONS
            } else if (choice.equals("9") && currentUser != null) {
                List<Notification> list = Notification.fetchForUser(currentUser.getUserId());
                if (list.isEmpty())
                    System.out.println("No notifications.");
                else {
                    list.forEach(System.out::println);
                    System.out.print("Mark all as read? (y/n): ");
                    if (sc.nextLine().equalsIgnoreCase("y")) {
                        list.forEach(n -> {
                            if (!n.isRead())
                                n.markAsRead();
                        });
                    }
                }

                // 10. CREATE BUDGET
            } else if (choice.equals("10") && currentUser != null) {
                try {
                    System.out.print("Category: ");
                    String bcat = sc.nextLine();
                    System.out.print("Limit Amount: ");
                    double lim = Double.parseDouble(sc.nextLine());
                    System.out.print("Alert Threshold % (e.g. 90): ");
                    double thr = Double.parseDouble(sc.nextLine()) / 100.0;
                    Budget b = new Budget(currentUser.getUserId(), bcat, lim, YearMonth.now().atDay(1),
                            YearMonth.now().atEndOfMonth(), thr);
                    if (b.create())
                        System.out.println("Budget Created Successfully!");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                // 11. VIEW/EDIT BUDGETS
            } else if (choice.equals("11") && currentUser != null) {
                List<Budget> budgets = Budget.loadForUser(currentUser.getUserId());
                if (budgets.isEmpty())
                    System.out.println("No budgets found.");
                else
                    budgets.forEach(System.out::println);

                // 12. VIEW REPORTS
            } else if (choice.equals("12") && currentUser != null) {
                Report r = new Report(currentUser.getUserId(), YearMonth.now().atDay(1),
                        YearMonth.now().atEndOfMonth());
                r.generate();
                r.display();

                // 13. EDIT TRANSACTION
            } else if (choice.equals("13") && currentUser != null) {
                try {
                    List<Transaction> txns = Transaction.loadAllForUser(currentUser.getUserId());
                    txns.forEach(t -> System.out.println(t.toDetailString()));
                    System.out.print("Enter Transaction ID to edit: ");
                    int id = Integer.parseInt(sc.nextLine());
                    Transaction t = Transaction.loadById(id);
                    if (t != null && t.getUserId() == currentUser.getUserId()) {
                        System.out.print("New Amount: ");
                        double amt = Double.parseDouble(sc.nextLine());
                        System.out.print("New Category: ");
                        String cat = sc.nextLine();
                        if (t.edit(amt, cat))
                            System.out.println("Transaction Updated!");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                // 14. DELETE TRANSACTION
            } else if (choice.equals("14") && currentUser != null) {
                try {
                    System.out.print("Enter ID to delete: ");
                    int id = Integer.parseInt(sc.nextLine());
                    if (Transaction.delete(id))
                        System.out.println("Deleted Successfully!");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                // 15. EXPORT REPORT
            } else if (choice.equals("15") && currentUser != null) {
                ExportReport er = new ExportReport(currentUser.getUserId(), YearMonth.now().atDay(1),
                        YearMonth.now().atEndOfMonth(), true, true, true);
                if (er.validateRequest("PDF")) {
                    er.generate();
                    er.download();
                }

                // 16. SYNC BANK ACCOUNT

                // 17. EDIT PROFILE & SETTINGS
            } else if (choice.equals("17") && currentUser != null) {
                Userprofile profile = Userprofile.getProfile(currentUser.getUserId());
                if (profile != null) {
                    System.out.print("New Name [" + profile.getDisplayName() + "]: ");
                    String n = sc.nextLine().trim();
                    if (n.isEmpty())
                        n = profile.getDisplayName();
                    System.out.print("New Currency [" + profile.getCurrency() + "]: ");
                    String c = sc.nextLine().trim();
                    if (c.isEmpty())
                        c = profile.getCurrency();

                    new Userprofile(currentUser.getUserId(), n, c).update();
                    currentUser = auth.login(currentUser.getEmail(), currentUser.getPassword());
                }

                // 18. TRANSACTION HISTORY
            } else if (choice.equals("18") && currentUser != null) {
                System.out.println("\n1. All History\n2. Filter by Category");
                System.out.print("Choice: ");
                String sub = sc.nextLine();
                List<Transaction> hist;
                if (sub.equals("2")) {
                    System.out.print("Category: ");
                    hist = Transaction.loadByCategory(currentUser.getUserId(), sc.nextLine());
                } else {
                    hist = Transaction.loadAllForUser(currentUser.getUserId());
                }

                if (hist.isEmpty())
                    System.out.println("No records found.");
                else
                    hist.forEach(t -> System.out.println(t.toDetailString()));

            } else {
                System.out.println("Invalid option or not logged in.");
            }
        }
        System.out.println("Program Closed.");
        sc.close();
    }
}