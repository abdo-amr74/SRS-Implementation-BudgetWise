package com.BudgetWise.demo.model;
import java.util.ArrayList;
import java.util.List;

public class AuthService {
    private List<User> database = new ArrayList<>(); // Temporary storage [cite: 85]

    // User Story #1: Sign-Up
    public String signUp(String name, String email, String pass, String confirmPass) {
        if (!pass.equals(confirmPass)) {
            return "Error: Passwords do not match."; //
        }
        if (pass.length() < 8) {
            return "Error: Password must be at least 8 characters."; // [cite: 93]
        }

        User newUser = new User(name, email, pass);
        database.add(newUser);
        return "Account created successfully for " + name; // [cite: 81]
    }

    // User Story #2: Login
    public User login(String email, String pass) {
        for (User user : database) {
            if (user.getEmail().equalsIgnoreCase(email) && user.getPassword().equals(pass)) {
                return user; // Successful authentication [cite: 103]
            }
        }
        System.out.println("Error: Invalid email or password. Please try again."); // [cite: 107]
        return null;
    }
}
