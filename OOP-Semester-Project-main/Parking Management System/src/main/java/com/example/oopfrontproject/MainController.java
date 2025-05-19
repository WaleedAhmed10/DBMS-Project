package com.example.oopfrontproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MainController
{

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    private Connection connect() {
        try
        {
            return DriverManager.getConnection("jdbc:sqlite:space_saver.db");
        } catch (Exception e)
        {
            showAlert("Database Error", "Unable to connect to the database.");
            return null;
        }
    }

    @FXML
    void handleLogin(ActionEvent event)
    {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Username and password cannot be empty.");
            return;
        }

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT role FROM Users WHERE username = ? AND password = ?"))
        {
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next())
            {
                String role = rs.getString("role");
                showAlert("Login Successful", "Welcome, " + role + " user!");
            } else
            {
                showAlert("Login Failed", "Invalid username or password.");
            }

        } catch (Exception e)
        {
            showAlert("Login Error", "An error occurred during login.");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
