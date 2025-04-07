package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class DeleteJournalEntryHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            String journalEntryId = pathParams.get("journalEntryId"); // Correct attribute name

            // 2. Validate input
            if (clientId == null || clientId.isEmpty() || journalEntryId == null || journalEntryId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID and Journal Entry ID are required");
            }

            // 3. Delete the journal entry from DynamoDB
            dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(System.getenv("JOURNAL_ENTRIES_TABLE"))
                .key(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "JournalEntryId", AttributeValue.builder().s(journalEntryId).build()
                ))
                .build());

            // 4. Return 204 No Content response
            return ResponseUtils.noContentResponse();

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error deleting journal entry: " + e.getMessage());
        }
    }
}
