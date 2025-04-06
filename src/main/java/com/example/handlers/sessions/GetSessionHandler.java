package com.example.handlers.sessions;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class GetSessionHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String sessionId = pathParams.get("sessionId");

            if (therapistId == null || therapistId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId or sessionId in path");
            }

            GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(System.getenv("SESSIONS_TABLE"))
                .key(Map.of(
                    "TherapistId", AttributeValue.builder().s(therapistId).build(),
                    "SessionId", AttributeValue.builder().s(sessionId).build()
                ))
                .build());

            if (!response.hasItem()) {
                return ResponseUtils.errorResponse(404, "Session not found");
            }

            return ResponseUtils.successResponse(200, Map.of(
                "therapistId", response.item().get("TherapistId").s(),
                "sessionId", response.item().get("SessionId").s(),
                "clientId", response.item().get("clientId").s(),
                "date", response.item().get("date").s(),
                "startTime", response.item().get("startTime").s(),
                "endTime", response.item().get("endTime").s(),
                "status", response.item().get("status").s(),
                "sharedNotes", response.item().get("sharedNotes").s(),
                "privateNotes", response.item().get("privateNotes").s()
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error retrieving session: " + e.getMessage());
        }
    }
}
