CREATE DATABASE IF NOT EXISTS space_saver;
USE space_saver;

-- VEHICLES table with vehicleNum as primary key
CREATE TABLE IF NOT EXISTS Vehicles 
(
    vehicleNum VARCHAR(15) PRIMARY KEY,
    ownerName VARCHAR(100) NOT NULL,
    contactNum BIGINT NOT NULL CHECK (contactNum > 1000000000),
    vehicleType ENUM('FourWheeler', 'TwoWheeler') NOT NULL,
    hasCarrier BOOLEAN DEFAULT FALSE
);

-- PARKING SLOTS table
CREATE TABLE IF NOT EXISTS ParkingSlots 
(
    slotID INT AUTO_INCREMENT PRIMARY KEY,
    slotType ENUM('Car', 'Bike') NOT NULL,
    isOccupied BOOLEAN DEFAULT FALSE
);

-- TICKETS table referencing Vehicles(vehicleNum)
CREATE TABLE IF NOT EXISTS Tickets 
(
    ticketID INT AUTO_INCREMENT PRIMARY KEY,
    entryTime DATETIME NOT NULL,
    exitTime DATETIME,
    duration INT CHECK (duration IS NULL OR duration >= 0),
    vehicleNum VARCHAR(15),
    FOREIGN KEY (vehicleNum) REFERENCES Vehicles(vehicleNum) ON DELETE CASCADE
);

-- ALERTS table referencing Vehicles(vehicleNum)
CREATE TABLE IF NOT EXISTS Alerts 
(
    alertID INT AUTO_INCREMENT PRIMARY KEY,
    vehicleNum VARCHAR(15),
    message TEXT,
    alertTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicleNum) REFERENCES Vehicles(vehicleNum) ON DELETE CASCADE
);

-- ENTRYEXITLOGS table referencing Vehicles(vehicleNum) and ParkingSlots(slotID)
CREATE TABLE IF NOT EXISTS EntryExitLogs
(
    logID INT AUTO_INCREMENT PRIMARY KEY,
    vehicleNum VARCHAR(15),
    slotID INT,
    entryTime DATETIME NOT NULL,
    exitTime DATETIME,
    duration INT,
    FOREIGN KEY (vehicleNum) REFERENCES Vehicles(vehicleNum) ON DELETE CASCADE,
    FOREIGN KEY (slotID) REFERENCES ParkingSlots(slotID)
);

-- Insert some initial parking slots
INSERT INTO ParkingSlots (slotType, isOccupied)
VALUES 
('Car', FALSE),
('Bike', FALSE),
('Car', FALSE),
('Bike', FALSE),
('Car', FALSE);

-- Create a view to show available slots
DROP VIEW IF EXISTS AvailableSlots;
CREATE VIEW AvailableSlots AS
SELECT slotID, slotType
FROM ParkingSlots
WHERE isOccupied = FALSE;

-- Stored procedure to register a vehicle and assign a parking slot
DELIMITER //

DROP PROCEDURE IF EXISTS RegisterVehicle;
CREATE PROCEDURE RegisterVehicle
(
    IN p_vehicleNum VARCHAR(15),
    IN p_ownerName VARCHAR(100),
    IN p_contactNum BIGINT,
    IN p_vehicleType ENUM('FourWheeler', 'TwoWheeler'),
    IN p_hasCarrier BOOLEAN
)
BEGIN
    DECLARE slotId INT;
    DECLARE slotTypeNeeded ENUM('Car', 'Bike');

    IF p_vehicleType = 'FourWheeler' THEN
        SET slotTypeNeeded = 'Car';
    ELSE
        SET slotTypeNeeded = 'Bike';
    END IF;

    SELECT slotID INTO slotId
    FROM ParkingSlots
    WHERE isOccupied = FALSE AND slotType = slotTypeNeeded
    LIMIT 1;

    IF slotId IS NOT NULL THEN
        INSERT INTO Vehicles(vehicleNum, ownerName, contactNum, vehicleType, hasCarrier)
        VALUES (p_vehicleNum, p_ownerName, p_contactNum, p_vehicleType, p_hasCarrier);

        UPDATE ParkingSlots SET isOccupied = TRUE WHERE slotID = slotId;

        INSERT INTO Tickets(entryTime, vehicleNum)
        VALUES (NOW(), p_vehicleNum);
    ELSE
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No suitable parking slot available.';
    END IF;
END //

DELIMITER ;

-- Stored procedure to mark vehicle exit and free parking slot
DELIMITER //

DROP PROCEDURE IF EXISTS ExitVehicle;
CREATE PROCEDURE ExitVehicle
(
    IN p_vehicleNum VARCHAR(15)
)
BEGIN
    DECLARE v_entryTime DATETIME;
    DECLARE v_exitTime DATETIME;
    DECLARE v_duration INT;
    DECLARE v_slotID INT;

    SET v_exitTime = NOW();

    SELECT entryTime, slotID INTO v_entryTime, v_slotID
    FROM Tickets
    JOIN EntryExitLogs ON Tickets.vehicleNum = EntryExitLogs.vehicleNum
    WHERE Tickets.vehicleNum = p_vehicleNum AND exitTime IS NULL
    LIMIT 1;

    SET v_duration = TIMESTAMPDIFF(HOUR, v_entryTime, v_exitTime);

    UPDATE Tickets
    SET exitTime = v_exitTime, duration = v_duration
    WHERE vehicleNum = p_vehicleNum AND exitTime IS NULL;

    UPDATE EntryExitLogs
    SET exitTime = v_exitTime, duration = v_duration
    WHERE vehicleNum = p_vehicleNum AND exitTime IS NULL;

    UPDATE ParkingSlots
    SET isOccupied = FALSE
    WHERE slotID = v_slotID;
END //

DELIMITER ;

-- Function to calculate total revenue based on $10 per hour parking fee
DELIMITER //

DROP FUNCTION IF EXISTS TotalRevenue;
CREATE FUNCTION TotalRevenue()
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
    DECLARE total DECIMAL(10,2);
    SELECT IFNULL(SUM(duration * 10), 0) INTO total
    FROM Tickets
    WHERE duration IS NOT NULL;
    RETURN total;
END //

DELIMITER ;

-- Trigger to alert if vehicle is parked for more than 2 hours without exit
DELIMITER //

DROP TRIGGER IF EXISTS AlertLongParking;
CREATE TRIGGER AlertLongParking
AFTER UPDATE ON Tickets
FOR EACH ROW
BEGIN
    IF NEW.exitTime IS NULL AND TIMESTAMPDIFF(HOUR, NEW.entryTime, NOW()) > 2 THEN
        INSERT INTO Alerts(vehicleNum, message)
        VALUES (
            NEW.vehicleNum,
            CONCAT('Vehicle ', NEW.vehicleNum, ' parked more than 2 hours.')
        );
    END IF;
END //

DELIMITER ;

DROP INDEX idx_contactNum ON Vehicles;
CREATE INDEX idx_contactNum ON Vehicles(contactNum);

DROP INDEX idx_type_carrier ON Vehicles;
CREATE INDEX idx_type_carrier ON Vehicles(vehicleType, hasCarrier);

DROP INDEX idx_slotType ON ParkingSlots;
CREATE INDEX idx_slotType ON ParkingSlots(slotType);

DROP INDEX idx_exit_duration ON Tickets;
CREATE INDEX idx_exit_duration ON Tickets(exitTime, duration);

DROP INDEX idx_alert_vehicleNum ON Alerts;
CREATE INDEX idx_alert_vehicleNum ON Alerts(vehicleNum);

-- Test selects to verify data
SELECT * FROM Vehicles;
SELECT * FROM Alerts;
SELECT * FROM ParkingSlots;
SELECT * FROM Tickets;
SELECT * FROM EntryExitLogs;
