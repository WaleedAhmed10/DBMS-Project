package com.example.oopfrontproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private static final String DB_NAME = "space_saver.db";

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseHandler.init();
            Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
            Scene scene = new Scene(root, 600, 400);
            primaryStage.setTitle("Parking Management System");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    static class DatabaseHandler {
        private static final String URL = "jdbc:sqlite:" + DB_NAME;

        public static void init() {
            try (Connection connection = DriverManager.getConnection(URL)) {
                Statement statement = connection.createStatement();

                statement.execute("CREATE TABLE IF NOT EXISTS Vehicles (" +
                        "vehicleNum TEXT PRIMARY KEY, ownerName TEXT, contactNum INTEGER, vehicleType TEXT, hasCarrier BOOLEAN)");
                statement.execute("CREATE TABLE IF NOT EXISTS ParkingSlots (" +
                        "slotID INTEGER PRIMARY KEY AUTOINCREMENT, slotType TEXT, isOccupied BOOLEAN)");
                statement.execute("CREATE TABLE IF NOT EXISTS Tickets (" +
                        "ticketID INTEGER PRIMARY KEY AUTOINCREMENT, entryTime TEXT, exitTime TEXT, vehicleNum TEXT, duration INTEGER, " +
                        "FOREIGN KEY(vehicleNum) REFERENCES Vehicles(vehicleNum))");

                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM ParkingSlots");
                rs.next();
                if (rs.getInt(1) == 0) {
                    statement.execute("INSERT INTO ParkingSlots (slotType, isOccupied) VALUES " +
                            "('Car', 0), ('Bike', 0), ('Car',0), ('Bike',0), ('Car',0)");
                }

                System.out.println("Database '" + DB_NAME + "' initialized.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL);
        }
    }

    abstract static class Vehicle {
        private String vehicleNum;
        private String ownerName;
        private long contactNum;

        public Vehicle(String vehicleNum, String ownerName, long contactNum) {
            this.vehicleNum = vehicleNum;
            this.ownerName = ownerName;
            this.contactNum = contactNum;
        }

        public String getVehicleNum() { return vehicleNum; }
        public String getOwnerName() { return ownerName; }
        public long getContactNum() { return contactNum; }

        public void displayDetails() {
            System.out.println("Vehicle Number: " + vehicleNum);
            System.out.println("Owner Name: " + ownerName);
            System.out.println("Contact Number: " + contactNum);
        }
    }

    static class FourWheeler extends Vehicle {
        private String vehicleType;

        public FourWheeler(String vehicleNum, String ownerName, long contactNum, String vehicleType) {
            super(vehicleNum, ownerName, contactNum);
            this.vehicleType = vehicleType;
        }

        @Override
        public void displayDetails() {
            super.displayDetails();
            System.out.println("Vehicle Type: 4 Wheeler (" + vehicleType + ")");
        }
    }

    static class TwoWheeler extends Vehicle {
        private boolean hasCarrier;

        public TwoWheeler(String vehicleNum, String ownerName, long contactNum, boolean hasCarrier) {
            super(vehicleNum, ownerName, contactNum);
            this.hasCarrier = hasCarrier;
        }

        public boolean hasCarrier() { return hasCarrier; }

        @Override
        public void displayDetails() {
            super.displayDetails();
            System.out.println("Vehicle Type: 2 Wheeler");
            System.out.println("Has Carrier: " + (hasCarrier ? "Yes" : "No"));
        }
    }

    static class ParkingSlot {
        String slotType;
        boolean isOccupied;

        public ParkingSlot(String slotType) {
            this.slotType = slotType;
            this.isOccupied = false;
        }

        public String getSlotType() { return slotType; }
        public boolean isOccupied() { return isOccupied; }
        public void occupy() { isOccupied = true; }
        public void vacate() { isOccupied = false; }
    }

    static class Ticket {
        private int ticketID;
        private String entryTime;
        private String exitTime;
        private Vehicle vehicle;
        private int duration;

        public Ticket(int ticketID, String entryTime, Vehicle vehicle) {
            this.ticketID = ticketID;
            this.entryTime = entryTime;
            this.vehicle = vehicle;
        }

        public void setExitTime(String exitTime) { this.exitTime = exitTime; }
        public void setDuration(int duration) { this.duration = duration; }

        public void displayTicketDetails() {
            System.out.println("Ticket ID: " + ticketID);
            System.out.println("Entry Time: " + entryTime);
            System.out.println("Exit Time: " + exitTime);
            vehicle.displayDetails();
        }
    }

    static class ParkingManagementSystem {
        public List<ParkingSlot> getAvailableSlots() {
            List<ParkingSlot> slots = new ArrayList<>();
            try (Connection connection = DatabaseHandler.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT slotType, isOccupied FROM ParkingSlots")) {
                while (rs.next()) {
                    ParkingSlot slot = new ParkingSlot(rs.getString("slotType"));
                    slot.isOccupied = rs.getBoolean("isOccupied");
                    slots.add(slot);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return slots;
        }

        public void registerVehicle(Vehicle vehicle) {
            try (Connection connection = DatabaseHandler.getConnection()) {
                String findSlotQuery = "SELECT slotID FROM ParkingSlots WHERE isOccupied = 0 LIMIT 1";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(findSlotQuery)) {

                    if (!rs.next()) {
                        System.out.println("No available slots.");
                        return;
                    }

                    int slotID = rs.getInt("slotID");

                    String insertVehicle = "INSERT INTO Vehicles (vehicleNum, ownerName, contactNum, vehicleType, hasCarrier) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertVehicle)) {
                        ps.setString(1, vehicle.getVehicleNum());
                        ps.setString(2, vehicle.getOwnerName());
                        ps.setLong(3, vehicle.getContactNum());
                        ps.setString(4, (vehicle instanceof FourWheeler) ? "FourWheeler" : "TwoWheeler");
                        ps.setBoolean(5, (vehicle instanceof TwoWheeler) && ((TwoWheeler) vehicle).hasCarrier());
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = connection.prepareStatement("UPDATE ParkingSlots SET isOccupied = 1 WHERE slotID = ?")) {
                        ps.setInt(1, slotID);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Tickets (entryTime, vehicleNum) VALUES (?, ?)")) {
                        ps.setString(1, getCurrentTime());
                        ps.setString(2, vehicle.getVehicleNum());
                        ps.executeUpdate();
                    }

                    System.out.println("Vehicle registered. Assigned Slot ID: " + slotID);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void generateReport() {
            try (Connection connection = DatabaseHandler.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS vehicleCount, SUM(duration * 10) AS revenue FROM Tickets")) {
                if (rs.next()) {
                    System.out.println("Total Vehicles: " + rs.getInt("vehicleCount"));
                    System.out.println("Total Revenue: $" + rs.getDouble("revenue"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void sendTimeAlerts() {
            try (Connection connection = DatabaseHandler.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT ticketID, entryTime, vehicleNum FROM Tickets WHERE exitTime IS NULL")) {

                while (rs.next()) {
                    String entryTimeStr = rs.getString("entryTime");
                    String vehicleNum = rs.getString("vehicleNum");

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime entryTime = LocalDateTime.parse(entryTimeStr, formatter);
                    LocalDateTime now = LocalDateTime.now();

                    long hours = java.time.Duration.between(entryTime, now).toHours();
                    if (hours > 2) {
                        System.out.println("Alert: Vehicle " + vehicleNum + " parked for " + hours + " hours.");
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Parking Alert");
                        alert.setHeaderText("Parking Time Exceeded");
                        alert.setContentText("Vehicle " + vehicleNum + " has been parked for " + hours + " hours.");
                        alert.showAndWait();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private String getCurrentTime() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    public static class Controller {
        @FXML
        private ListView<String> slotListView;

        @FXML
        private void viewParkingSlots() {
            ParkingManagementSystem system = new ParkingManagementSystem();
            List<ParkingSlot> slots = system.getAvailableSlots();
            List<String> slotDetails = new ArrayList<>();

            for (ParkingSlot slot : slots) {
                slotDetails.add("Slot Type: " + slot.getSlotType() + " | Occupied: " + (slot.isOccupied() ? "Yes" : "No"));
            }

            slotListView.getItems().setAll(slotDetails);
        }

        @FXML
        private void sendTimeAlerts() {
            ParkingManagementSystem system = new ParkingManagementSystem();
            system.sendTimeAlerts();
        }
    }
}
