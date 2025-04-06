package com.example.handlers.sessionslots;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListSessionSlotsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract and validate therapistId
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId in path");
            }

            // 2. Build query request with new schema attributes
            QueryRequest request = QueryRequest.builder()
                .tableName(System.getenv("SESSION_SLOTS_TABLE"))
                .keyConditionExpression("TherapistId = :therapistId")
                .filterExpression("#status = :status")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(
                    new HashMap<String, AttributeValue>() {{
                        put(":therapistId", AttributeValue.builder().s(therapistId).build());
                        put(":status", AttributeValue.builder().s("Available").build());
                    }}
                )
                .build();

            // 3. Execute query
            QueryResponse response = dynamoDb.query(request);

            // 4. Format response with new attributes
            List<Map<String, String>> slots = response.items().stream()
                .map(item -> Map.of(
                    "slotId", item.get("SlotId").s(),
                    "therapistId", item.get("TherapistId").s(),
                    "date", item.get("date").s(),
                    "startTime", item.get("startTime").s(),
                    "endTime", item.get("endTime").s(),
                    "status", item.get("status").s()
                ))
                .collect(Collectors.toList());

            return ResponseUtils.successResponse(200, slots);

        } catch (DynamoDbException e) {
            return ResponseUtils.errorResponse(500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Server error: " + e.getMessage());
        }
    }
}
