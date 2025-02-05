const AWS = require('./aws-config');
const dynamodb = new AWS.DynamoDB.DocumentClient();

const TABLE_NAME = 'prashanth';
const TABLE_NAME_2 = 'therapy-data'; 

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



// Function to add an item to the therapy data table
async function addTherapyData(item) {
  const params = {
    TableName: TABLE_NAME_2,
    Item: item,
  };
  try {
    await dynamodb.put(params).promise();
    console.log('Therapy data added successfully:', item);
  } catch (error) {
    console.error('Error adding therapy data:', error);
    throw new Error('Failed to add therapy data to the database.');
  }
}

// Function to get an item from the therapy data table
async function getTherapyData(key) {
  const params = {
    TableName: TABLE_NAME_2,
    Key: key, 
  };

  try {
    const data = await dynamodb.get(params).promise();

    if (!data.Item) {
      console.warn('No therapy data found for key:', key);
      return null; 
    }

    console.log('Therapy data fetched successfully:', data.Item);
    return data.Item; 
  } catch (error) {
    console.error('Error fetching therapy data:', error);
    throw new Error('Failed to fetch therapy data from the database.');
  }
}


// Function to update an item in the therapy data table
async function updateTherapyData(key, updateExpression, expressionValues, expressionAttributeNames) {
  const params = {
    TableName: TABLE_NAME_2,
    Key: key,
    UpdateExpression: updateExpression,
    ExpressionAttributeValues: expressionValues,
    ExpressionAttributeNames: expressionAttributeNames, 
    ReturnValues: 'UPDATED_NEW',
  };

  try {
    const result = await dynamodb.update(params).promise();
    console.log('Therapy data updated successfully:', result.Attributes);
    return result.Attributes;
  } catch (error) {
    console.error('Error updating therapy data:', error);
    throw new Error('Failed to update therapy data in the database.');
  }
}

// Function to delete an item from the therapy data table
async function deleteTherapyData(key) {
  const params = {
    TableName: TABLE_NAME_2,
    Key: key,
  };
  try {
    await dynamodb.delete(params).promise();
    console.log('Therapy data deleted successfully:', key);
  } catch (error) {
    console.error('Error deleting therapy data:', error);
    throw new Error('Failed to delete therapy data from the database.');
  }
}


// Scan Function for Retrieving All Clients ----done
async function scanClients() {
  const params = {
    TableName: TABLE_NAME_2,
    
    FilterExpression: '#typeAttr = :typeVal',
    ExpressionAttributeNames: {
      '#typeAttr': 'type', 
    },
    ExpressionAttributeValues: {
      ':typeVal': 'client', 
    },
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



// Scan Function for Retrieving All Therapists ---- done
async function scanTherapists() {
  const params = {
    TableName: TABLE_NAME_2,
    
    FilterExpression: '#typeAttr = :typeVal',
    ExpressionAttributeNames: {
      '#typeAttr': 'type', 
    },
    ExpressionAttributeValues: {
      ':typeVal': 'therapist', 
    },
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Therapists fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning therapists:', error);
    throw new Error('Failed to scan therapists');
  }
}


// Scan Function for Retrieving All Sessions 

async function scanSessions() {
  const params = {
    TableName: TABLE_NAME_2,
    FilterExpression: '#type = :type',
    ExpressionAttributeNames: {
      '#type': 'type'
    },
    ExpressionAttributeValues: {
      ':type': 'session'
    }
  };
  
  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Session scan error:', error);
    throw error;
  }
}



// Scan Function for Retrieving All Messages (Adjust the parameters as needed)
async function scanMessages() {
  const params = {
    TableName: TABLE_NAME_2,
  };
  try {
    const data = await dynamodb.scan(params).promise();
    console.log('Messages fetched successfully:', data.Items);
    return data.Items;
  } catch (error) {
    console.error('Error scanning messages:', error);
    throw new Error('Failed to scan messages');
  }
}

//
const scanJournalAccessRequests = async (clientId) => {
  const params = {
    TableName: 'therapy-data',
    FilterExpression: 'clientId = :clientId AND #type = :type AND #status = :status',
    ExpressionAttributeNames: {
      '#type': 'type',
      '#status': 'status'
    },
    ExpressionAttributeValues: {
      ':clientId': clientId,
      ':type': 'journal_access_request',
      ':status': 'pending'
    }
  };
  return dynamodb.scan(params).promise();
};


// Function to retrieve therapists with journal access for a specific client
const scanTherapistsWithAccess = async (clientId) => {
  const params = {
    TableName: 'therapy-data',
    FilterExpression: 'clientId = :clientId AND #type = :type AND #status = :status',
    ExpressionAttributeNames: {
      '#type': 'type',
      '#status': 'status'
    },
    ExpressionAttributeValues: {
      ':clientId': clientId,
      ':type': 'journal_access_request',
      ':status': 'approved'
    }
  };
  return dynamodb.scan(params).promise();
};


async function scanAppointmentRequests(therapistId) {
  const params = {
    TableName: 'therapy-data',
    FilterExpression: 'therapistId = :therapistId AND #type = :type AND #status = :status',
    ExpressionAttributeValues: {
      ':therapistId': therapistId,
      ':type': 'appointment_request',
      ':status': 'pending'
    },
    ExpressionAttributeNames: {
      '#type': 'type',
      '#status': 'status'
    }
  };
  
  try {
    const data = await dynamodb.scan(params).promise();
    return data.Items;
  } catch (error) {
    console.error('Error scanning appointment requests:', error);
    throw error;
  }
}





module.exports = {
  addItem,
  getItem,
  updateItem,
  deleteItem,
  addTherapyData,
  getTherapyData,
  updateTherapyData,
  deleteTherapyData,
  scanClients,
  scanTherapists,
  scanSessions,
  scanMessages,
  scanJournalAccessRequests,
  scanTherapistsWithAccess,
  scanAppointmentRequests,
};
