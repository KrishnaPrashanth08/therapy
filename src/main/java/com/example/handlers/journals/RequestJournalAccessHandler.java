package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class RequestJournalAccessHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");

            // 2. Parse request body
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            String clientId = (String) requestBody.get("clientId");

            // 3. Validate input
            if (therapistId == null || therapistId.isEmpty() || clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Therapist ID and Client ID are required");
            }

            // 4. Create journal access request
            String journalAccessRequestId = UUID.randomUUID().toString();
            String createdAt = Instant.now().toString();

            Map<String, AttributeValue> item = Map.of(
                "ClientId", AttributeValue.builder().s(clientId).build(),
                "JournalAccessRequestId", AttributeValue.builder().s(journalAccessRequestId).build(),
                "TherapistId", AttributeValue.builder().s(therapistId).build(),
                "createdAt", AttributeValue.builder().s(createdAt).build(),
                "status", AttributeValue.builder().s("Pending").build()
            );

            // 5. Store in DynamoDB
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("JOURNAL_ACCESS_REQUESTS_TABLE"))
                .item(item)
                .build());

            // 6. Return success response
            return ResponseUtils.successResponse(200, Map.of(
                "message", "Journal access request created successfully",
                "requestId", journalAccessRequestId,
                "clientId", clientId,
                "therapistId", therapistId,
                "status", "Pending",
                "createdAt", createdAt
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error creating journal access request: " + e.getMessage());
        }
    }
}
