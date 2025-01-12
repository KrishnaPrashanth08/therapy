const express = require('express');
const yamljs = require('yamljs');
const swaggerUi = require('swagger-ui-express');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { addItem, getItem } = require('./dynamodb-operations'); // Import functions from dynamodb-operations
const cors = require('cors');

const swaggerDocument = yamljs.load('./swagger.yaml');

const app = express();
app.use(cors());
app.use(bodyParser.json());

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocument));

// Signup route
app.post('/signup', async (req, res) => {
  const { email, password, role = 'client' } = req.body; // Default role is 'client'
  console.log('Received signup request:', req.body);

  if (!email || !password) {
    return res.status(400).json({ message: 'Email and password are required.' });
  }

  if (role !== 'client' && role !== 'therapist') {
    return res.status(400).json({ message: 'Invalid role. Role must be either client or therapist.' });
  }

  try {
    // Check if the user already exists in DynamoDB
    const existingUser = await getItem({ email });
    if (existingUser) {
      return res.status(400).json({ message: 'User already exists.' });
    }

    // Hash the password and create a new user
    const hashedPassword = bcrypt.hashSync(password, 10);
    const userId = Date.now().toString(); // Generate a unique user ID
    const newUser = { email, password: hashedPassword, userId, role };

    // Call the addItem function to store the new user in DynamoDB
    await addItem(newUser);

    res.status(200).json({ message: 'Signup successful', userId, role });
  } catch (error) {
    console.error('Error during signup:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// Login route
app.post('/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ message: 'Email and password are required.' });
  }

  try {
    // Retrieve user from DynamoDB
    const user = await getItem({ email });

    if (!user) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Check password
    const isPasswordValid = bcrypt.compareSync(password, user.password);
    if (!isPasswordValid) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Generate JWT
    const token = jwt.sign(
      { userId: user.userId, role: user.role }, // Include role in the token payload
      'your-secret-key',
      { expiresIn: '1h' }
    );

    res.status(200).json({ message: 'Login successful', token, role: user.role });
  } catch (error) {
    console.error('Error during login:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

const port = 5000;
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
