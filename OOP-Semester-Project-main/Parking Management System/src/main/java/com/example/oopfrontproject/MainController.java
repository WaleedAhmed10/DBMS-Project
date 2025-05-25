package com.example.oopfrontproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.example.oopfrontproject.Main.ParkingManagementSystem;
import com.example.oopfrontproject.Main.Vehicle;
import com.example.oopfrontproject.Main.FourWheeler;
import com.example.oopfrontproject.Main.TwoWheeler;
import com.example.oopfrontproject.Main.ParkingSlot;

import java.sql.*;

public class MainController {

    @FXML
    private TextField vehicleNumberField;

    @FXML
    private TextField ownerNameField;

    @FXML
    private TextField contactNumberField;

    @FXML
    private ComboBox<String> vehicleTypeComboBox;

    @FXML
    private CheckBox hasCarrierCheckBox;

    @FXML
    private TextArea outputArea;

    @FXML
    private TableView<ParkingSlot> slotsTable;

    @FXML
    private TableColumn<ParkingSlot, String> slotColumn;

    @FXML
    private TableColumn<ParkingSlot, String> statusColumn;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private ParkingManagementSystem pms;

    public MainController() {
        pms = new ParkingManagementSystem();
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:space_saver.db");
        } catch (Exception e) {
            showAlert("Database Error", "Unable to connect to the database.");
            return null;
        }
    }

    @FXML
    public void initialize() {
        vehicleTypeComboBox.getItems().addAll("FourWheeler", "TwoWheeler");
        slotColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getSlotType()));
        statusColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().isOccupied() ? "Occupied" : "Available"));
    }

    @FXML
    private void handleRegisterVehicle(ActionEvent event) {
        try {
            String vehicleNum = vehicleNumberField.getText();
            String ownerName = ownerNameField.getText();
            long contactNum = Long.parseLong(contactNumberField.getText());

            if (vehicleNum.isEmpty() || ownerName.isEmpty()) {
                outputArea.setText("Please fill all the fields correctly.");
                return;
            }

            Vehicle vehicle;
            String vehicleType = vehicleTypeComboBox.getValue();
            boolean hasCarrier = hasCarrierCheckBox.isSelected();

            if ("FourWheeler".equalsIgnoreCase(vehicleType)) {
                vehicle = new FourWheeler(vehicleNum, ownerName, contactNum, vehicleType);
            } else if ("TwoWheeler".equalsIgnoreCase(vehicleType)) {
                vehicle = new TwoWheeler(vehicleNum, ownerName, contactNum, hasCarrier);
            } else {
                outputArea.setText("Please select a valid vehicle type.");
                return;
            }

            pms.registerVehicle(vehicle);
            outputArea.appendText("Vehicle Registered Successfully.\n");

        } catch (NumberFormatException e) {
            outputArea.setText("Please enter a valid contact number.");
        }
    }

    @FXML
    private void handleGenerateReport(ActionEvent event) {
        outputArea.appendText("Generating Report...\n");
        pms.generateReport();
    }

    @FXML
    private void handleDisplaySlots(ActionEvent event) {
        outputArea.appendText("Fetching Available Parking Slots...\n");
        ObservableList<ParkingSlot> availableSlots = FXCollections.observableArrayList(pms.getAvailableSlots());
        slotsTable.getItems().clear();
        slotsTable.setItems(availableSlots);
        outputArea.appendText("Displayed available parking slots.\n");
    }

    @FXML
    private void handleNotifyAlerts(ActionEvent event) {
        outputArea.appendText("Sending Time Alerts...\n");
        outputArea.appendText("Time alerts have been sent.\n");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Username and password cannot be empty.");
            return;
        }

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT role FROM Users WHERE username = ? AND password = ?")) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                showAlert("Login Successful", "Welcome, " + role + " user!");
            } else {
                showAlert("Login Failed", "Invalid username or password.");
            }

        } catch (Exception e) {
            showAlert("Login Error", "An error occurred during login.");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
