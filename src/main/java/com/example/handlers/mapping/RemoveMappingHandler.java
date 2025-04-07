package com.example.handlers.mapping;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;

public class RemoveMappingHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            String therapistId = pathParams.get("therapistId");

            // 2. Validate input
            if (clientId == null || therapistId == null) {
                return ResponseUtils.errorResponse(400, "Client ID and Therapist ID are required");
            }

            // 3. Delete from MappedTherapists
            dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(System.getenv("MAPPED_THERAPISTS_TABLE"))
                .key(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "TherapistId", AttributeValue.builder().s(therapistId).build()
                ))
                .build());

            return ResponseUtils.noContentResponse();

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error removing mapping: " + e.getMessage());
        }
    }
}
