const bcrypt = require('bcrypt');
const mysql = require('mysql2/promise');

const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: 'Waleed*2002',
  database: 'space_saver'
});

async function hashAndUpdatePasswords() {
  try {
    const users = [
      { email: 'Amir@gmail.com', password: '1234567' },
      { email: 'Waleed2002@gmail.com', password: '12345678' }
    ];

    for (const user of users) {
      const hashedPassword = await bcrypt.hash(user.password, 10);
      await pool.query('UPDATE Users SET password = ? WHERE email = ?', [hashedPassword, user.email]);
      console.log(`Updated password for ${user.email}`);
    }

    console.log('All passwords updated successfully.');
    process.exit(0);
  } catch (error) {
    console.error('Error updating passwords:', error);
    process.exit(1);
  }
}
hashAndUpdatePasswords();