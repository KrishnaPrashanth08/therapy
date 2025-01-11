const express = require('express');
const yamljs = require('yamljs');
const swaggerUi = require('swagger-ui-express');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');


const swaggerDocument = yamljs.load('./swagger.yaml');


const app = express();
app.use(bodyParser.json());


app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocument));


let users = []; 


app.post('/signup', (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ message: 'Email and password are required.' });
  }

  
  const existingUser = users.find(user => user.email === email);
  if (existingUser) {
    return res.status(400).json({ message: 'User already exists.' });
  }


  const hashedPassword = bcrypt.hashSync(password, 10);

  
  const userId = Date.now().toString(); 
  users.push({ email, password: hashedPassword, userId });

  
  res.status(200).json({ message: 'Signup successful', userId });
});


app.post('/login', (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ message: 'Email and password are required.' });
  }

  
  const user = users.find(u => u.email === email);
  if (!user) {
    return res.status(401).json({ message: 'Unauthorized' });
  }

 
  const isPasswordValid = bcrypt.compareSync(password, user.password);
  if (!isPasswordValid) {
    return res.status(401).json({ message: 'Unauthorized' });
  }

 
  const token = jwt.sign({ userId: user.userId }, 'your-secret-key', { expiresIn: '1h' });


  res.status(200).json({ message: 'Login successful', token });
});

const port = 3000;
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
