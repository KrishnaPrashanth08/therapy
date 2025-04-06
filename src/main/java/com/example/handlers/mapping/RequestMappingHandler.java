package com.example.handlers.mapping;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.*;

public class RequestMappingHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            String clientId = (String) requestBody.get("clientId");

            // 2. Validate input
            if (therapistId == null || clientId == null) {
                return ResponseUtils.errorResponse(400, "Therapist ID and Client ID are required");
            }

            // 3. Create mapping request
            String mappingRequestId = UUID.randomUUID().toString();
            String createdAt = Instant.now().toString();

            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("MAPPING_REQUESTS_TABLE"))
                .item(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "mappingRequestId", AttributeValue.builder().s(mappingRequestId).build(),
                    "therapistId", AttributeValue.builder().s(therapistId).build(),
                    "createdAt", AttributeValue.builder().s(createdAt).build(),
                    "status", AttributeValue.builder().s("pending").build()
                ))
                .build());

            return ResponseUtils.successResponse(200, Map.of(
                "message", "Mapping request created successfully",
                "mappingRequestId", mappingRequestId,
                "clientId", clientId,
                "therapistId", therapistId
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error creating mapping request: " + e.getMessage());
        }
    }
}
