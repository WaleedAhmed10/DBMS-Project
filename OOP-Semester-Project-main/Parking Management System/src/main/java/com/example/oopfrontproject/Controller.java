package com.example.oopfrontproject;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class Controller {
    private Connection connect() throws SQLException
    {
        return DriverManager.getConnection("jdbc:sqlite:space_saver.db");
    }

    public boolean enterVehicle(String vehicleNum)
    {
        try (Connection conn = connect())
        {
            PreparedStatement checkVehicle = conn.prepareStatement("SELECT vehicleType FROM Vehicles WHERE vehicleNum = ?");
            checkVehicle.setString(1, vehicleNum);
            ResultSet rs = checkVehicle.executeQuery();
            if (!rs.next()) return false;
            String type = rs.getString("vehicleType");
            String slotType = type.equalsIgnoreCase("FourWheeler") ? "Car" : "Bike";
            PreparedStatement findSlot = conn.prepareStatement("SELECT slotID FROM ParkingSlots WHERE slotType = ? AND isOccupied = 0 LIMIT 1");
            findSlot.setString(1, slotType);
            ResultSet slotRs = findSlot.executeQuery();
            if (!slotRs.next()) return false;
            int slotID = slotRs.getInt("slotID");
            PreparedStatement ticketStmt = conn.prepareStatement("INSERT INTO Tickets(entryTime, vehicleNum) VALUES (?, ?)");
            ticketStmt.setString(1, LocalDateTime.now().toString());
            ticketStmt.setString(2, vehicleNum);
            ticketStmt.executeUpdate();
            PreparedStatement occupySlot = conn.prepareStatement("UPDATE ParkingSlots SET isOccupied = 1 WHERE slotID = ?");
            occupySlot.setInt(1, slotID);
            occupySlot.executeUpdate();
            return true;
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exitVehicle(String vehicleNum)
    {
        try (Connection conn = connect())
        {
            PreparedStatement ticketStmt = conn.prepareStatement("SELECT ticketID, entryTime FROM Tickets WHERE vehicleNum = ? AND exitTime IS NULL ORDER BY ticketID DESC LIMIT 1");
            ticketStmt.setString(1, vehicleNum);
            ResultSet rs = ticketStmt.executeQuery();
            if (!rs.next()) return false;
            int ticketID = rs.getInt("ticketID");
            LocalDateTime entryTime = LocalDateTime.parse(rs.getString("entryTime"));
            LocalDateTime exitTime = LocalDateTime.now();
            long duration = Duration.between(entryTime, exitTime).toMinutes();
            double fee = calculateFee(duration);
            PreparedStatement payStmt = conn.prepareStatement("INSERT INTO Payments(amount, method) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            payStmt.setDouble(1, fee);
            payStmt.setString(2, "cash"); // Default to cash; can be extended
            payStmt.executeUpdate();
            ResultSet payKeys = payStmt.getGeneratedKeys();
            int paymentID = payKeys.next() ? payKeys.getInt(1) : 0;
            PreparedStatement updateTicket = conn.prepareStatement("UPDATE Tickets SET exitTime = ?, duration = ?, paymentID = ? WHERE ticketID = ?");
            updateTicket.setString(1, exitTime.toString());
            updateTicket.setLong(2, duration);
            updateTicket.setInt(3, paymentID);
            updateTicket.setInt(4, ticketID);
            updateTicket.executeUpdate();
            PreparedStatement vehicleStmt = conn.prepareStatement("SELECT vehicleType FROM Vehicles WHERE vehicleNum = ?");
            vehicleStmt.setString(1, vehicleNum);
            ResultSet vehicleRs = vehicleStmt.executeQuery();
            if (vehicleRs.next()) {
                String slotType = vehicleRs.getString("vehicleType").equalsIgnoreCase("FourWheeler") ? "Car" : "Bike";
                PreparedStatement freeSlot = conn.prepareStatement("UPDATE ParkingSlots SET isOccupied = 0 WHERE slotType = ? AND isOccupied = 1 LIMIT 1");
                freeSlot.setString(1, slotType);
                freeSlot.executeUpdate();
            }

            return true;
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private double calculateFee(long durationMinutes)
    {
        if (durationMinutes <= 30) return 20.0;
        return 20.0 + ((durationMinutes - 30) / 30) * 10.0;
    }
}