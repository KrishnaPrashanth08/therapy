const express = require('express');
const path = require('path');
const yamljs = require('yamljs');
const swaggerUi = require('swagger-ui-express');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { addItem, getItem, addTherapyData, getTherapyData,scanClients, scanTherapists,scanSessions,updateTherapyData,deleteTherapyData ,scanJournalAccessRequests ,scanTherapistsWithAccess,scanAppointmentRequests} = require('./dynamodb-operations'); // Import functions for new table and existing ones
const cors = require('cors');

const swaggerDocument = yamljs.load('./swagger.yaml');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// Swagger docs route
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
    // Check if the user already exists in DynamoDB (authentication table)
    const existingUser = await getItem({ email });
    if (existingUser) {
      return res.status(400).json({ message: 'User already exists.' });
    }

    // Hash the password and create a new user
    const hashedPassword = bcrypt.hashSync(password, 10);
    const userId = Date.now().toString(); // Generate a unique user ID
    const newUser = { email, password: hashedPassword, userId, role };

    // Call the addItem function to store the new user in DynamoDB (authentication table)
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

// Clients endpoint ---- done
app.post('/clients', async (req, res) => {
  try {
    const client = { ...req.body, type: 'client', id: `client-${Date.now()}` };
    await addTherapyData(client);
    res.status(201).json({ message: 'Client added successfully', client });
  } catch (error) {
    res.status(500).json({ message: 'Error adding client', error });
  }
});

// Get all clients --- done
app.get('/clients', async (req, res) => {
  try {
   
    const clients = await scanClients();
    console.log(clients); 
    res.status(200).json(clients); 
  } catch (error) {
    res.status(500).json({ message: 'Error fetching clients', error });
  }
});

// Endpoint to get a client by ID
app.get('/clients/:id', async (req, res) => {
  const clientId = req.params.id; 

  try {
   
    const key = { id: clientId }; 
    const clientData = await getTherapyData(key);

    if (!clientData || clientData.type !== 'client') {
      return res.status(404).json({ message: 'Client not found' });
    }

    res.status(200).json(clientData); 
  } catch (error) {
    console.error('Error fetching client data:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

//endpoint for client to update by id
app.put('/clients/:id', async (req, res) => {
  const { id } = req.params; 
  const updateData = req.body; 

  if (!id || Object.keys(updateData).length === 0) {
    return res.status(400).json({ error: 'Invalid request. ID and update data are required.' });
  }

  try {
    
    const updateExpressionParts = [];
    const expressionValues = {};
    const expressionAttributeNames = {}; 

    for (const key in updateData) {
      
      const attributePlaceholder = `#${key}`;
      const valuePlaceholder = `:${key}`;

      updateExpressionParts.push(`${attributePlaceholder} = ${valuePlaceholder}`);
      expressionValues[valuePlaceholder] = updateData[key];
      expressionAttributeNames[attributePlaceholder] = key; 
    }

    const updateExpression = `SET ${updateExpressionParts.join(', ')}`;

    
    const updatedClient = await updateTherapyData(
      { id }, 
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );

    res.json({ message: 'Client updated successfully', updatedClient });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

//endpoint for deleting a client by id
app.delete('/clients/:id', async (req, res) => {
  const { id } = req.params;

  try {
    
    const key = { id }; 
    await deleteTherapyData(key); 

    res.status(200).send({ message: `Client with ID ${id} deleted successfully.` });
  } catch (error) {
    console.error('Error deleting client profile:', error);
    res.status(500).send({ error: 'Failed to delete client profile.' });
  }
});

//enpoint for clients to check therapist requests 
app.get('/clients/:id/journal-access-requests', async (req, res) => {
  try {
    const requests = await scanJournalAccessRequests(req.params.id);
    res.status(200).json(requests);
  } catch (error) {
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

//endpoint foe clients  to accepting or rejecting request
app.post('/clients/:id/approve-access', async (req, res) => {
  const clientId = req.params.id;
  const { requestId, action } = req.body;

  if (!clientId || !requestId || !action) {
    return res.status(400).json({ message: 'Client ID, request ID, and action are required.' });
  }

  if (action !== 'approve' && action !== 'reject') {
    return res.status(400).json({ message: 'Invalid action. Must be "approve" or "reject".' });
  }

  try {
    
    const request = await getTherapyData({ id: requestId });

    if (!request || request.type !== 'journal_access_request' || request.clientId !== clientId) {
      return res.status(404).json({ message: 'Journal access request not found.' });
    }

   
    const updateExpression = 'SET #status = :status';
    const expressionValues = {
      ':status': action === 'approve' ? 'approved' : 'rejected'
    };
    const expressionAttributeNames = {
      '#status': 'status'
    };

    const updatedRequest = await updateTherapyData(
      { id: requestId },
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );

    res.status(200).json({ 
      message: `Journal access request ${action}d successfully`, 
      updatedRequest 
    });
  } catch (error) {
    console.error('Error processing journal access request:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// endpoint for a client to see who has acces to journals
app.get('/clients/:id/therapists-access', async (req, res) => {
  try {
    const requests = await scanTherapistsWithAccess(req.params.id);
    res.status(200).json(requests);
  } catch (error) {
    res.status(500).json({ message: 'Internal Server Error' });
  }
});
//endpoint for clients to request for appointments based on sessionsId
app.post('/clients/:id/appointments', async (req, res) => {
  const clientId = req.params.id;
  const { sessionId } = req.body;

  if (!clientId || !sessionId) {
    return res.status(400).json({ message: 'Client ID and Session ID are required' });
  }

  try {
    
    const session = await getTherapyData({ id: sessionId });
    
    if (!session || session.type !== 'session') {
      return res.status(404).json({ message: 'Session not found' });
    }

    const request = {
      id: `appreq-${Date.now()}`,
      type: 'appointment_request',
      clientId,
      sessionId,
      therapistId: session.therapistId, 
      status: 'pending',
      requestedAt: new Date().toISOString()
    };

    await addTherapyData(request);
    res.status(201).json({ message: 'Appointment request submitted', request });
  } catch (error) {
    console.error('Error submitting appointment request:', error);
    res.status(500).json({ message: 'Request failed', error: error.message });
  }
});

//ENDPOINT FOR CLIENT TO EDIT JOURNAL ACCESS PERMISSION
app.put('/clients/:id/journal-access-permissions', async (req, res) => {
  const clientId = req.params.id;
  const { therapists } = req.body;

  if (!clientId || !Array.isArray(therapists)) {
    return res.status(400).json({ message: 'Client ID and therapists array are required.' });
  }

  try {
    const client = await getTherapyData({ id: clientId });
    if (!client || client.type !== 'client') {
      return res.status(404).json({ message: 'Client not found.' });
    }

    const updateExpression = 'SET journalAccessPermissions = :permissions';
    const expressionValues = {
      ':permissions': therapists
    };
    const updatedClient = await updateTherapyData(
      { id: clientId },
      updateExpression,
      expressionValues
    );

    res.status(200).json({
      message: 'Journal access permissions updated successfully',
      updatedPermissions: updatedClient.journalAccessPermissions
    });
  } catch (error) {
    console.error('Error updating journal access permissions:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// Endpoint to get a therapist by ID
app.get('/therapists/:id', async (req, res) => {
  const therapistId = req.params.id; 

  try {
    const key = { id: therapistId }; 
    const therapistData = await getTherapyData(key);

    
    if (!therapistData || therapistData.type !== "therapist") {
      return res.status(404).json({ message: 'Therapist not found' }); 
    }

    res.status(200).json(therapistData); 
  } catch (error) {
    console.error('Error fetching therapist data:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// Therapists endpoint --- done
app.post('/therapists', async (req, res) => {
  try {
    const { email, name, location, expertise } = req.body;
    
    if (!email || !name) {
      return res.status(400).json({ message: 'Email and name are required.' });
    }

    const therapist = {
      id: `therapist-${Date.now()}`,
      type: 'therapist',
      email,
      name,
      location,
      expertise,
      mappedClients: []
    };

    await addTherapyData(therapist);
    res.status(201).json({ message: 'Therapist added successfully', therapist });
  } catch (error) {
    console.error('Error adding therapist:', error);
    res.status(500).json({ message: 'Error adding therapist', error: error.message });
  }
});

// get all therapists
app.get('/therapists', async (req, res) => {
  try {
    const { location, expertise } = req.query;
    
    let filterExpression = '#type = :therapistType';
    let expressionAttributeNames = { '#type': 'type' };
    let expressionAttributeValues = { ':therapistType': 'therapist' };

    if (location) {
      filterExpression += ' AND contains(#location, :location)';
      expressionAttributeNames['#location'] = 'location';
      expressionAttributeValues[':location'] = location;
    }

    if (expertise) {
      filterExpression += ' AND contains(#expertise, :expertise)';
      expressionAttributeNames['#expertise'] = 'expertise';
      expressionAttributeValues[':expertise'] = expertise;
    }

    const params = {
      TableName: TABLE_NAME_2,
      FilterExpression: filterExpression,
      ExpressionAttributeNames: expressionAttributeNames,
      ExpressionAttributeValues: expressionAttributeValues
    };

    const data = await dynamodb.scan(params).promise();
    res.status(200).json(data.Items);
  } catch (error) {
    console.error('Error fetching therapists:', error);
    res.status(500).json({ message: 'Error fetching therapists', error });
  }
});


// Endpoint for therapist to update by ID
app.put('/therapists/:id', async (req, res) => {
  const { id } = req.params;
  const updateData = req.body;
  if (!id || Object.keys(updateData).length === 0) {
    return res.status(400).json({ error: 'Invalid request. ID and update data are required.' });
  }
  try {
    const updateExpressionParts = [];
    const expressionValues = {};
    const expressionAttributeNames = {};
    for (const key in updateData) {
      const attributePlaceholder = `#${key}`;
      const valuePlaceholder = `:${key}`;
      updateExpressionParts.push(`${attributePlaceholder} = ${valuePlaceholder}`);
      expressionValues[valuePlaceholder] = updateData[key];
      expressionAttributeNames[attributePlaceholder] = key;
    }
    const updateExpression = `SET ${updateExpressionParts.join(', ')}`;
    const updatedTherapist = await updateTherapyData(
      { id },
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );
    res.json({ message: 'Therapist updated successfully', updatedTherapist });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

//endpoint for deleting a therapist by id
app.delete('/therapists/:id', async (req, res) => {
  const { id } = req.params;

  try {
   
    const key = { id }; 
    await deleteTherapyData(key); 

    res.status(200).send({ message: `Therapist with ID ${id} deleted successfully.` });
  } catch (error) {
    console.error('Error deleting therapist profile:', error);
    res.status(500).send({ error: 'Failed to delete therapist profile.' });
  }
});

//endpoint for therapists to request journal access from clients
app.post('/therapists/:id/request-access', async (req, res) => {
  const therapistId = req.params.id; 
  const { clientId } = req.body; 

  if (!therapistId || !clientId) {
    return res.status(400).json({ message: 'Therapist ID and Client ID are required.' });
  }

  try {
    
    const requestId = `request-${Date.now()}`;
    const journalAccessRequest = {
      id: requestId,
      therapistId,
      clientId,
      status: 'pending', 
      type: 'journal_access_request', 
    };

    await addTherapyData(journalAccessRequest);

    res.status(200).json({ message: 'Journal access request sent successfully', requestId });
  } catch (error) {
    console.error('Error requesting journal access:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// Endpoint to create available session slots for a therapist
app.post('/therapists/:id/sessions', async (req, res) => {
  const therapistId = req.params.id; 
  const { startTime, endTime } = req.body; 

  
  if (!therapistId || !startTime || !endTime) {
    return res.status(400).json({ message: 'Therapist ID, start time, and end time are required.' });
  }

  try {
    
    const session = {
      id: `session-${Date.now()}`, 
      therapistId,
      startTime,
      endTime,
      status: 'available', 
      type: 'session' 
    };

    
    await addTherapyData(session);

    res.status(201).json({ message: 'Session created successfully', session });
  } catch (error) {
    console.error('Error creating session:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});


// Get pending appointments for therapist

app.get('/therapists/:id/appointments', async (req, res) => {
  try {
    const requests = await scanAppointmentRequests(req.params.id);
    res.status(200).json(requests);
  } catch (error) {
    console.error('Appointment Error:', error);
    res.status(500).json({ 
      message: 'Error fetching requests',
      error: error.message
    });
  }
});
// therapists to accept or reject an appointment
app.patch('/therapists/:id/appointments/:appointmentId', async (req, res) => {
  const { id: therapistId, appointmentId } = req.params;
  const { decision } = req.body;


  if (!['approve', 'reject'].includes(decision)) {
    return res.status(400).json({ message: 'Invalid decision. Must be "approve" or "reject".' });
  }

  try {
    const request = await getTherapyData({ id: appointmentId });

    if (!request || request.type !== 'appointment_request' || request.therapistId !== therapistId) {
      return res.status(404).json({ message: 'Appointment request not found or unauthorized.' });
    }

   
    const updatedRequest = await updateTherapyData(
      { id: appointmentId }, 
      'SET #status = :status', 
      { ':status': decision === 'approve' ? 'approved' : 'rejected' }, 
      { '#status': 'status' } 
    );

    
    if (decision === 'approve') {
      await updateTherapyData(
        { id: request.sessionId }, 
        'SET #status = :status', 
        { ':status': 'booked' }, 
        { '#status': 'status' } 
      );
    }

    res.status(200).json({
      message: `Appointment request ${decision}ed successfully`,
      updatedRequest,
    });
  } catch (error) {
    console.error('Error processing appointment request:', error);
    res.status(500).json({ message: 'Internal Server Error', error: error.message });
  }
});


// POST /messages - Send message
app.post('/messages', async (req, res) => {
  try {
    const { senderId, recipientId, content } = req.body;
    
    if (!senderId || !recipientId || !content) {
      return res.status(400).json({ message: 'Sender ID, recipient ID, and content are required' });
    }

    const message = {
      id: `msg-${Date.now()}`,
      type: 'message',
      senderId,
      recipientId,
      content,
      timestamp: new Date().toISOString(),
      status: 'sent'
    };

    await addTherapyData(message);
    res.status(201).json(message);
  } catch (error) {
    res.status(500).json({ message: 'Error sending message', error: error.message });
  }
});

// GET /messages - Get conversation history
app.get('/messages', async (req, res) => {
  try {
    const { clientId, therapistId } = req.query;
    
    if (!clientId || !therapistId) {
      return res.status(400).json({ message: 'clientId and therapistId query parameters are required' });
    }

    const params = {
      TableName: 'therapy-data',
      FilterExpression: '(senderId = :clientId AND recipientId = :therapistId) OR ' +
                       '(senderId = :therapistId AND recipientId = :clientId)',
      ExpressionAttributeValues: {
        ':clientId': clientId,
        ':therapistId': therapistId
      }
    };

    const data = await dynamodb.scan(params).promise();
    const sortedMessages = data.Items.sort((a, b) => 
      new Date(a.timestamp) - new Date(b.timestamp)
    );
    
    res.status(200).json(sortedMessages);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching messages', error: error.message });
  }
});

// GET /messages/{id} - Get single message
app.get('/messages/:id', async (req, res) => {
  try {
    const message = await getTherapyData({ id: req.params.id });
    
    if (!message || message.type !== 'message') {
      return res.status(404).json({ message: 'Message not found' });
    }
    
    res.status(200).json(message);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching message', error: error.message });
  }
});

// PUT /messages/{id} - Update message
app.put('/messages/:id', async (req, res) => {
  try {
    const message = await getTherapyData({ id: req.params.id });
    
    if (!message || message.type !== 'message') {
      return res.status(404).json({ message: 'Message not found' });
    }

    
    const allowedUpdates = ['content', 'status'];
    const invalidUpdates = Object.keys(req.body).filter(
      key => !allowedUpdates.includes(key)
    );

    if (invalidUpdates.length > 0) {
      return res.status(400).json({
        message: `Invalid updates: ${invalidUpdates.join(', ')}`
      });
    }

    const updatedMessage = await updateTherapyData(
      { id: req.params.id },
      'SET content = :content, #status = :status',
      {
        ':content': req.body.content,
        ':status': req.body.status || 'edited'
      },
      { '#status': 'status' }
    );

    res.status(200).json(updatedMessage);
  } catch (error) {
    res.status(500).json({ message: 'Error updating message', error: error.message });
  }
});

// DELETE /messages/{id}
app.delete('/messages/:id', async (req, res) => {
  try {
    const message = await getTherapyData({ id: req.params.id });
    
    if (!message || message.type !== 'message') {
      return res.status(404).json({ message: 'Message not found' });
    }

    await deleteTherapyData({ id: req.params.id });
    res.status(204).send();
  } catch (error) {
    res.status(500).json({ message: 'Error deleting message', error: error.message });
  }
});

// POST /journals - Create journal entry with emotional tracking
app.post('/journals', async (req, res) => {
  try {
    const { time, feeling, intensity, clientId } = req.body;
    if (!time || !feeling || intensity === undefined || !clientId) {
      return res.status(400).json({ message: 'Missing required fields' });
    }

    if (intensity < 1 || intensity > 10) {
      return res.status(400).json({ message: 'Intensity must be between 1-10' });
    }

    const journalEntry = {
      id: `journal-${Date.now()}`,
      type: 'journal',
      time,
      feeling: feeling.toLowerCase(),
      intensity: parseInt(intensity),
      clientId,
      createdAt: new Date().toISOString(),
    };

    await addTherapyData(journalEntry);
    res.status(201).json(journalEntry);
  } catch (error) {
    res.status(500).json({ message: 'Error creating entry', error: error.message });
  }
});


// GET /journals - Filter by type and user
app.get('/journals', async (req, res) => {
  try {
    const { clientId } = req.query;
    if (!clientId) {
      return res.status(400).json({ message: 'clientId query parameter is required' });
    }

    const params = {
      TableName: 'therapy-data',
      FilterExpression: '#type = :type AND clientId = :clientId',
      ExpressionAttributeNames: { '#type': 'type' },
      ExpressionAttributeValues: { ':type': 'journal', ':clientId': clientId }
    };

    const data = await dynamodb.scan(params).promise();
    const formatted = data.Items.map(item => ({
      id: item.id,
      time: item.time,
      feeling: item.feeling,
      intensity: item.intensity,
      clientId: item.clientId,
      createdAt: item.createdAt
    }));

    res.status(200).json(formatted);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching journals', error: error.message });
  }
});


// GET /journals/{id} - Get specific entry
app.get('/journals/:id', async (req, res) => {
  try {
    const journal = await getTherapyData({ id: req.params.id });
    if (!journal || journal.type !== 'journal') {
      return res.status(404).json({ message: 'Journal not found' });
    }

    const response = {
      id: journal.id,
      time: journal.time,
      feeling: journal.feeling,
      intensity: journal.intensity,
      clientId: journal.clientId,
      createdAt: journal.createdAt
    };

    res.status(200).json(response);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching journal', error: error.message });
  }
});


// PUT /journals/{id} - Update entry
app.put('/journals/:id', async (req, res) => {
  try {
    const journal = await getTherapyData({ id: req.params.id });
    if (!journal || journal.type !== 'journal') {
      return res.status(404).json({ message: 'Journal not found' });
    }

    const allowedUpdates = ['time', 'feeling', 'intensity'];
    const invalidUpdates = Object.keys(req.body).filter(
      key => !allowedUpdates.includes(key)
    );
    if (invalidUpdates.length > 0) {
      return res.status(400).json({
        message: `Invalid updates: ${invalidUpdates.join(', ')}`
      });
    }

    if (req.body.intensity && (req.body.intensity < 1 || req.body.intensity > 10)) {
      return res.status(400).json({ message: 'Intensity must be between 1-10' });
    }

    const updateResult = await updateTherapyData(
      { id: req.params.id },
      'SET ' + Object.keys(req.body).map(k => `#${k} = :${k}`).join(', '),
      Object.fromEntries(Object.entries(req.body).map(([k,v]) => [`:${k}`, k === 'intensity' ? parseInt(v) : v])),
      Object.fromEntries(Object.keys(req.body).map(k => [`#${k}`, k]))
    );

    res.status(200).json(updateResult);
  } catch (error) {
    res.status(500).json({ message: 'Error updating journal', error: error.message });
  }
});


// DELETE /journals/{id}
app.delete('/journals/:id', async (req, res) => {
  try {
    const journal = await getTherapyData({ id: req.params.id });
    
    if (!journal || journal.type !== 'journal') {
      return res.status(404).json({ message: 'Journal not found' });
    }

    await deleteTherapyData({ id: req.params.id });
    res.status(204).send();
  } catch (error) {
    res.status(500).json({ message: 'Error deleting journal', error: error.message });
  }
});

//SESSIONS ENDPOINTS
//POST A SESSION 
app.post('/sessions', async (req, res) => {
  const { date, startTime, endTime, therapistId, clientId, privateNotes, sharedNotes } = req.body;

  if (!date || !startTime || !endTime || !therapistId || !clientId) {
    return res.status(400).json({ message: 'Date, startTime, endTime, therapistId, and clientId are required.' });
  }

  try {
    const session = {
      id: `session-${Date.now()}`,
      type: 'session',
      date,
      startTime,
      endTime,
      therapistId,
      clientId,
      privateNotes: privateNotes || '',
      sharedNotes: sharedNotes || '',
      status: 'scheduled'
    };

    await addTherapyData(session);
    res.status(201).json({ message: 'Session created successfully', session });
  } catch (error) {
    console.error('Error creating session:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});


//get all sessions
app.get('/sessions', async (req, res) => {
  try {
    const sessions = await scanSessions();
    const formattedSessions = sessions.map(session => ({
      ...session,
      startTime: session.startTime,
      endTime: session.endTime
    }));
    res.status(200).json(formattedSessions);
  } catch (error) {
    console.error('Error fetching sessions:', error);
    res.status(500).json({ message: 'Error fetching sessions', error: error.message });
  }
});


// Get single session by ID
app.get('/sessions/:id', async (req, res) => {
  try {
    const session = await getTherapyData({ id: req.params.id });
    
    if (!session || session.type !== 'session') {
      return res.status(404).json({ message: 'Session not found' });
    }
    
    const formattedSession = {
      ...session,
      startTime: session.startTime,
      endTime: session.endTime
    };
    
    res.status(200).json(formattedSession);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching session', error });
  }
});


// Update session endpoint
app.put('/sessions/:id', async (req, res) => {
  try {
    const session = await getTherapyData({ id: req.params.id });
    
    if (!session || session.type !== 'session') {
      return res.status(404).json({ message: 'Session not found' });
    }

    const { date, startTime, endTime, therapistId, clientId, privateNotes, sharedNotes } = req.body;
    
    const updateExpressionParts = [];
    const expressionValues = {};
    const expressionAttributeNames = {};

    const updateFields = { date, startTime, endTime, therapistId, clientId, privateNotes, sharedNotes };

    for (const [key, value] of Object.entries(updateFields)) {
      if (value !== undefined) {
        const attrPlaceholder = `#${key}`;
        const valuePlaceholder = `:${key}`;
        
        updateExpressionParts.push(`${attrPlaceholder} = ${valuePlaceholder}`);
        expressionAttributeNames[attrPlaceholder] = key;
        expressionValues[valuePlaceholder] = value;
      }
    }

    if (updateExpressionParts.length === 0) {
      return res.status(400).json({ message: 'No valid fields to update' });
    }

    const updatedSession = await updateTherapyData(
      { id: req.params.id },
      `SET ${updateExpressionParts.join(', ')}`,
      expressionValues,
      expressionAttributeNames
    );

    res.status(200).json(updatedSession);
  } catch (error) {
    res.status(500).json({ message: 'Error updating session', error: error.message });
  }
});


// Delete session endpoint
app.delete('/sessions/:id', async (req, res) => {
  try {
    const session = await getTherapyData({ id: req.params.id });
    
    if (!session || session.type !== 'session') {
      return res.status(404).json({ message: 'Session not found' });
    }

    await deleteTherapyData({ id: req.params.id });
    res.status(204).send();
  } catch (error) {
    res.status(500).json({ message: 'Error deleting session' });
  }
});

// POST /therapists/{therapistId}/mapping-requests
app.post('/therapists/:therapistId/mapping-requests', async (req, res) => {
  const { therapistId } = req.params;
  const { clientId } = req.body;

  try {
    const therapist = await getTherapyData({ id: therapistId });
    const client = await getTherapyData({ id: clientId });

    if (!therapist || therapist.type !== 'therapist' || !client || client.type !== 'client') {
      return res.status(404).json({ message: 'Therapist or client not found.' });
    }

    const updateExpression = 'SET mappingRequests = list_append(if_not_exists(mappingRequests, :emptyList), :newRequest)';
    const expressionValues = {
      ':emptyList': [],
      ':newRequest': [clientId]
    };

    await updateTherapyData({ id: therapistId }, updateExpression, expressionValues);

    res.status(200).json({ message: 'Mapping request sent successfully.' });
  } catch (error) {
    console.error('Error sending mapping request:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// PATCH /clients/{clientId}/mapping-requests/{therapistId}
app.patch('/clients/:clientId/mapping-requests/:therapistId', async (req, res) => {
  const { clientId, therapistId } = req.params;
  const { status } = req.body; // 'accepted' or 'rejected'

  try {
    const client = await getTherapyData({ id: clientId });
    const therapist = await getTherapyData({ id: therapistId });

    if (!client || client.type !== 'client' || !therapist || therapist.type !== 'therapist') {
      return res.status(404).json({ message: 'Client or therapist not found.' });
    }

    if (status === 'accepted') {
      // Update client's mappedTherapists
      await updateTherapyData(
        { id: clientId },
        'SET mappedTherapists = list_append(if_not_exists(mappedTherapists, :emptyList), :newTherapist)',
        { ':emptyList': [], ':newTherapist': [therapistId] }
      );

      // Update therapist's mappedClients
      await updateTherapyData(
        { id: therapistId },
        'SET mappedClients = list_append(if_not_exists(mappedClients, :emptyList), :newClient)',
        { ':emptyList': [], ':newClient': [clientId] }
      );
    }

    // Remove the mapping request
    await updateTherapyData(
      { id: therapistId },
      'SET mappingRequests = list_remove(mappingRequests, :clientId)',
      { ':clientId': clientId }
    );

    res.status(200).json({ message: `Mapping request ${status}.` });
  } catch (error) {
    console.error('Error updating mapping request:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// DELETE /clients/{clientId}/mapped-therapists/{therapistId}
app.delete('/clients/:clientId/mapped-therapists/:therapistId', async (req, res) => {
  const { clientId, therapistId } = req.params;

  try {
    const client = await getTherapyData({ id: clientId });
    const therapist = await getTherapyData({ id: therapistId });

    if (!client || client.type !== 'client' || !therapist || therapist.type !== 'therapist') {
      return res.status(404).json({ message: 'Client or therapist not found.' });
    }

    // Remove therapist from client's mappedTherapists
    await updateTherapyData(
      { id: clientId },
      'SET mappedTherapists = list_remove(mappedTherapists, :therapistId)',
      { ':therapistId': therapistId }
    );

    // Remove client from therapist's mappedClients
    await updateTherapyData(
      { id: therapistId },
      'SET mappedClients = list_remove(mappedClients, :clientId)',
      { ':clientId': clientId }
    );

    res.status(200).json({ message: 'Mapping removed successfully.' });
  } catch (error) {
    console.error('Error removing mapping:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});


app.use(express.static(path.join(__dirname, '../therapy-app/dist')));

// Fallback for any unmatched routes to serve `index.html`
// app.get('*', (req, res) => {
//   res.sendFile(path.join(__dirname, '../therapy-app/dist/index.html'));
// });

const port = process.env.PORT || 5000;
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
