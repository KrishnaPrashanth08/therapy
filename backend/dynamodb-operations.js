const AWS = require('./aws-config');
const dynamodb = new AWS.DynamoDB.DocumentClient();

const TABLE_NAME = 'prashanth';
const TABLE_NAME_2 = 'therapy-data'; 
const CLIENTS_TABLE = 'Clients';
const THERAPIST_TABLE = 'Therapists';
const SESSION_SLOTS_TABLE = 'SessionSlots';
const APPOINTMENT_REQUESTS_TABLE = 'AppointmentRequests';
const SESSIONS_TABLE = 'Sessions';
const JOURNAL_ENTRIES_TABLE = 'JournalEntries';
const JOURNAL_ACCESS_TABLE = 'JournalAccessRequests';
const MESSAGES_TABLE = 'Messages';
const MAPPING_REQUESTS_TABLE = 'MappingRequests';
const MAPPED_THERAPISTS_TABLE = 'MappedTherapists';

// Authentication Table Functions 

// Function to add an item to the authentication table
async function addItem(item) {
  const params = {
    TableName: TABLE_NAME,
    Item: item,
  };
  try {
    await dynamodb.put(params).promise();
    console.log('Item added successfully:', item);
  } catch (error) {
    console.error('Error adding item:', error);
    throw new Error('Failed to add item to the database.');
  }
}



// Function to get an item from the authentication table
async function getItem(key) {
  const params = {
    TableName: TABLE_NAME,
    Key: key,
  };
  try {
    const data = await dynamodb.get(params).promise();
    if (!data.Item) {
      console.warn('No item found for key:', key);
      return null;
    }
    console.log('Item fetched successfully:', data.Item);
    return data.Item; 
  } catch (error) {
    console.error('Error fetching item:', error);
    throw new Error('Failed to fetch item from the database.');
  }
}

// Function to update an item in the authentication table
async function updateItem(key, updateExpression, expressionValues) {
  const params = {
    TableName: TABLE_NAME,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    console.log('Item updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating item:', error);
    throw new Error('Failed to update item in the database.');
  }
}

// Function to delete an item from the authentication table
async function deleteItem(key) {
  const params = {
    TableName: TABLE_NAME,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Item deleted successfully:', key);
  } catch (error) {
    console.error('Error deleting item:', error);
    throw new Error('Failed to delete item from the database.');
  }
}

// functions for clients table


async function addClient(client){
  const params = {
    TableName : CLIENTS_TABLE,
    Item: client,
  };
  try{
    await dynamodb.put(params).promise();
    console.log('client added successfully:',client);
  }catch(error){
    console.log('error adding client:',error);
    throw new  Error('failed to add client to DB.');
  }
}
// Function to get a client from the Clients table
async function getClient(key) {
  const params = {
    TableName: CLIENTS_TABLE,
    Key: key,
  };
  try {
    const data = await dynamodb.get(params).promise();
    if (!data.Item) {
      console.warn('No client found for key:', key);
      return null;
    }
    console.log('Client fetched successfully:', data.Item);
    return data.Item;
  } catch (error) {
    console.error('Error fetching client:', error);
    throw new Error('Failed to fetch client from the database.');
  }
}

// Function to update a client in the Clients table
async function updateClient(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: CLIENTS_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    console.log('Client updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating client:', error);
    throw new Error('Failed to update client in the database.');
  }
}

// Function to delete a client from the Clients table
async function deleteClient(key) {
  const params = {
    TableName: CLIENTS_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Client deleted successfully:', key);
  } catch (error) {
    console.error('Error deleting client:', error);
    throw new Error('Failed to delete client from the database.');
  }
}

// Function to scan all clients
async function scanClients() {
  const params = {
    TableName: CLIENTS_TABLE,
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Clients fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning clients:', error);
    throw new Error('Failed to scan clients');
  }
}



// functions for therapists table 

 async function addTherapist(therapist){
  const params = {
    TableName : THERAPIST_TABLE ,
    Item : therapist,

  }
  try{
    await dynamodb.put(params).promise();
    console.log('therapist added successfully:', therapist);
  
  }
  catch(error){
    console.error('Error adding therapist:', error);
    throw new Error('Failed to add therapist to the database.')
  }
 }

 async function getTherapist(key){
   const params = {
     TableName:THERAPIST_TABLE ,
     Key : key,
   }
   try{
    const data  = await dynamodb.get(params).promise();
    if(!data.Item){
      console.warn('no theraapist found:', key);
      return null;
    }
    console.log('therapist found :', data.Item);
    return data.Item;
   }
   catch(error){
    console.error('Error fetching therapist:', error);
    throw new Error('Failed to fetch therapist from the database.');
   }
 }

 async function updateTherapist(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: THERAPIST_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    console.log('Therapist updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating therapist:', error);
    throw new Error('Failed to update therapist in the database.');
  }
}

async function deleteTherapist(key){
  const params = {
    TableName: THERAPIST_TABLE ,
    Key : key,
  }
  try{
    await dynamodb.delete(params).promise();
    console.log('therapist deleted successfully:', key);
  }
  catch(error){
    console.error('Error deleting therapist:', error);
    throw new Error('Failed to delete therapist from the database.');
  }
  }
  
async function scanTherapists(){
  const params = {
    TableName:THERAPIST_TABLE ,
  }
  try{
     const data = await dynamodb.scan(params).promise();
    console.log('Therapists fetched successfully:', data.Items);
    return data.Items;
  }catch(error){
    console.error('Error scanning therapists:', error);
    throw new Error('Failed to scan therapists');
  }
}

//functions for sessionslots 

async function addSessionSlot(slot){
  const params = {
    TableName: SESSION_SLOTS_TABLE , 
    Item: slot,
  }
  try{
    await dynamodb.put(params).promise();
    console.log('session slot added succesfully:', slot);
  }catch(error){
    console.error('Error adding session slot:', error);
    throw new Error('Failed to add session slot to the database.');

  }
}

async function getSessionSlot(key) {
  const params = {
    TableName: SESSION_SLOTS_TABLE,
    Key: key,
  };
  try {
    const data = await dynamodb.get(params).promise();
    if (!data.Item) {
      console.warn('No session slot found for key:', key);
      return null;
    }
    console.log('Session slot fetched successfully:', data.Item);
    return data.Item;
  } catch (error) {
    console.error('Error fetching session slot:', error);
    throw new Error('Failed to fetch session slot from the database.');
  }
}

async function updateSessionSlot(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: SESSION_SLOTS_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    console.log('Session slot updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating session slot:', error);
    throw new Error('Failed to update session slot in the database.');
  }
}

async function deleteSessionSlot(key) {
  const params = {
    TableName: SESSION_SLOTS_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Session slot deleted successfully:', key);
  } catch (error) {
    console.error('Error deleting session slot:', error);
    throw new Error('Failed to delete session slot from the database.');
  }
}

async function scanSessionSlots(therapistId) {
  const params = {
    TableName: SESSION_SLOTS_TABLE,
    FilterExpression: '#therapistId = :therapistId',
    ExpressionAttributeNames: {
      '#therapistId': 'therapistId',
    },
    ExpressionAttributeValues: {
      ':therapistId': therapistId,
    },
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Session slots fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning session slots:', error);
    throw new Error('Failed to scan session slots');
  }
}


//APPOINTMENTREQUEST TABLE FUNCIOTNS

async function addAppointmentRequest(request) {
  const params = {
    TableName: APPOINTMENT_REQUESTS_TABLE,
    Item: request,
  };
  try {
    await dynamodb.put(params).promise();
    console.log('Appointment request added successfully:', request);
  } catch (error) {
    console.error('Error adding appointment request:', error);
    throw new Error('Failed to add appointment request to the database.');
  }
}

async function getAppointmentRequest(key) {
  const params = {
    TableName: APPOINTMENT_REQUESTS_TABLE,
    Key: key,
  };
  try {
    const data = await dynamodb.get(params).promise();
    if (!data.Item) {
      console.warn('No appointment request found for key:', key);
      return null;
    }
    console.log('Appointment request fetched successfully:', data.Item);
    return data.Item;
  } catch (error) {
    console.error('Error fetching appointment request:', error);
    throw new Error('Failed to fetch appointment request from the database.');
  }
}

async function updateAppointmentRequest(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: APPOINTMENT_REQUESTS_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    console.log('Appointment request updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating appointment request:', error);
    throw new Error('Failed to update appointment request in the database.');
  }
}

async function deleteAppointmentRequest(key) {
  const params = {
    TableName: APPOINTMENT_REQUESTS_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Appointment request deleted successfully:', key);
  } catch (error) {
    console.error('Error deleting appointment request:', error);
    throw new Error('Failed to delete appointment request from the database.');
  }
}

async function scanAppointmentRequests() {
  const params = {
    TableName: APPOINTMENT_REQUESTS_TABLE,
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Appointment requests fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning appointment requests:', error);
    throw new Error('Failed to scan appointment requests');
  }
}

async function scanAppointmentRequestsByTherapistId(therapistId) {
  const params = {
    TableName: APPOINTMENT_REQUESTS_TABLE,
    FilterExpression: '#TherapistId = :therapistId',
    ExpressionAttributeNames: {
      '#TherapistId': 'TherapistId',
    },
    ExpressionAttributeValues: {
      ':therapistId': therapistId,
    },
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Appointment requests fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning appointment requests:', error);
    throw new Error('Failed to scan appointment requests');
  }
}


//functions for sessions table

async function addSession(session){
  const params = {
      TableName:SESSIONS_TABLE,
      Item : session,
  }
  try{
    await dynamodb.put(params).promise();
    console.log('session added successfully:',session);
  }
  catch(error){
    console.error('Error adding session:', error);
    throw new Error('Failed to add session to the database.');
  }
}

async function getSession(key){
  const params = {
    TableName:SESSIONS_TABLE,
    Key:key,
  }
  try{
    const data = await dynamodb.get(params).promise();
    if(!data.Item){
      console.log('no session found for key',key);
      return null;
    }
    console.log('sesion fgetched successfully',data.Item);
    return data.Item;
  }
  catch(error){
    console.error('Error fetching session:', error);
    throw new Error('Failed to fetch session from the database.');
  }
}

async function updateSession(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: SESSIONS_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    console.log('Session updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating session:', error);
    throw new Error('Failed to update session in the database.');
  }
}

async function deleteSession(key){
  const params = {
    TableName:SESSIONS_TABLE,
    Key :key ,
  }
  try{
    await dynamodb.delete(parms).promise();
    console.log('Session deleted successfully:', key);
  }
  catch(error){
    console.error('Error deleting session:', error);
    throw new Error('Failed to delete session from the database.');
  }
 
}

async function scanSessions() {
  const params = {
    TableName: SESSIONS_TABLE,
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Sessions fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning sessions:', error);
    throw new Error('Failed to scan sessions');
  }
}

//functions for journalEntries
async function addJournalEntry(entry){
  const params = {
    TableName : 'JournalEntries',
    Item : entry,
  }
   try{
    await dynamodb.put(params).promise();
    console.log('journal entry successfull:',entry);
   }
   catch(error){
    console.error('Error adding journal entry:', error);
    throw new Error('Failed to add journal entry');
   }
}

async function getJournalEntry(key){
  const params = {
    TableName : JOURNAL_ENTRIES_TABLE , 
    Key : key ,
  }
  try{
    const data = await dynamodb.get(params).promise();
    return data.Item;
    
  }
  catch(error){
    console.error('Error fetching journal entry:', error);
    throw new Error('Failed to fetch journal entry');
  }
}

async function updateJournalEntry(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: JOURNAL_ENTRIES_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    return result.Attributes;
  } catch (error) {
    throw new Error('Journal entry update failed');
  }
}

async function deleteJournalEntry(key) {
  const params = {
    TableName: JOURNAL_ENTRIES_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
  } catch (error) {
    throw new Error('Journal entry deletion failed');
  }
}

async function scanJournalEntries(clientId) {
  const params = {
    TableName: JOURNAL_ENTRIES_TABLE,
    FilterExpression: 'clientId = :clientId',
    ExpressionAttributeValues: { ':clientId': clientId }
  };
  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    throw new Error('Journal entries scan failed');
  }
}

// Journal Access Requests Functions
async function addJournalAccessRequest(request) {
  const params = {
    TableName: JOURNAL_ACCESS_TABLE,
    Item: request,
  };
  try {
    await dynamodb.put(params).promise();
    console.log('Access request added:', request);
  } catch (error) {
    console.error('Error adding access request:', error);
    throw new Error('Failed to add access request');
  }
}

async function getJournalAccessRequest(key) {
  const params = {
    TableName: JOURNAL_ACCESS_TABLE,
    Key: key,
  };
  try {
    const data = await dynamodb.get(params).promise();
    return data.Item;
  } catch (error) {
    console.error('Error fetching access request:', error);
    throw new Error('Failed to fetch access request');
  }
}

async function updateJournalAccessRequest(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: JOURNAL_ACCESS_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    return result.Attributes;
  } catch (error) {
    throw new Error('Access request update failed');
  }
}

async function updateJournalAccessPermissions(clientId, therapists) {
  
  const key = { ClientId: clientId };
  const updateExpression = 'SET allowedTherapists = :therapists';
  const expressionValues = { ':therapists': therapists };
  const expressionAttributeNames = {};

  try {
    const result = await updateJournalAccessRequest(
      key,
      updateExpression,
      expressionValues,
      expressionAttributeNames
    );
    return result;
  } catch (error) {
    throw new Error('Failed to update journal access permissions');
  }
}

async function deleteJournalAccessRequest(key) {
  const params = {
    TableName: JOURNAL_ACCESS_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
  } catch (error) {
    throw new Error('Access request deletion failed');
  }
}

async function scanJournalAccessRequests(clientId) {
  const params = {
    TableName: JOURNAL_ACCESS_TABLE,
    FilterExpression: 'ClientId = :clientId AND #status = :status',
    ExpressionAttributeNames: { '#status': 'status' },
    ExpressionAttributeValues: {
      ':clientId': clientId,
      ':status': 'pending'
    }
  };
  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    throw new Error('Access requests scan failed');
  }
}

async function getTherapistsWithJournalAccess(clientId) {
  const params = {
    TableName: JOURNAL_ACCESS_TABLE,
    FilterExpression: 'ClientId = :clientId AND #status = :status',
    ExpressionAttributeValues: {
      ':clientId': clientId,
      ':status': 'approved'
    },
    ExpressionAttributeNames: {
      '#status': 'status'
    }
  };

  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    throw new Error('Failed to fetch therapists with journal access');
  }
}

//messages functions
async function addMessage(message){
  const params = {
    TableName : MESSAGES_TABLE,
    Item : message,
  }

  try{
    await dynamodb.put(params).promise();
    console.log('message added:',message);
  }
  catch(error){
    console.error('Error adding message:', error);
    throw new Error('Failed to add message');
  }
}
async function getMessage(key){
  const params = {
    TableName:MESSAGES_TABLE,
    Key :key ,
  }

  try{
    const data = await dynamodb.get(params).promise();
    return data.Item;
  }catch(error){
    console.error('error fetching message',error);
    throw new Error('failed to fetch message');
  }
}
async function updateMessage(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: MESSAGES_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };
  try {
    const result = await dynamodb.update(params).promise();
    return result.Attributes;
  } catch (error) {
    console.error('Error updating message:', error);
    throw new Error('Failed to update message');
  }
}
async function deleteMessage(key) {
  const params = {
    TableName: MESSAGES_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Message deleted:', key);
  } catch (error) {
    console.error('Error deleting message:', error);
    throw new Error('Failed to delete message');
  }
}
async function scanMessages(clientId, therapistId) {
  const params = {
    TableName: MESSAGES_TABLE,
    FilterExpression: '(senderId = :clientId AND recipientId = :therapistId) OR ' +
                     '(senderId = :therapistId AND recipientId = :clientId)',
    ExpressionAttributeValues: {
      ':clientId': clientId,
      ':therapistId': therapistId
    }
  };
  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Error scanning messages:', error);
    throw new Error('Failed to scan messages');
  }
}

//mapping functions
async function addMappingRequest(request) {
  const params = {
    TableName: MAPPING_REQUESTS_TABLE,
    Item: request,
  };
  try {
    await dynamodb.put(params).promise();
    console.log('Mapping request added:', request);
  } catch (error) {
    console.error('Error adding mapping request:', error);
    throw new Error('Failed to add mapping request');
  }
}

async function updateMappingRequest(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: MAPPING_REQUESTS_TABLE,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames,
    ReturnValues: 'UPDATED_NEW',
  };

  try {
    const result = await dynamodb.update(params).promise();
    return result.Attributes;
  } catch (error) {
    console.error('Error updating mapping request:', error);
    throw new Error('Failed to update mapping request');
  }
}

async function getMappingRequest(clientId, therapistId) {
  const params = {
    TableName: MAPPING_REQUESTS_TABLE,
    FilterExpression: 'clientId = :clientId AND therapistId = :therapistId',
    ExpressionAttributeValues: {
      ':clientId': clientId,
      ':therapistId': therapistId
    }
  };

  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items[0]; // Assuming there's only one request per client-therapist pair
  } catch (error) {
    console.error('Error fetching mapping request:', error);
    throw new Error('Failed to fetch mapping request');
  }
}

async function addMappedTherapist(mapping) {
  const params = {
    TableName: MAPPED_THERAPISTS_TABLE,
    Item: mapping,
  };
  try {
    await dynamodb.put(params).promise();
    console.log('Mapped therapist added:', mapping);
  } catch (error) {
    console.error('Error adding mapped therapist:', error);
    throw new Error('Failed to add mapped therapist');
  }
}



async function deleteMappedTherapist(key) {
  const params = {
    TableName: MAPPED_THERAPISTS_TABLE,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Mapped therapist deleted:', key);
  } catch (error) {
    console.error('Error deleting mapped therapist:', error);
    throw new Error('Failed to delete mapped therapist');
  }
}


//search functions 
async function searchTherapists(keyword) {
  const params = {
    TableName: 'Therapists',
    FilterExpression: 'contains(#location, :keyword) OR contains(#expertise, :keyword)',
    ExpressionAttributeNames: {
      '#location': 'location',
      '#expertise': 'expertise',
    },
    ExpressionAttributeValues: {
      ':keyword': keyword,
    },
  };

  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Error searching therapists:', error);
    throw new Error('Failed to search therapists');
  }
}

async function searchClients(keyword) {
  const params = {
    TableName: 'Clients',
    FilterExpression: 'contains(#name, :keyword)',
    ExpressionAttributeNames: { '#name': 'name' },
    ExpressionAttributeValues: { ':keyword': keyword },
  };

  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Error searching clients:', error);
    throw new Error('Failed to search clients');
  }
}

async function searchJournals(keyword, ownerType) {
  const params = {
    TableName: 'JournalEntries',
    FilterExpression: 'contains(#content, :keyword) AND #ownerType = :ownerType',
    ExpressionAttributeNames: {
      '#content': 'content',
      '#ownerType': 'ownerType',
    },
    ExpressionAttributeValues: {
      ':keyword': keyword,
      ':ownerType': ownerType,
    },
  };

  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Error searching journals:', error);
    throw new Error('Failed to search journals');
  }
}

async function searchMessages(ownerId, keyword) {
  const params = {
    TableName: 'Messages',
    FilterExpression:
      '(senderId = :ownerId OR recipientId = :ownerId) AND contains(#content, :keyword)',
    ExpressionAttributeNames: { '#content': 'content' },
    ExpressionAttributeValues: {
      ':ownerId': ownerId,
      ':keyword': keyword,
    },
  };

  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Error searching messages:', error);
    throw new Error('Failed to search messages');
  }
}






//----------------------------------------------------------------------------------------------------





module.exports = {
  addItem,
  getItem,
  updateItem,
  deleteItem,
 
  
  scanSessions,
  
  scanJournalAccessRequests,


  addClient,
  getClient,
  updateClient,
  deleteClient,
  scanClients,
  addTherapist,
  getTherapist,
  updateTherapist,
  deleteTherapist,
  scanTherapists,
  addSessionSlot,
  getSessionSlot,
  updateSessionSlot,
  deleteSessionSlot,
  scanSessionSlots,
  addAppointmentRequest,
  getAppointmentRequest,
  updateAppointmentRequest,
  deleteAppointmentRequest,
  scanAppointmentRequests,
  addSession,
  getSession,
  updateSession,
  deleteSession,
  scanSessions,
  scanAppointmentRequestsByTherapistId,
  addJournalEntry,
  getJournalEntry,
  updateJournalEntry,
  deleteJournalEntry,
  scanJournalEntries,
  addJournalAccessRequest,
  getJournalAccessRequest,
  updateJournalAccessRequest,
  deleteJournalAccessRequest,
  updateJournalAccessPermissions,
  getTherapistsWithJournalAccess,
  addMessage,
  getMessage,
  updateMessage,
  deleteMessage,
  scanMessages,
  addMappingRequest,
  updateMappingRequest,
  deleteMappedTherapist,
  addMappedTherapist,
  getMappingRequest,
  searchTherapists,
  searchClients,
  searchJournals,
  searchMessages,
};
