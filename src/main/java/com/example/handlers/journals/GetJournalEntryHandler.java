package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import java.util.HashMap;
import java.util.Map;

public class GetJournalEntryHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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

            // 3. Build DynamoDB request
            GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(System.getenv("JOURNAL_ENTRIES_TABLE"))
                .key(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "JournalEntryId", AttributeValue.builder().s(journalEntryId).build()
                ))
                .build();

            // 4. Retrieve item
            GetItemResponse getItemResponse = dynamoDb.getItem(getItemRequest);

            // 5. Process result
            if (getItemResponse.item() == null || getItemResponse.item().isEmpty()) {
                return ResponseUtils.errorResponse(404, "Journal entry not found");
            }

            Map<String, AttributeValue> item = getItemResponse.item();
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

            // 6. Return response
            return ResponseUtils.successResponse(200, journalEntry);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error retrieving journal entry: " + e.getMessage());
        }
    }
}
