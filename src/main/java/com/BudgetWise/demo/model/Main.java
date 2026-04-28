package com.BudgetWise.demo.model;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
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
            }
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            // 1. SIGN UP
            if (choice.equals("1")) {
                System.out.print("Full Name: "); String name = sc.nextLine();
                System.out.print("Email: "); String email = sc.nextLine();
                System.out.print("Password: "); String p1 = sc.nextLine();
                System.out.print("Confirm Password: "); String p2 = sc.nextLine();
                System.out.println(auth.signUp(name, email, p1, p2));

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
                // Inside Choice "4" (Add Transaction)
            } else if (choice.equals("4") && currentUser != null) {
                // 1. Ask for Type First
                System.out.print("Transaction Type (Income/Expense): ");
                String type = sc.nextLine();

                // 2. Ask for Amount
                System.out.print("Amount: ");
                double amt = sc.nextDouble();
                sc.nextLine(); // Clear buffer

                // 3. Ask for Category
                System.out.print("Category: ");
                String cat = sc.nextLine();

                try {
                    // Create the transaction
                    Transaction t = new Transaction(type, amt, cat);

                    // Update the user's actual balance
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
            }
        }
        System.out.println("Program Closed.");
        sc.close();
    }
}