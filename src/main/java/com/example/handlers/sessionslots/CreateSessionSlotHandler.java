package com.example.handlers.sessionslots;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.UUID;

public class CreateSessionSlotHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // Validate therapistId
            String therapistId = ((Map<String,String>) input.get("pathParameters")).get("therapistId");
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId");
            }

            // Parse request body
            Map<String, Object> body = ResponseUtils.MAPPER.readValue((String) input.get("body"), Map.class);
            
            // Validate required fields
            if (!body.containsKey("date") || !body.containsKey("startTime") 
                || !body.containsKey("endTime") || !body.containsKey("status")) {
                return ResponseUtils.errorResponse(400, "Missing required fields");
            }

            // Create session slot
            String slotId = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = Map.of(
                "TherapistId", AttributeValue.builder().s(therapistId).build(),
                "SlotId", AttributeValue.builder().s(slotId).build(),
                "date", AttributeValue.builder().s(body.get("date").toString()).build(),
                "startTime", AttributeValue.builder().s(body.get("startTime").toString()).build(),
                "endTime", AttributeValue.builder().s(body.get("endTime").toString()).build(),
                "status", AttributeValue.builder().s(body.get("status").toString()).build()
            );

            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("SESSION_SLOTS_TABLE"))
                .item(item)
                .build());

            return ResponseUtils.successResponse(201, Map.of(
                "slotId", slotId,
                "therapistId", therapistId,
                "date", body.get("date"),
                "startTime", body.get("startTime"),
                "endTime", body.get("endTime"),
                "status", body.get("status")
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error creating slot: " + e.getMessage());
        }
    }
}
