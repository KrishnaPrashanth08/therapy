const express = require('express');
const path = require('path');
const yamljs = require('yamljs');
const swaggerUi = require('swagger-ui-express');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { addItem, getItem,scanJournalAccessRequests ,
        addClient,getClient,updateClient,deleteClient,scanClients,addTherapist,getTherapist,updateTherapist,
        deleteTherapist,scanTherapists,addSessionSlot,getSessionSlot,updateSessionSlot,deleteSessionSlot,scanSessionSlots,
        addAppointmentRequest,getAppointmentRequest,updateAppointmentRequest,deleteAppointmentRequest,scanAppointmentRequests,
        addSession,getSession,updateSession,deleteSession,scanSessions,scanAppointmentRequestsByTherapistId,addJournalEntry,getJournalEntry,
        updateJournalEntry,deleteJournalEntry,scanJournalEntries,addJournalAccessRequest,getJournalAccessRequest,updateJournalAccessRequest,deleteJournalAccessRequest,
        updateJournalAccessPermissions,getTherapistsWithJournalAccess,addMessage,getMessage,updateMessage,deleteMessage,scanMessages,
        addMappingRequest,updateMappingRequest,deleteMappedTherapist,addMappedTherapist,getMappingRequest,searchTherapists,searchClients, searchJournals,searchMessages,} = require('./dynamodb-operations'); // Import functions for new table and existing ones
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
    const client = { ...req.body, ClientId: `client-${Date.now()}` };
    await addClient(client);
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
app.get('/clients/:ClientId', async (req, res) => {
  const clientId = req.params.ClientId; 

  try {
    const client = await getClient({ ClientId: clientId });
    if (!client) {
      return res.status(404).json({ message: 'Client not found' });
    }
    res.status(200).json(client);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching client', error });
  }
});

// PUT /clients/:id - Update a client's profile
app.put('/clients/:ClientId', async (req, res) => {
  const clientId = req.params.ClientId;
  const updateData = req.body;
  if (!clientId || Object.keys(updateData).length === 0) {
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
    const updatedClient = await updateClient(
      { ClientId: clientId },
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );

    res.json({ message: 'Client updated successfully', updatedClient });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// DELETE /clients/:id - Delete a client's profile
app.delete('/clients/:ClientId', async (req, res) => {
  const clientId = req.params.ClientId;
  try {
    await deleteClient({ ClientId: clientId });
    res.status(204).send({ message: `Client with ID ${ClientId} deleted successfully.` });
  } catch (error) {
    res.status(500).send({ error: 'Failed to delete client profile.' });
  }
});

//search endpoint for client
app.get('/clients/:clientId/search', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const { keyword } = req.query;

    if (!keyword) {
      return res.status(400).json({ message: 'Keyword is required for search' });
    }

    // Search therapists' notes, journals, locations, and expertise
    const therapists = await searchTherapists(keyword);
    const journals = await searchJournals(keyword, 'therapist');
    const messages = await searchMessages(clientId, keyword);

    res.status(200).json({
      clientId,
      results: {
        therapists,
        journals,
        messages,
      },
    });
  } catch (error) {
    console.error('Error performing search:', error);
    res.status(500).json({ message: 'Error performing search', error: error.message });
  }
});


//journal  related entry points
// POST /journal/{therapistId}/request-access
app.post('/journal/:therapistId/request-access', async (req, res) => {
  try {
    const TherapistId = req.params.therapistId;
    const { ClientId } = req.body;

    const request = {
      JournalAccessRequestId: `request-${Date.now()}`,
      TherapistId,
      ClientId,
      status: 'pending',
      createdAt: new Date().toISOString()
    };

    await addJournalAccessRequest(request);
    res.status(201).json(request);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// GET /journal/{clientId}/access-requests
app.get('/journal/:clientId/access-requests', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const requests = await scanJournalAccessRequests(clientId);
    res.status(200).json(requests);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// GET /journal/{clientId}/entries
app.get('/journal/:clientId/entries', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const entries = await scanJournalEntries(clientId);
    res.status(200).json(entries);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// POST /journal/{clientId}/entries
app.post('/journal/:clientId/entries', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const entry = {
      JournalEntryId: `entry-${Date.now()}`,
      clientId,
      time: req.body.time,
      feeling: req.body.feeling,
      intensity: req.body.intensity,
      content: req.body.content,
      createdAt: new Date().toISOString()
    };

    await addJournalEntry(entry);
    res.status(201).json(entry);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// PUT /journal/{clientId}/journal-access-permissions---------
app.put('/journal/:clientId/journal-access-permissions', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const { therapists } = req.body;

    const updatedPermissions = await updateJournalAccessPermissions(clientId, therapists);
    res.status(200).json({ message: 'Permissions updated' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// POST /journal/{clientId}/approve
app.post('/journal/:clientId/approve', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const { requestId, status } = req.body;

    const updatedRequest = await updateJournalAccessRequest(
      { JournalAccessRequestId: requestId },
      'SET #status = :status',
      { ':status': status },
      { '#status': 'status' }
    );

    res.status(200).json(updatedRequest);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
// ---------------------done
app.get('/journal/:clientId/therapists', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const therapists = await getTherapistsWithJournalAccess(clientId);
    res.status(200).json(therapists);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});


// GET /journal/{clientId}/{journalId} --------------done
app.get('/journal/:clientId/:journalId', async (req, res) => {
  try {
    const journalId = req.params.journalId;
    const entry = await getJournalEntry({ JournalEntryId: journalId });
    
    if (!entry || entry.clientId !== req.params.clientId) {
      return res.status(404).json({ message: 'Journal entry not found' });
    }
    
    res.status(200).json(entry);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// PUT /journal/{clientId}/{journalId} ---------done
app.put('/journal/:clientId/:journalId', async (req, res) => {
  try {
    const journalId = req.params.journalId;
    const updateData = req.body;

    const updateExpression = Object.keys(updateData)
      .map(key => `#${key} = :${key}`)
      .join(', ');

    const updatedEntry = await updateJournalEntry(
      { JournalEntryId: journalId },
      `SET ${updateExpression}`,
      Object.fromEntries(Object.entries(updateData).map(([k,v]) => [`:${k}`, v])),
      Object.fromEntries(Object.keys(updateData).map(k => [`#${k}`, k]))
    );

    res.status(200).json(updatedEntry);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// DELETE /journal/{clientId}/{journalId} ------done
app.delete('/journal/:clientId/:journalId', async (req, res) => {
  try {
    await deleteJournalEntry({ JournalEntryId: req.params.journalId });
    res.status(204).send();
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

//SESSION_SLOT ENDPOINTS

app.post('/session-slots/:TherapistId', async (req, res) => {
  try {
    const therapistId = req.params.TherapistId;
    const slotData = req.body;

    if (!therapistId || !slotData) {
      return res.status(400).json({ error: 'Invalid request. Therapist ID and slot data are required.' });
    }

    const slot = {
      SlotId: `slot-${Date.now()}`,
      therapistId,
      date: slotData.date,
      startTime: slotData.startTime,
      endTime: slotData.endTime,
      status: 'available'
    };

    await addSessionSlot(slot);
    res.status(201).json({ message: 'Session slot created successfully', slot });
  } catch (error) {
    res.status(500).json({ message: 'Error creating session slot', error });
  }
});

// GET /session-slots/{therapistId} - List session slots for a therapist
app.get('/session-slots/:TherapistId', async (req, res) => {
  try {
    const therapistId = req.params.TherapistId;
    const sessionSlots = await scanSessionSlots(therapistId);
    res.status(200).json(sessionSlots);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching session slots', error });
  }
});

// POST /session-slots/{therapistId}/appointments - Request an appointment
app.post('/session-slots/:TherapistId/appointments', async (req, res) => {
  try {
    const therapistId = req.params.TherapistId;
    const request = req.body;

    if (!therapistId || !request) {
      return res.status(400).json({ error: 'Invalid request. Therapist ID and request data are required.' });
    }

    const appointmentRequest = {
      AppointmentRequestId: `appointment-${Date.now()}`,
      SlotId: request.SlotId,
      ClientId: request.ClientId,
      TherapistId: therapistId,
      status: 'pending'
    };

    await addAppointmentRequest(appointmentRequest);
    res.status(201).json({ message: 'Appointment requested successfully', appointmentRequest });
  } catch (error) {
    res.status(500).json({ message: 'Error requesting appointment', error });
  }
});

// GET /session/{therapistId}/appointments - List pending appointment requests
app.get('/session/:therapistId/appointments', async (req, res) => {
  try {
    const therapistId = req.params.therapistId;
    const appointmentRequests = await scanAppointmentRequestsByTherapistId(therapistId);
    res.status(200).json(appointmentRequests);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching appointment requests', error });
  }
});



app.put('/session/:therapistId/appointments/:appointmentRequestId', async (req, res) => {
  const therapistId = req.params.therapistId;
  const appointmentRequestId = req.params.appointmentRequestId;
  const updateData = req.body;

  if (!therapistId || !appointmentRequestId || Object.keys(updateData).length === 0) {
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
    const updatedAppointment = await updateAppointmentRequest(
      { AppointmentRequestId: appointmentRequestId },
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );

    // Convert session slot to actual session if approved
    if (updateData.status === 'approved') {
      const appointmentRequest = await getAppointmentRequest({ AppointmentRequestId: appointmentRequestId });
      const slotId = appointmentRequest.SlotId;
      const slot = await getSessionSlot({ SlotId: slotId });

      if (!slot) {
        return res.status(404).json({ message: 'Session slot not found' });
      }

      // Update session slot status to booked
      await updateSessionSlot(
        { SlotId: slotId },
        'SET #status = :status',
        { ':status': 'booked' },
        { '#status': 'status' }
      );

      // Create a new session
      const session = {
        SessionId: `session-${Date.now()}`,
        date: slot.date,
        startTime: slot.startTime,
        endTime: slot.endTime,
        therapistId: slot.therapistId,
        clientId: appointmentRequest.ClientId,
        privateNotes: '',
        sharedNotes: '',
        status: 'scheduled'
      };

      await addSession(session);
      res.json({ message: 'Appointment request updated and session created successfully', session });
    } else {
      res.json({ message: 'Appointment request updated successfully', updatedAppointment });
    }
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});


//session endpoints 
app.get('/session/:TherapistId/:SessionId', async (req, res) => {
  const therapistId = req.params.TherapistId;
  const sessionId = req.params.SessionId;
  try {
    const session = await getSession({ SessionId: sessionId });
    if (!session || session.therapistId !== therapistId) {
      return res.status(404).json({ message: 'Session not found' });
    }
    res.status(200).json(session);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching session', error });
  }
});

app.put('/session/:TherapistId/:SessionId', async (req, res) => {
  const therapistId = req.params.TherapistId;
  const sessionId = req.params.SessionId;
  const updateData = req.body;
  if (!therapistId || !sessionId || Object.keys(updateData).length === 0) {
    return res.status(400).json({ error: 'Invalid request. ID and update data are required.' });
  }
  try {
    const session = await getSession({ SessionId: sessionId });
    if (!session || session.therapistId !== therapistId) {
      return res.status(404).json({ message: 'Session not found' });
    }

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
    const updatedSession = await updateSession(
      { SessionId: sessionId },
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );

    res.json({ message: 'Session updated successfully', updatedSession });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.delete('/session/:TherapistId/:SessionId', async (req, res) => {
  const therapistId = req.params.TherapistId;
  const sessionId = req.params.SessionId;
  try {
    const session = await getSession({ SessionId: sessionId });
    if (!session || session.therapistId !== therapistId) {
      return res.status(404).json({ message: 'Session not found' });
    }

    await deleteSession({ SessionId: sessionId });
    res.status(204).send({ message: `Session with ID ${sessionId} deleted successfully.` });
  } catch (error) {
    res.status(500).send({ error: 'Failed to delete session.' });
  }
});

//messages endpoints 
// POST /messages
app.post('/messages', async (req, res) => {
  try {
    const { senderId, recipientId, content } = req.body;

    if (!senderId || !recipientId || !content) {
      return res.status(400).json({ message: 'Sender ID, recipient ID, and content are required' });
    }

    const message = {
      messageId: `msg-${Date.now()}`,
      senderId,
      recipientId,
      content,
      timestamp: new Date().toISOString(),
      status: 'sent'
    };

    await addMessage(message);
    res.status(201).json(message);
  } catch (error) {
    res.status(500).json({ message: 'Error sending message', error: error.message });
  }
});

// GET /messages
app.get('/messages', async (req, res) => {
  try {
    const { clientId, therapistId } = req.query;

    if (!clientId || !therapistId) {
      return res.status(400).json({ message: 'clientId and therapistId query parameters are required' });
    }

    const messages = await scanMessages(clientId, therapistId);
    res.status(200).json(messages);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching messages', error: error.message });
  }
});

// GET /messages/{messageId}
app.get('/messages/:messageId', async (req, res) => {
  try {
    const messageId = req.params.messageId;
    const message = await getMessage({ messageId });

    if (!message) {
      return res.status(404).json({ message: 'Message not found' });
    }

    res.status(200).json(message);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching message', error: error.message });
  }
});

// PUT /messages/{messageId}
app.put('/messages/:messageId', async (req, res) => {
  try {
    const messageId = req.params.messageId;
    const message = await getMessage({ messageId });

    if (!message) {
      return res.status(404).json({ message: 'Message not found' });
    }

    const allowedUpdates = ['content', 'status'];
    const invalidUpdates = Object.keys(req.body).filter(key => !allowedUpdates.includes(key));

    if (invalidUpdates.length > 0) {
      return res.status(400).json({
        message: `Invalid updates: ${invalidUpdates.join(', ')}`
      });
    }

    const updatedMessage = await updateMessage(
      { messageId },
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

// DELETE /messages/{messageId}
app.delete('/messages/:messageId', async (req, res) => {
  try {
    const messageId = req.params.messageId;
    const message = await getMessage({ messageId });

    if (!message) {
      return res.status(404).json({ message: 'Message not found' });
    }

    await deleteMessage({ messageId });
    res.status(204).send();
  } catch (error) {
    res.status(500).json({ message: 'Error deleting message', error: error.message });
  }
});

//mapping endpoints
// POST /therapists/{therapistId}/mapping-requests
app.post('/therapists/:therapistId/mapping-requests', async (req, res) => {
  try {
    const therapistId = req.params.therapistId;
    const { clientId } = req.body;

    if (!clientId) {
      return res.status(400).json({ message: 'clientId is required' });
    }

    const request = {
      mappingRequestId: `mapreq-${Date.now()}`,
      therapistId,
      clientId,
      status: 'pending',
      createdAt: new Date().toISOString()
    };

    await addMappingRequest(request);
    res.status(201).json(request);
  } catch (error) {
    res.status(500).json({ message: 'Error sending mapping request', error: error.message });
  }
});

// PUT /clients/{clientId}/mapping-requests/{therapistId}
app.put('/clients/:clientId/mapping-requests/:therapistId', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const therapistId = req.params.therapistId;
    const { status } = req.body;

    if (!status) {
      return res.status(400).json({ message: 'status is required' });
    }

    // Fetch the mapping request from MappingRequests table
    const request = await getMappingRequest(clientId, therapistId);

    if (!request) {
      return res.status(404).json({ message: 'Mapping request not found' });
    }

    // Update the status of the mapping request
    const updatedRequest = await updateMappingRequest(
      { mappingRequestId: request.mappingRequestId },
      'SET #status = :status',
      { ':status': status },
      { '#status': 'status' }
    );

    // If approved, copy details to MappedTherapists table
    if (status === 'approved') {
      const mapping = {
        clientId,
        therapistId,
        mappedAt: new Date().toISOString()
      };

      try {
        await addMappedTherapist(mapping);
        console.log('Mapped therapist added:', mapping);
      } catch (error) {
        console.error('Error adding mapped therapist:', error);
        return res.status(500).json({ message: 'Error adding mapped therapist', error: error.message });
      }
    }

    res.status(200).json(updatedRequest);
  } catch (error) {
    res.status(500).json({ message: 'Error updating mapping request', error: error.message });
  }
});


// DELETE /clients/{clientId}/mapped-therapists/{therapistId}
app.delete('/clients/:clientId/mapped-therapists/:therapistId', async (req, res) => {
  try {
    const clientId = req.params.clientId;
    const therapistId = req.params.therapistId;

    await deleteMappedTherapist({ clientId, therapistId });
    res.status(204).send();
  } catch (error) {
    res.status(500).json({ message: 'Error deleting mapped therapist', error: error.message });
  }
});



//--------------------------------------------------------------------------------------------------------------------------------------

// Therapists endpoint --- done
app.post('/therapists', async (req, res) => {
  try {
    const { email, name, location, expertise } = req.body;

    if (!email || !name) {
      return res.status(400).json({ message: 'Email and name are required.' });
    }

    const therapist = {
      TherapistId: `therapist-${Date.now()}`,
      email,
      name,
      location,
      expertise,
      mappedClientsIds: [] // Optional
    };

    await addTherapist(therapist);
    res.status(201).json({ message: 'Therapist added successfully', therapist });
  } catch (error) {
    console.error('Error adding therapist:', error);
    res.status(500).json({ message: 'Error adding therapist', error: error.message });
  }
});

// get all therapists
app.get('/therapists', async (req, res) => {
  try {
    const therapists = await scanTherapists();
    res.status(200).json(therapists);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching therapists', error });
  }
});
// Endpoint to get a therapist by ID
app.get('/therapists/:TherapistId', async (req, res) => {
  const therapistId = req.params.TherapistId;
  try {
    const therapist = await getTherapist({ TherapistId: therapistId });
    if (!therapist) {
      return res.status(404).json({ message: 'Therapist not found' });
    }
    res.status(200).json(therapist);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching therapist', error });
  }
});

// Endpoint for therapist to update by ID
app.put('/therapists/:TherapistId', async (req, res) => {
  const therapistId = req.params.TherapistId;
  const updateData = req.body;
  if (!therapistId || Object.keys(updateData).length === 0) {
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
    const updatedTherapist = await updateTherapist(
      { TherapistId: therapistId },
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
app.delete('/therapists/:TherapistId', async (req, res) => {
  const therapistId = req.params.TherapistId;
  try {
    await deleteTherapist({ TherapistId: therapistId });
    res.status(204).send({ message: `Therapist with ID ${therapistId} deleted successfully.` });
  } catch (error) {
    res.status(500).send({ error: 'Failed to delete therapist profile.' });
  }
});

//search endpoint for therapists 
app.get('/therapists/:therapistId/search', async (req, res) => {
  try {
    const therapistId = req.params.therapistId;
    const { keyword } = req.query;

    if (!keyword) {
      return res.status(400).json({ message: 'Keyword is required for search' });
    }

    // Search clients, notes, and journals
    const clients = await searchClients(keyword);
    const journals = await searchJournals(keyword, 'client');
    const messages = await searchMessages(therapistId, keyword);

    res.status(200).json({
      therapistId,
      results: {
        clients,
        journals,
        messages,
      },
    });
  } catch (error) {
    console.error('Error performing search:', error);
    res.status(500).json({ message: 'Error performing search', error: error.message });
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
