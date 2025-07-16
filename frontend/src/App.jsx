import { useState, useEffect } from 'react';

function App() {
  const [user, setUser] = useState(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [slots, setSlots] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [payments, setPayments] = useState([]);
  const [totalRevenue, setTotalRevenue] = useState(null);
  const [revenueError, setRevenueError] = useState(null);
  const [reports, setReports] = useState([]);
  const [parkedVehicles, setParkedVehicles] = useState([]);
  const [registerMode, setRegisterMode] = useState(false);
  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [vehicleData, setVehicleData] = useState({
    licensePlate: '',
    vehicleType: 'Car',
    model: '',
    color: ''
  });
  const [exitLicensePlate, setExitLicensePlate] = useState('');

  const fetchAvailableSlots = async () => {
    try {
      const response = await fetch('http://localhost:3001/api/slots/available');
      if (!response.ok) throw new Error('Failed to fetch slots');
      const data = await response.json();
      setSlots(data);
    } catch (error) {
      console.error('Error fetching slots:', error);
      setSlots([]);
    }
  };

  const fetchNotifications = async (userID) => {
    try {
      const response = await fetch(`http://localhost:3001/api/notifications/${userID}`);
      if (!response.ok) throw new Error('Failed to fetch notifications');
      const data = await response.json();
      setNotifications(data);
    } catch (error) {
      console.error('Error fetching notifications:', error);
      setNotifications([]);
    }
  };

  const fetchPayments = async (userID) => {
    try {
      const response = await fetch(`http://localhost:3001/api/payments/${userID}`);
      if (!response.ok) throw new Error('Failed to fetch payments');
      const data = await response.json();
      setPayments(data);
    } catch (error) {
      console.error('Error fetching payments:', error);
      setPayments([]);
    }
  };

  const fetchTotalRevenue = async () => {
    try {
      setRevenueError(null);
      const response = await fetch('http://localhost:3001/api/revenue');
      if (!response.ok) throw new Error('Failed to fetch total revenue');
      const data = await response.json();
      setTotalRevenue(data.totalRevenue || 0);
    } catch (error) {
      console.error('Error fetching total revenue:', error);
      setRevenueError('Unable to fetch total revenue. Please try again later.');
      setTotalRevenue(0);
    }
  };

  const fetchReports = async () => {
    try {
      const response = await fetch('http://localhost:3001/api/reports');
      if (!response.ok) throw new Error('Failed to fetch reports');
      const data = await response.json();
      setReports(data);
    } catch (error) {
      console.error('Error fetching reports:', error);
      setReports([]);
    }
  };

  const fetchParkedVehicles = async () => {
    try {
      const response = await fetch('http://localhost:3001/api/vehicles/parked');
      if (!response.ok) throw new Error('Failed to fetch parked vehicles');
      const data = await response.json();
      setParkedVehicles(data);
    } catch (error) {
      console.error('Error fetching parked vehicles:', error);
      setParkedVehicles([]);
    }
  };

  const handleRegisterVehicle = async () => {
    if (!vehicleData.licensePlate || !vehicleData.vehicleType || !vehicleData.model || !vehicleData.color) {
      alert('Please fill in all vehicle details.');
      return;
    }
    try {
      const response = await fetch('http://localhost:3001/api/vehicles/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          licensePlate: vehicleData.licensePlate,
          vehicleType: vehicleData.vehicleType,
          model: vehicleData.model,
          color: vehicleData.color,
          userID: user.userID
        })
      });
      const data = await response.json();
      if (data.message) {
        alert(data.message);
        setVehicleData({ licensePlate: '', vehicleType: 'Car', model: '', color: '' });
        await fetchAvailableSlots();
        await fetchNotifications(user.userID);
      } else {
        alert(data.error || 'Failed to register vehicle.');
      }
    } catch (error) {
      console.error('Error registering vehicle:', error);
      alert('Failed to register vehicle. Please try again. Check console for details.');
    }
  };

  const handleExitVehicle = async () => {
    if (!exitLicensePlate) {
      alert('Please enter the license plate.');
      return;
    }
    try {
      const response = await fetch('http://localhost:3001/api/vehicles/exit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ licensePlate: exitLicensePlate })
      });
      const data = await response.json();
      if (data.message) {
        alert(data.message);
        setExitLicensePlate('');
        await fetchAvailableSlots();
        await fetchNotifications(user.userID);
        await fetchPayments(user.userID);
        if (user.role === 'Admin') {
          await fetchTotalRevenue();
          await fetchParkedVehicles();
        }
      } else {
        alert(data.error || 'Failed to exit vehicle.');
      }
    } catch (error) {
      console.error('Error exiting vehicle:', error);
      alert('Failed to exit vehicle. Please try again.');
    }
  };

  const generateRevenueReport = async () => {
    if (!user || user.role !== 'Admin') {
      alert('Only admins can generate reports');
      return;
    }
    try {
      const revenue = totalRevenue !== null && typeof totalRevenue === 'number' ? totalRevenue.toFixed(2) : '0.00';
      console.log('Generating report with:', { userID: user.userID, revenue });
      const response = await fetch('http://localhost:3001/api/reports/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userID: user.userID,
          reportType: 'Revenue',
          details: `Total Revenue as of ${new Date().toISOString()}: $${revenue}`
        })
      });
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Failed to generate report');
      }
      const data = await response.json();
      alert(data.message || 'Report generated successfully');
      await fetchReports();
    } catch (error) {
      console.error('Error generating report:', error);
      alert(`Failed to generate report: ${error.message}`);
    }
  };

  const handleLogin = async () => {
    if (!email || !password) {
      alert('Please enter email and password.');
      return;
    }
    try {
      const response = await fetch('http://localhost:3001/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await response.json();
      if (data.user) {
        setUser(data.user);
      } else {
        alert(data.error || 'Invalid credentials');
      }
    } catch (error) {
      console.error('Error during login:', error);
      alert('Failed to log in. Please try again.');
    }
  };

  const handleRegister = async () => {
    if (!name || !email || !password || !phoneNumber) {
      alert('Please fill in all fields.');
      return;
    }
    try {
      const response = await fetch('http://localhost:3001/api/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, password, phoneNumber, role: 'Customer' })
      });
      const data = await response.json();
      if (data.userID) {
        alert('Registration successful! Please log in.');
        setRegisterMode(false);
        setEmail('');
        setPassword('');
        setName('');
        setPhoneNumber('');
      } else {
        alert(data.error || 'Registration failed.');
      }
    } catch (error) {
      console.error('Error during registration:', error);
      alert('Failed to register. Please try again.');
    }
  };

  const handleLogout = () => {
    setUser(null);
    setEmail('');
    setPassword('');
    setSlots([]);
    setNotifications([]);
    setPayments([]);
    setTotalRevenue(null);
    setRevenueError(null);
    setReports([]);
    setParkedVehicles([]);
    setVehicleData({ licensePlate: '', vehicleType: 'Car', model: '', color: '' });
    setExitLicensePlate('');
  };

  useEffect(() => {
    if (user) {
      const fetchData = async () => {
        try {
          await Promise.all([
            fetchAvailableSlots(),
            fetchNotifications(user.userID),
            fetchPayments(user.userID)
          ]);
          if (user.role === 'Admin') {
            await Promise.all([fetchTotalRevenue(), fetchReports(), fetchParkedVehicles()]);
          }
        } catch (error) {
          console.error('Error during polling:', error);
        }
      };
      fetchData();

      const interval = setInterval(fetchData, 10000);
      return () => clearInterval(interval);
    }
  }, [user]);

  return (
    <>
      <style>
        {`
          body { margin: 0; font-family: 'Arial', sans-serif; }
          .container { min-height: 100vh; padding: 2rem; background: linear-gradient(135deg, #6B46C1, #F6AD55); display: flex; flex-direction: column; align-items: center; }
          .nav { width: 100%; background: rgba(107, 70, 193, 0.9); padding: 1rem; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2); position: sticky; top: 0; z-index: 10; }
          .nav-content { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; color: white; }
          .nav-title { font-size: 1.875rem; font-weight: bold; }
          .nav-user { font-size: 0.875rem; margin-right: 1rem; color: #F6AD55; }
          .nav-button { padding: 0.5rem 1rem; background: #38B2AC; color: white; border: none; border-radius: 0.5rem; cursor: pointer; transition: background 0.3s ease; }
          .nav-button:hover { background: #2D8F8C; }
          .main-content { max-width: 1200px; width: 100%; margin-top: 2rem; display: grid; gap: 2rem; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); }
          .section { background: white; padding: 1.5rem; border-radius: 1rem; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1); transition: transform 0.3s ease; }
          .section:hover { transform: translateY(-5px); }
          .section-title { font-size: 1.5rem; font-weight: bold; color: #333333; margin-bottom: 1rem; background: linear-gradient(90deg, #6B46C1, #38B2AC); -webkit-background-clip: text; background-clip: text; color: transparent; }
          .input-group { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem; }
          .input { padding: 0.75rem; border: 2px solid #ddd; border-radius: 0.5rem; font-size: 1rem; transition: border-color 0.3s ease; }
          .input:focus { border-color: #6B46C1; outline: none; }
          .select { padding: 0.75rem; border: 2px solid #ddd; border-radius: 0.5rem; font-size: 1rem; transition: border-color 0.3s ease; }
          .select:focus { border-color: #6B46C1; outline: none; }
          .button { padding: 0.75rem; background: #38B2AC; color: white; border: none; border-radius: 0.5rem; font-size: 1.1rem; font-weight: bold; cursor: pointer; transition: background 0.3s ease, transform 0.3s ease; width: 100%; }
          .button:hover { background: #2D8F8C; transform: scale(1.02); }
          .list { list-style: none; padding: 0; }
          .list-item { padding: 0.5rem; background: #f9f9f9; border-radius: 0.5rem; margin-bottom: 0.5rem; color: #333333; }
          .login-container { min-height: 100vh; display: flex; justify-content: center; align-items: center; background: linear-gradient(135deg, #6B46C1, #F6AD55); }
          .login-card { background: white; padding: 2rem; border-radius: 1rem; box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2); width: 100%; max-width: 24rem; text-align: center; }
          .login-title { font-size: 2rem; font-weight: bold; color: #333333; margin-bottom: 1.5rem; background: linear-gradient(90deg, #6B46C1, #38B2AC); -webkit-background-clip: text; background-clip: text; color: transparent; }
          .link { color: #38B2AC; text-decoration: none; font-weight: bold; transition: color 0.3s ease; }
          .link:hover { color: #2D8F8C; }
          .error { color: red; font-size: 0.875rem; margin-top: 0.5rem; }
          @media (max-width: 640px) { .main-content { grid-template-columns: 1fr; } .nav-content { flex-direction: column; gap: 0.5rem; } .nav-button { width: 100%; } }
        `}
      </style>

      {user ? (
        <div>
          <nav className="nav">
            <div className="nav-content">
              <h1 className="nav-title">Space Saver</h1>
              <div>
                <span className="nav-user">Welcome, {user.name} ({user.role})</span>
                <button onClick={handleLogout} className="nav-button">Logout</button>
              </div>
            </div>
          </nav>
          <div className="container">
            <div className="main-content">
              {user.role === 'Customer' && (
                <>
                  <div className="section">
                    <h2 className="section-title">Available Slots</h2>
                    <ul className="list">
                      {slots.length > 0 ? (
                        slots.map(slot => (
                          <li key={slot.slotID} className="list-item">
                            Slot {slot.slotNumber} ({slot.slotType}) - {slot.location} - ${slot.hourlyRate}/hr
                          </li>
                        ))
                      ) : (
                        <li className="list-item">No available slots</li>
                      )}
                    </ul>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Register Vehicle</h2>
                    <div className="input-group">
                      <input
                        type="text"
                        placeholder="License Plate"
                        value={vehicleData.licensePlate}
                        onChange={(e) => setVehicleData({ ...vehicleData, licensePlate: e.target.value })}
                        className="input"
                      />
                      <select
                        value={vehicleData.vehicleType}
                        onChange={(e) => setVehicleData({ ...vehicleData, vehicleType: e.target.value })}
                        className="select"
                      >
                        <option value="Car">Car</option>
                        <option value="Bike">Bike</option>
                        <option value="Other">Other</option>
                      </select>
                      <input
                        type="text"
                        placeholder="Model"
                        value={vehicleData.model}
                        onChange={(e) => setVehicleData({ ...vehicleData, model: e.target.value })}
                        className="input"
                      />
                      <input
                        type="text"
                        placeholder="Color"
                        value={vehicleData.color}
                        onChange={(e) => setVehicleData({ ...vehicleData, color: e.target.value })}
                        className="input"
                      />
                      <button onClick={handleRegisterVehicle} className="button mt-4">Register Vehicle</button>
                    </div>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Exit Vehicle</h2>
                    <div className="input-group">
                      <input
                        type="text"
                        placeholder="License Plate"
                        value={exitLicensePlate}
                        onChange={(e) => setExitLicensePlate(e.target.value)}
                        className="input"
                      />
                      <button onClick={handleExitVehicle} className="button mt-4">Exit Vehicle</button>
                    </div>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Notifications</h2>
                    <ul className="list">
                      {notifications.length > 0 ? (
                        notifications.map(note => (
                          <li key={note.notificationID} className="list-item">
                            {note.message} - {new Date(note.timestamp).toLocaleString()} ({note.status})
                          </li>
                        ))
                      ) : (
                        <li className="list-item">No notifications</li>
                      )}
                    </ul>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Payment History</h2>
                    <ul className="list">
                      {payments.length > 0 ? (
                        payments.map(payment => (
                          <li key={payment.paymentID} className="list-item">
                            Amount: ${payment.amount} - {new Date(payment.timestamp).toLocaleString()} ({payment.transactionStatus})
                          </li>
                        ))
                      ) : (
                        <li className="list-item">No payments yet</li>
                      )}
                    </ul>
                  </div>
                </>
              )}

              {user.role === 'Admin' && (
                <>
                  <div className="section">
                    <h2 className="section-title">Total Revenue</h2>
                    {totalRevenue === null ? (
                      <p>Loading revenue...</p>
                    ) : revenueError ? (
                      <p className="error">{revenueError}</p>
                    ) : (
                      <p className="text-lg font-semibold text-gray-800">
                        Total Revenue: <span className="text-teal-600">${typeof totalRevenue === 'number' ? totalRevenue.toFixed(2) : 0}</span>
                      </p>
                    )}
                    <button onClick={generateRevenueReport} className="button mt-4">Generate Revenue Report</button>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Reports</h2>
                    <ul className="list">
                      {reports.length > 0 ? (
                        reports.map(report => (
                          <li key={report.reportID} className="list-item">
                            {report.reportType} - {new Date(report.dateGenerated).toLocaleString()}: {report.details}
                          </li>
                        ))
                      ) : (
                        <li className="list-item">No reports generated yet</li>
                      )}
                    </ul>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Parked Vehicles</h2>
                    <ul className="list">
                      {parkedVehicles.length > 0 ? (
                        parkedVehicles.map(vehicle => (
                          <li key={vehicle.vehicleID} className="list-item">
                            License: {vehicle.licensePlate} - Type: {vehicle.vehicleType} - Slot: {vehicle.slotNumber} - Entry: {new Date(vehicle.entryTime).toLocaleString()}
                          </li>
                        ))
                      ) : (
                        <li className="list-item">No vehicles parked</li>
                      )}
                    </ul>
                  </div>

                  <div className="section">
                    <h2 className="section-title">Payment History</h2>
                    <ul className="list">
                      {payments.length > 0 ? (
                        payments.map(payment => (
                          <li key={payment.paymentID} className="list-item">
                            Amount: ${payment.amount} - {new Date(payment.timestamp).toLocaleString()} ({payment.transactionStatus})
                          </li>
                        ))
                      ) : (
                        <li className="list-item">No payments yet</li>
                      )}
                    </ul>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      ) : (
        <div className="login-container">
          <div className="login-card">
            <h1 className="login-title">{registerMode ? 'Sign Up' : 'Login'}</h1>
            {registerMode && (
              <>
                <input
                  type="text"
                  placeholder="Full Name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="input"
                />
                <input
                  type="tel"
                  placeholder="Phone Number (10 digits)"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value.replace(/\D/g, '').slice(0, 10))}
                  className="input"
                />
              </>
            )}
            <input
              type="email"
              placeholder="Email Address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input"
            />
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input"
            />
            <button onClick={registerMode ? handleRegister : handleLogin} className="button mt-4">
              {registerMode ? 'Sign Up' : 'Login'}
            </button>
            <p className="mt-4 text-gray-700">
              {registerMode ? 'Already have an account?' : 'Don’t have an account?'}
              <a href="#" onClick={() => setRegisterMode(!registerMode)} className="link ml-1">
                {registerMode ? 'Login' : 'Sign Up'}
              </a>
            </p>
          </div>
        </div>
      )}
    </>
  );
}
export default App;