DROP DATABASE IF EXISTS space_saver;
CREATE DATABASE space_saver;
USE space_saver;

CREATE TABLE Users (
    userID INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin', 'Staff', 'Customer') DEFAULT 'Customer',
    phoneNumber BIGINT CHECK (phoneNumber >= 1000000000 AND phoneNumber <= 9999999999), -- Exactly 10 digits
    registrationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Vehicles (
    vehicleID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT,
    licensePlate VARCHAR(15) UNIQUE NOT NULL,
    vehicleType ENUM('Car', 'Bike', 'Other') NOT NULL,
    model VARCHAR(50),
    color VARCHAR(30),
    FOREIGN KEY (userID) REFERENCES Users(userID) ON DELETE CASCADE
);

CREATE TABLE ParkingSlots (
    slotID INT AUTO_INCREMENT PRIMARY KEY,
    slotNumber VARCHAR(10) UNIQUE,
    status ENUM('Available', 'Occupied') DEFAULT 'Available',
    location VARCHAR(100),
    slotType ENUM('Compact', 'Large', 'Handicapped') NOT NULL,
    hourlyRate DECIMAL(6,2) NOT NULL
);

CREATE TABLE Bookings (
    bookingID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT,
    slotID INT,
    bookingTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration INT,
    status ENUM('Booked', 'Cancelled', 'Completed') DEFAULT 'Booked',
    FOREIGN KEY (userID) REFERENCES Users(userID),
    FOREIGN KEY (slotID) REFERENCES ParkingSlots(slotID)
);

CREATE TABLE EntryExitLogs (
    logID INT AUTO_INCREMENT PRIMARY KEY,
    vehicleID INT,
    slotID INT,
    entryTime DATETIME NOT NULL,
    exitTime DATETIME,
    duration DECIMAL(10,2),
    FOREIGN KEY (vehicleID) REFERENCES Vehicles(vehicleID),
    FOREIGN KEY (slotID) REFERENCES ParkingSlots(slotID)
);

CREATE TABLE Payments (
    paymentID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT,
    bookingID INT DEFAULT NULL,
    amount DECIMAL(8,2),
    paymentMethod ENUM('Cash', 'Card', 'Online'),
    transactionStatus ENUM('Successful', 'Pending', 'Failed') DEFAULT 'Pending',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userID) REFERENCES Users(userID),
    FOREIGN KEY (bookingID) REFERENCES Bookings(bookingID) ON DELETE SET NULL
);

CREATE TABLE Notifications (
    notificationID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT,
    message TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Read', 'Unread') DEFAULT 'Unread',
    FOREIGN KEY (userID) REFERENCES Users(userID)
);

CREATE TABLE Reports (
    reportID INT AUTO_INCREMENT PRIMARY KEY,
    generatedBy INT,
    reportType ENUM('Usage', 'Revenue', 'Other') NOT NULL,
    dateGenerated DATETIME DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    FOREIGN KEY (generatedBy) REFERENCES Users(userID)
);

CREATE INDEX idx_user_phone ON Users(phoneNumber);
CREATE INDEX idx_vehicle_type ON Vehicles(vehicleType);
CREATE INDEX idx_parking_slotType ON ParkingSlots(slotType);
CREATE INDEX idx_booking_status ON Bookings(status);
CREATE INDEX idx_payment_status ON Payments(transactionStatus);
CREATE INDEX idx_log_time ON EntryExitLogs(entryTime);
CREATE INDEX idx_notification_user ON Notifications(userID);

CREATE VIEW AvailableSlots AS
SELECT slotID, slotNumber, slotType, location, hourlyRate
FROM ParkingSlots
WHERE status = 'Available';

DELIMITER //

CREATE PROCEDURE RegisterVehicle (
    IN p_userID INT,
    IN p_licensePlate VARCHAR(15),
    IN p_vehicleType ENUM('Car', 'Bike', 'Other'),
    IN p_model VARCHAR(50),
    IN p_color VARCHAR(30)
)
BEGIN
    DECLARE v_slotID INT;

    SELECT slotID INTO v_slotID
    FROM ParkingSlots
    WHERE status = 'Available'
        AND (
            (p_vehicleType = 'Car' AND slotType IN ('Large', 'Compact'))
            OR (p_vehicleType = 'Bike' AND slotType = 'Compact')
            OR (p_vehicleType = 'Other' AND slotType IN ('Large', 'Compact'))
        )
    LIMIT 1;

    IF v_slotID IS NOT NULL THEN
        INSERT INTO Vehicles(userID, licensePlate, vehicleType, model, color)
        VALUES (p_userID, p_licensePlate, p_vehicleType, p_model, p_color);

        UPDATE ParkingSlots SET status = 'Occupied' WHERE slotID = v_slotID;

        INSERT INTO EntryExitLogs(vehicleID, slotID, entryTime)
        VALUES (
            (SELECT vehicleID FROM Vehicles WHERE licensePlate = p_licensePlate),
            v_slotID,
            NOW()
        );
    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No available parking slot.';
    END IF;
END //

CREATE PROCEDURE ExitVehicle(IN p_vehicleID INT, IN p_slotID INT)
BEGIN
    DECLARE v_exitTime DATETIME;
    DECLARE v_hourlyRate DECIMAL(10, 2);
    DECLARE v_parkingDuration DECIMAL(10, 2);
    DECLARE v_amount DECIMAL(10, 2);
    DECLARE v_userID INT;

    SELECT userID INTO v_userID FROM Vehicles WHERE vehicleID = p_vehicleID;
    SET v_exitTime = NOW();
    UPDATE EntryExitLogs
    SET exitTime = v_exitTime
    WHERE vehicleID = p_vehicleID AND exitTime IS NULL;
    SELECT TIMESTAMPDIFF(MINUTE, entryTime, v_exitTime) / 60.0 INTO v_parkingDuration
    FROM EntryExitLogs
    WHERE vehicleID = p_vehicleID AND exitTime = v_exitTime;
    SET v_parkingDuration = GREATEST(v_parkingDuration, 1.0); -- Minimum 1 hour
    SELECT hourlyRate INTO v_hourlyRate
    FROM ParkingSlots
    WHERE slotID = p_slotID;
    SET v_amount = ROUND(v_parkingDuration * v_hourlyRate, 2);
    INSERT INTO Payments (userID, amount, paymentMethod, transactionStatus, timestamp)
    VALUES (v_userID, v_amount, 'Online', 'Successful', v_exitTime);

    UPDATE ParkingSlots
    SET status = 'Available'
    WHERE slotID = p_slotID;

    UPDATE EntryExitLogs
    SET duration = v_parkingDuration
    WHERE vehicleID = p_vehicleID AND exitTime = v_exitTime;
END //

CREATE FUNCTION TotalRevenue()
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
    DECLARE total DECIMAL(10,2);
    SELECT IFNULL(SUM(amount), 0) INTO total
    FROM Payments
    WHERE transactionStatus = 'Successful';
    RETURN total;
END //

CREATE TRIGGER TriggerLongParking
AFTER UPDATE ON EntryExitLogs
FOR EACH ROW
BEGIN
    DECLARE parkedHours INT;
    SET parkedHours = TIMESTAMPDIFF(HOUR, OLD.entryTime, NEW.exitTime);

    IF NEW.exitTime IS NOT NULL AND parkedHours > 2 THEN
        INSERT INTO Notifications(userID, message, status)
        VALUES (
            (SELECT userID FROM Vehicles WHERE vehicleID = NEW.vehicleID),
            CONCAT('Vehicle parked for more than 2 hours: ', NEW.vehicleID),
            'Unread'
        );
    END IF;
END //

DELIMITER ;

INSERT INTO Users (name, email, password, role, phoneNumber) VALUES
('Amir Ali', 'Amir@gmail.com', '1234567', 'Customer', 1234567890),
('Waleed Ahmad', 'Waleed2002@gmail.com', '12345678', 'Admin', 9876543210);

INSERT INTO ParkingSlots (slotNumber, location, slotType, hourlyRate) VALUES
('A1', 'Zone 1', 'Compact', 5.00),
('B1', 'Zone 2', 'Large', 7.00);

SELECT * FROM Users;
SELECT * FROM Vehicles;
SELECT * FROM ParkingSlots;
SELECT * FROM EntryExitLogs;
SELECT * FROM Payments;
SELECT * FROM Notifications;
SELECT * FROM Reports;
SELECT TotalRevenue() AS totalRevenue;