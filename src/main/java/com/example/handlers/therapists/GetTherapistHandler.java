package com.example.handlers.therapists;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;
import java.util.Collections;

public class GetTherapistHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");

            // Validate therapistId
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId in path");
            }

            // Build DynamoDB request
            GetItemRequest request = GetItemRequest.builder()
                .tableName(System.getenv("THERAPISTS_TABLE"))
                .key(Collections.singletonMap("TherapistId", 
                    AttributeValue.builder().s(therapistId).build()))
                .build();

            // Execute query
            Map<String, AttributeValue> item = dynamoDb.getItem(request).item();

            if (item == null || item.isEmpty()) {
                return ResponseUtils.errorResponse(404, "Therapist not found");
            }

            // Format response according to Swagger schema
            return ResponseUtils.successResponse(200, Map.of(
                "therapistId", item.get("TherapistId").s(),
                "name", item.get("name").s(),
                "email", item.get("email").s(),
                "location", item.get("location").s(),
                "expertise", item.get("expertise").s(),
                "mappedClientsIds", item.containsKey("mappedClientsIds") ? 
                    item.get("mappedClientsIds").s().split(",") : new String[0]
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, 
                "Error retrieving therapist: " + e.getMessage());
        }
    }
}
