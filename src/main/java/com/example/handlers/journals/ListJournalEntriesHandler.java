package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ListJournalEntriesHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final String JOURNAL_ENTRIES_TABLE = System.getenv("JOURNAL_ENTRIES_TABLE");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract client ID from path parameters
            Map<String, String> pathParameters = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParameters.get("clientId");

            // 2. Validate input
            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }

            // 3. Build DynamoDB query request
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":clientId", AttributeValue.builder().s(clientId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                .tableName(JOURNAL_ENTRIES_TABLE)
                .keyConditionExpression("ClientId = :clientId")
                .expressionAttributeValues(expressionValues)
                .build();

            // 4. Execute query
            QueryResponse queryResponse = dynamoDb.query(queryRequest);

            // 5. Process results
            List<Map<String, Object>> journalEntries = new ArrayList<>();
            queryResponse.items().forEach(item -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("ClientId", item.get("ClientId").s());
                entry.put("JournalEntryId", item.get("JournalEntryId").s());
                entry.put("date", item.get("date").s());
                entry.put("time", item.get("time").s());
                entry.put("content", item.get("content").s());

                // Handle optional attributes
                if (item.containsKey("feeling")) {
                    entry.put("feeling", item.get("feeling").s());
                }
                if (item.containsKey("intensity")) {
                    entry.put("intensity", Integer.parseInt(item.get("intensity").n()));
                }
                journalEntries.add(entry);
            });

            // 6. Return response
            return ResponseUtils.successResponse(200, journalEntries);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error retrieving journal entries: " + e.getMessage());
        }
    }
}
