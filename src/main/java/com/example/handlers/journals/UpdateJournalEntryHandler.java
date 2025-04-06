package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.HashMap;
import java.util.Map;

public class UpdateJournalEntryHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            String journalEntryId = pathParams.get("journalEntryId");

            // 2. Validate input
            if (clientId == null || clientId.isEmpty() || journalEntryId == null || journalEntryId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID and Journal ID are required");
            }

            // 3. Parse request body
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);

            // 4. Build update expression
            Map<String, String> attributeNames = new HashMap<>();
            Map<String, AttributeValue> attributeValues = new HashMap<>();
            StringBuilder updateExpression = new StringBuilder("SET ");

            int counter = 1;
            for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
                String attribute = entry.getKey();
                Object value = entry.getValue();

                // Skip null values
                if (value == null) {
                    continue; // Skip this attribute
                }

                // Validate attribute name
                if (!isValidAttribute(attribute)) {
                    return ResponseUtils.errorResponse(400, "Invalid attribute name: " + attribute);
                }

                String placeholder = ":val" + counter;
                attributeNames.put("#" + attribute, attribute);

                // Handle different data types
                if (attribute.equals("intensity")) {
                    try {
                        int intensityValue = Integer.parseInt(value.toString());
                        attributeValues.put(placeholder, AttributeValue.builder().n(String.valueOf(intensityValue)).build());
                    } catch (NumberFormatException e) {
                        return ResponseUtils.errorResponse(400, "Invalid intensity format - must be integer");
                    }
                } else {
                    attributeValues.put(placeholder, AttributeValue.builder().s(value.toString()).build());
                }

                updateExpression.append("#").append(attribute).append(" = ").append(placeholder);
                if (counter < requestBody.size()) updateExpression.append(", ");
                counter++;
            }
            

            // 5. Perform update
            UpdateItemResponse response = dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(System.getenv("JOURNAL_ENTRIES_TABLE"))
                .key(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "JournalEntryId", AttributeValue.builder().s(journalEntryId).build()
                ))
                .updateExpression(updateExpression.toString())
                .expressionAttributeNames(attributeNames)
                .expressionAttributeValues(attributeValues)
                .returnValues(ReturnValue.ALL_NEW)
                .build());

            // 6. Return updated journal entry
            Map<String, AttributeValue> item = response.attributes();
            Map<String, Object> journalEntry = new HashMap<>();
            journalEntry.put("ClientId", item.get("ClientId").s());
            journalEntry.put("JournalEntryId", item.get("JournalEntryId").s());
            journalEntry.put("date", item.get("date").s());
            journalEntry.put("time", item.get("time").s());
            journalEntry.put("content", item.get("content").s());

            if (item.containsKey("feeling")) {
                journalEntry.put("feeling", item.get("feeling").s());
            }
            if (item.containsKey("intensity")) {
                journalEntry.put("intensity", Integer.parseInt(item.get("intensity").n()));
            }

            return ResponseUtils.successResponse(200, journalEntry);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error updating journal entry: " + e.getMessage());
        }
    }

    // Helper function to validate attribute names
    private boolean isValidAttribute(String attribute) {
        return attribute.equals("date") ||
               attribute.equals("time") ||
               attribute.equals("content") ||
               attribute.equals("feeling") ||
               attribute.equals("intensity");
    }
}
