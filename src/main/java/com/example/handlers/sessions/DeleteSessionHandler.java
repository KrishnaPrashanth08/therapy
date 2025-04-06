package com.example.handlers.sessions;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class DeleteSessionHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String sessionId = pathParams.get("sessionId");

            if (therapistId == null || therapistId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId or sessionId in path");
            }

            // 2. Delete session
            dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(System.getenv("SESSIONS_TABLE"))
                .key(Map.of(
                    "TherapistId", AttributeValue.builder().s(therapistId).build(),
                    "SessionId", AttributeValue.builder().s(sessionId).build()
                ))
                .build());

            // 3. Return success message
            return ResponseUtils.successResponse(200, Map.of(
                "message", "Session deleted successfully",
                "therapistId", therapistId,
                "sessionId", sessionId
            ));

        } catch (DynamoDbException e) {
            return ResponseUtils.errorResponse(500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Server error: " + e.getMessage());
        }
    }
}
