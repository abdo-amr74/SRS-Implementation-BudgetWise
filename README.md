# 💰 BudgetWise

A personal finance management system built with **Java** and **SQLite**. BudgetWise helps users track income and expenses, set budgets with smart alerts, manage financial goals, and generate reports — all from a clean command-line interface.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Usage Guide](#-usage-guide)
- [Database Schema](#-database-schema)

---

## ✨ Features

| Feature | Description |
|---|---|
| **User Authentication** | Sign up, log in, and log out with email/password authentication |
| **Transaction Management** | Add, edit, delete, and browse income/expense transactions |
| **Budget Tracking** | Create budgets per category with spending limits and date ranges |
| **Budget Alerts** | Automatic notifications when spending approaches or exceeds a configurable threshold |
| **Financial Goals** | Set savings goals with target amounts, deadlines, and progress tracking |
| **Dashboard** | At-a-glance summary of balances, recent activity, and goal progress |
| **Reports & Analytics** | Generate monthly reports with income/expense breakdowns |
| **Export Reports** | Export financial reports to PDF |
| **Notifications** | In-app notification centre with read/unread management |
| **Profile & Settings** | Edit display name and preferred currency |
| **Transaction History** | View full history or filter transactions by category |

---

## 🛠 Tech Stack

- **Language:** Java 25
- **Build Tool:** Apache Maven
- **Database:** SQLite (via [xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) `3.45.1.0`)
- **Testing:** JUnit 5.10.0

---

## 📁 Project Structure

```
BudgetWise-A2/
├── demo/
│   ├── pom.xml                          # Maven build configuration
│   ├── mvnw / mvnw.cmd                  # Maven wrapper scripts
│   ├── budgetwise.db                    # SQLite database (auto-created)
│   └── src/main/java/com/BudgetWise/demo/
│       ├── database/
│       │   └── DatabaseManager.java     # DB connection & schema initialisation
│       ├── model/
│       │   ├── Main.java                # Application entry point & CLI menu
│       │   ├── AuthService.java         # Sign-up & login logic
│       │   ├── User.java                # User entity & balance management
│       │   ├── Userprofile.java         # Profile settings (name, currency)
│       │   ├── Transaction.java         # Income/expense CRUD operations
│       │   ├── Budget.java              # Budget creation, tracking & threshold checks
│       │   ├── BudgetAlert.java         # Over-limit alert generation
│       │   ├── FinancialGoal.java       # Savings goal tracking
│       │   ├── Dashboard.java           # Summary dashboard view
│       │   ├── Report.java              # Monthly report generation
│       │   ├── ExportReport.java        # PDF report export
│       │   ├── Notification.java        # Notification CRUD & display
│       │   ├── BankAccount.java         # Bank account linking (stub)
│       │   ├── Category.java            # Category model
│       │   └── UserSetting.java         # User settings model
│       └── util/
│           └── SessionManager.java      # Session management utility
└── README.md
```

---

## 📦 Prerequisites

- **Java JDK 25** (or compatible version)
- **Apache Maven 3.8+** (or use the included Maven wrapper)

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/BudgetWise-A2.git
cd BudgetWise-A2/demo
```

### 2. Build the project

```bash
# Using the Maven wrapper (no Maven install required)
./mvnw clean package        # macOS / Linux
mvnw.cmd clean package      # Windows
```

### 3. Run the application

```bash
java -jar target/demo-1.0-SNAPSHOT.jar
```

> **Note:** The SQLite database file (`budgetwise.db`) is created automatically on first run in the working directory.

---

## 📖 Usage Guide

After launching, you'll see the main menu:

```
=== BUDGET WISE SYSTEM ===
1. Sign Up
2. Login
3. Exit
```

**Once logged in**, the full menu becomes available:

| Option | Action |
|--------|--------|
| `4` | Add a new transaction (Income / Expense) |
| `5` | Logout |
| `6` | Create a financial goal |
| `7` | View your goals |
| `8` | Open the dashboard |
| `9` | View & manage notifications |
| `10` | Create a new budget |
| `11` | View / edit existing budgets |
| `12` | View monthly reports |
| `13` | Edit a transaction |
| `14` | Delete a transaction |
| `15` | Export report to PDF |
| `17` | Edit profile & settings |
| `18` | Transaction history (all or filtered) |

---

## 🗄 Database Schema

BudgetWise uses SQLite with the following tables:

```
┌──────────────────┐     ┌──────────────────┐
│      users       │     │   transactions   │
├──────────────────┤     ├──────────────────┤
│ id (PK)          │◄────│ userId (FK)      │
│ fullName         │     │ id (PK)          │
│ email (UNIQUE)   │     │ amount           │
│ password         │     │ type             │
│ currency         │     │ category         │
│ createdAt        │     │ note             │
└──────────────────┘     │ date             │
        │                └──────────────────┘
        │
        ├────────────────────────┐
        │                        │
┌───────▼──────────┐    ┌────────▼─────────┐
│     budgets      │    │      goals       │
├──────────────────┤    ├──────────────────┤
│ id (PK)          │    │ id (PK)          │
│ userId (FK)      │    │ userId (FK)      │
│ name             │    │ name             │
│ amount           │    │ targetAmount     │
│ spentAmount      │    │ currentAmount    │
│ startDate        │    │ deadline         │
│ endDate          │    │ status           │
│ alertThreshold   │    └──────────────────┘
└──────────────────┘
        │
┌───────▼──────────┐    ┌──────────────────┐
│  budget_alerts   │    │  notifications   │
├──────────────────┤    ├──────────────────┤
│ id (PK)          │    │ id (PK)          │
│ budgetId (FK)    │    │ userId (FK)      │
│ userId (FK)      │    │ type             │
│ percentageReached│    │ message          │
│ triggeredAt      │    │ isRead           │
│ message          │    │ createdAt        │
└──────────────────┘    └──────────────────┘
```

---

## 📄 License

This project is developed for academic purposes.
