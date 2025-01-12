const AWS = require('./aws-config');
const dynamodb = new AWS.DynamoDB.DocumentClient();

const TABLE_NAME = 'prashanth';

// Function to add an item
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

// Function to get an item by key (e.g., email)
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
    return data.Item; // Includes 'role' and other attributes
  } catch (error) {
    console.error('Error fetching item:', error);
    throw new Error('Failed to fetch item from the database.');
  }
}

// Function to update an item
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

// Function to delete an item
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

module.exports = { addItem, getItem, updateItem, deleteItem };
