package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;
import java.time.Instant;

public class CreateJournalEntryHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract and validate path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            
            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }

            // 2. Parse and validate request body
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            
            if (!requestBody.containsKey("date") || 
                !requestBody.containsKey("time") || 
                !requestBody.containsKey("content")) {
                return ResponseUtils.errorResponse(400, "Missing required fields: date, time, or content");
            }

            // 3. Create journal entry
            String journalEntryId = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = new HashMap<>();
            
            // Required attributes
            item.put("ClientId", AttributeValue.builder().s(clientId).build());
            item.put("JournalEntryId", AttributeValue.builder().s(journalEntryId).build());
            item.put("date", AttributeValue.builder().s(requestBody.get("date").toString()).build());
            item.put("time", AttributeValue.builder().s(requestBody.get("time").toString()).build());
            item.put("content", AttributeValue.builder().s(requestBody.get("content").toString()).build());

            // Optional attributes
            if (requestBody.containsKey("feeling")) {
                item.put("feeling", AttributeValue.builder().s(requestBody.get("feeling").toString()).build());
            }
            if (requestBody.containsKey("intensity")) {
                item.put("intensity", AttributeValue.builder().n(requestBody.get("intensity").toString()).build());
            }

            // 4. Store in DynamoDB
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("JOURNAL_ENTRIES_TABLE"))
                .item(item)
                .build());

            // 5. Return response
            Map<String, Object> response = new HashMap<>();
            response.put("ClientId", clientId);
            response.put("JournalEntryId", journalEntryId);
            response.put("date", requestBody.get("date"));
            response.put("time", requestBody.get("time"));
            response.put("content", requestBody.get("content"));
            if (requestBody.containsKey("feeling")) response.put("feeling", requestBody.get("feeling"));
            if (requestBody.containsKey("intensity")) response.put("intensity", requestBody.get("intensity"));

            return ResponseUtils.successResponse(201, response);

        } catch (NumberFormatException e) {
            return ResponseUtils.errorResponse(400, "Invalid intensity format - must be integer");
        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error creating journal entry: " + e.getMessage());
        }
    }
}
