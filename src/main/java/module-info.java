module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.BudgetWise.demo to javafx.fxml;
    exports com.BudgetWise.demo;
}