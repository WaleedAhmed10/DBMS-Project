const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
const bcrypt = require('bcrypt');

const app = express();
app.use(cors());
app.use(express.json());

const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: 'Waleed*2002',
  database: 'space_saver'
});

app.post('/api/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    const [rows] = await pool.query('SELECT * FROM Users WHERE email = ?', [email]);
    if (rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    const user = rows[0];
    const match = await bcrypt.compare(password, user.password);
    if (match) {
      res.json({ user });
    } else {
      res.status(401).json({ error: 'Invalid credentials' });
    }
  } catch (error) {
    console.error('Error during login:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/register', async (req, res) => {
  const { name, email, password, phoneNumber, role } = req.body;
  try {
    if (!name || !email || !password || !phoneNumber) {
      return res.status(400).json({ error: 'All fields are required' });
    }
    if (phoneNumber.length !== 10 || isNaN(phoneNumber)) {
      return res.status(400).json({ error: 'Phone number must be 10 digits' });
    }
    const [existingUser] = await pool.query('SELECT * FROM Users WHERE email = ?', [email]);
    if (existingUser.length > 0) {
      return res.status(400).json({ error: 'Email already registered' });
    }
    const hashedPassword = await bcrypt.hash(password, 10);
    const [result] = await pool.query(
      'INSERT INTO Users (name, email, password, phoneNumber, role) VALUES (?, ?, ?, ?, ?)',
      [name, email, hashedPassword, phoneNumber, role || 'Customer']
    );
    res.json({ userID: result.insertId, message: 'Registration successful' });
  } catch (error) {
    console.error('Error during registration:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.get('/api/slots/available', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM AvailableSlots');
    res.json(rows);
  } catch (error) {
    console.error('Error fetching available slots:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.get('/api/notifications/:userID', async (req, res) => {
  const { userID } = req.params;
  try {
    const [rows] = await pool.query('SELECT * FROM Notifications WHERE userID = ?', [userID]);
    res.json(rows);
  } catch (error) {
    console.error('Error fetching notifications:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.get('/api/payments/:userID', async (req, res) => {
  const { userID } = req.params;
  try {
    const [rows] = await pool.query('SELECT * FROM Payments WHERE userID = ?', [userID]);
    res.json(rows);
  } catch (error) {
    console.error('Error fetching payments:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/vehicles/register', async (req, res) => {
  const { licensePlate, vehicleType, model, color, userID } = req.body;
  try {
    if (!licensePlate || !vehicleType || !model || !color || !userID) {
      return res.status(400).json({ error: 'All fields are required' });
    }
    const [userCheck] = await pool.query('SELECT * FROM Users WHERE userID = ?', [userID]);
    if (userCheck.length === 0) {
      return res.status(400).json({ error: 'User not found' });
    }

    const [vehicleCheck] = await pool.query('SELECT * FROM Vehicles WHERE licensePlate = ?', [licensePlate]);
    if (vehicleCheck.length > 0) {
      return res.status(400).json({ error: 'Vehicle already registered' });
    }

    await pool.query('CALL RegisterVehicle(?, ?, ?, ?, ?)', [userID, licensePlate, vehicleType, model, color]);

    const [vehicle] = await pool.query('SELECT * FROM Vehicles WHERE licensePlate = ?', [licensePlate]);
    const vehicleID = vehicle[0].vehicleID;
    const [log] = await pool.query('SELECT * FROM EntryExitLogs WHERE vehicleID = ? AND exitTime IS NULL', [vehicleID]);
    const slotNumber = (await pool.query('SELECT slotNumber FROM ParkingSlots WHERE slotID = ?', [log[0].slotID]))[0][0].slotNumber;

    await pool.query(
      'INSERT INTO Notifications (userID, message, status) VALUES (?, ?, ?)',
      [userID, `Vehicle ${licensePlate} parked in slot ${slotNumber}`, 'Unread']
    );

    res.json({ message: 'Vehicle registered and parked successfully' });
  } catch (error) {
    console.error('Error registering vehicle:', error);
    if (error.sqlState === '45000') {
      return res.status(400).json({ error: 'No available parking slot.' });
    }
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/vehicles/exit', async (req, res) => {
  const { licensePlate } = req.body;
  try {
    const [vehicle] = await pool.query('SELECT * FROM Vehicles WHERE licensePlate = ?', [licensePlate]);
    if (vehicle.length === 0) {
      return res.status(400).json({ error: 'Vehicle not found' });
    }

    const vehicleID = vehicle[0].vehicleID;
    const [log] = await pool.query(
      'SELECT * FROM EntryExitLogs WHERE vehicleID = ? AND exitTime IS NULL',
      [vehicleID]
    );
    if (log.length === 0) {
      return res.status(400).json({ error: 'No active parking session found' });
    }

    const slotID = log[0].slotID;

    await pool.query('CALL ExitVehicle(?, ?)', [vehicleID, slotID]);

    const [payments] = await pool.query('SELECT * FROM Payments WHERE userID = ? ORDER BY timestamp DESC LIMIT 1', [vehicle[0].userID]);
    console.log('Payment after exit:', payments);

    await pool.query(
      'INSERT INTO Notifications (userID, message, status) VALUES (?, ?, ?)',
      [vehicle[0].userID, `Vehicle ${licensePlate} has exited`, 'Unread']
    );

    res.json({ message: 'Vehicle exited successfully' });
  } catch (error) {
    console.error('Error exiting vehicle:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.get('/api/revenue', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT TotalRevenue() AS totalRevenue');
    const totalRevenue = rows[0]?.totalRevenue || 0;
    res.json({ totalRevenue });
  } catch (error) {
    console.error('Error fetching total revenue:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/reports/generate', async (req, res) => {
  try {
    const { userID, reportType, details } = req.body;
    const [result] = await pool.query(
      'INSERT INTO Reports (generatedBy, reportType, details) VALUES (?, ?, ?)',
      [userID, reportType, details]
    );
    res.json({ message: 'Report generated successfully', reportID: result.insertId });
  } catch (error) {
    console.error('Error generating report:', error);
    res.status(500).json({ error: 'Failed to generate report. Please try again.' });
  }
});

app.get('/api/reports', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM Reports ORDER BY dateGenerated DESC');
    res.json(rows);
  } catch (error) {
    console.error('Error fetching reports:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.get('/api/vehicles/parked', async (req, res) => {
  try {
    const [rows] = await pool.query(`
      SELECT v.vehicleID, v.licensePlate, v.vehicleType, ps.slotNumber, eel.entryTime
      FROM Vehicles v
      JOIN EntryExitLogs eel ON v.vehicleID = eel.vehicleID
      JOIN ParkingSlots ps ON eel.slotID = ps.slotID
      WHERE eel.exitTime IS NULL
    `);
    res.json(rows);
  } catch (error) {
    console.error('Error fetching parked vehicles:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});
app.listen(3001, () => console.log('Server running on port 3001'));