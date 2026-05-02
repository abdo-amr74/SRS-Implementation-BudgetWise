package com.BudgetWise.demo.model;

import com.BudgetWise.demo.database.DatabaseManager;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Initialize the database tables before anything else
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

                //NEW FEATURES
                System.out.println("6. Add Financial Goal");
                System.out.println("7. View My Goals");
                System.out.println("8. Dashboard");
                System.out.println("9. Notifications");
            }

            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            // 1. SIGN UP
            if (choice.equals("1")) {
                System.out.print("Full Name: "); String name = sc.nextLine();
                System.out.print("Email: "); String email = sc.nextLine();
                System.out.print("Password: "); String p1 = sc.nextLine();
                System.out.print("Confirm Password: "); String p2 = sc.nextLine();
                String result = auth.signUp(name, email, p1, p2); 
                System.out.println(result);


                if (result.toLowerCase().contains("success")) {
                    currentUser = auth.login(email, p1);

                if (currentUser != null) {
                    System.out.println("Welcome " + currentUser.getFullName() + "!");

        
                Dashboard d = new Dashboard(
                    currentUser.getUserId(),
                    currentUser.getFullName()
                );

                d.load();
                d.display();
                }
                }

            // 2. LOGIN
            } else if (choice.equals("2")) {
                System.out.print("Email: "); String lemail = sc.nextLine();
                System.out.print("Pass: "); String lpass = sc.nextLine();
                currentUser = auth.login(lemail, lpass);
                if (currentUser != null) System.out.println("Login Successful!");

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
                    System.out.println("New Balance: " + currentUser.getBalance() + " EGP");

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
                            init
                    );

                    goal.save();
                    // Notification 
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
                        currentUser.getFullName()
                );

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
                            if (!n.isRead()) n.markAsRead();
                        }
                    }
                }

            } else {
                System.out.println("Invalid option.");
            }
        }

        System.out.println("Program Closed.");
        sc.close();
    }
}
