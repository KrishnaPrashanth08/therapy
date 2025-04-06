package com.example.handlers.therapists;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

public class CreateTherapistHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // Validate request body exists
            if (!input.containsKey("body") || ((String) input.get("body")).isEmpty()) {
                return ResponseUtils.errorResponse(400, "Request body is missing or empty");
            }

            // Parse request body
            String requestBody = (String) input.get("body");
            Map<String, Object> data = objectMapper.readValue(requestBody, Map.class);

            // Validate required fields
            List<String> requiredFields = Arrays.asList("email", "name", "location", "expertise");
            for (String field : requiredFields) {
                Object value = data.get(field);
                if (value == null || value.toString().trim().isEmpty()) {
                    return ResponseUtils.errorResponse(400, 
                        String.format("Field '%s' is required and cannot be empty", field));
                }
            }

            // Create therapist record
            String therapistId = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("TherapistId", AttributeValue.builder().s(therapistId).build());
            item.put("email", AttributeValue.builder().s(data.get("email").toString()).build());
            item.put("name", AttributeValue.builder().s(data.get("name").toString()).build());
            item.put("location", AttributeValue.builder().s(data.get("location").toString()).build());
            item.put("expertise", AttributeValue.builder().s(data.get("expertise").toString()).build());
            item.put("mappedClientsIds", AttributeValue.builder().s("").build());

            // Save to DynamoDB
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("THERAPISTS_TABLE"))
                .item(item)
                .build());

            // Build success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("therapistId", therapistId);
            responseBody.put("email", data.get("email"));
            responseBody.put("name", data.get("name"));

            return ResponseUtils.successResponse(201, responseBody);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, 
                "Error creating therapist: " + e.getMessage());
        }
    }
}
