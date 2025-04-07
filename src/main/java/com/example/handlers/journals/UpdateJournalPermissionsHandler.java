package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class UpdateJournalPermissionsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");

            // 2. Validate input
            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }

            // 3. Parse request body
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            List<String> therapistIds = (List<String>) requestBody.get("therapists");

            if (therapistIds == null || therapistIds.isEmpty()) {
                return ResponseUtils.errorResponse(400, "At least one therapist ID is required");
            }

            // 4. Delete existing mappings
            deleteExistingMappings(clientId);

            // 5. Create new mappings
            createNewMappings(clientId, therapistIds);

            return ResponseUtils.successResponse(200, Map.of(
                "message", "Journal access permissions updated successfully",
                "clientId", clientId,
                "therapists", therapistIds
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error updating permissions: " + e.getMessage());
        }
    }

    private void deleteExistingMappings(String clientId) {
        // Query existing mappings
        QueryResponse queryResponse = dynamoDb.query(QueryRequest.builder()
            .tableName(System.getenv("MAPPED_THERAPISTS_TABLE"))
            .keyConditionExpression("ClientId = :clientId")
            .expressionAttributeValues(Map.of(
                ":clientId", AttributeValue.builder().s(clientId).build()
            ))
            .build());

        // Batch delete
        List<WriteRequest> deleteRequests = queryResponse.items().stream()
            .map(item -> WriteRequest.builder()
                .deleteRequest(DeleteRequest.builder()
                    .key(Map.of(
                        "ClientId", item.get("ClientId"),
                        "TherapistId", item.get("TherapistId")
                    ))
                    .build())
                .build())
            .collect(Collectors.toList());

        if (!deleteRequests.isEmpty()) {
            dynamoDb.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                    System.getenv("MAPPED_THERAPISTS_TABLE"), deleteRequests
                ))
                .build());
        }
    }

    private void createNewMappings(String clientId, List<String> therapistIds) {
        String mappedAt = Instant.now().toString();
        List<WriteRequest> putRequests = therapistIds.stream()
            .map(therapistId -> WriteRequest.builder()
                .putRequest(PutRequest.builder()
                    .item(Map.of(
                        "ClientId", AttributeValue.builder().s(clientId).build(),
                        "TherapistId", AttributeValue.builder().s(therapistId).build(),
                        "mappedAt", AttributeValue.builder().s(mappedAt).build()
                    ))
                    .build())
                .build())
            .collect(Collectors.toList());

        // Split into batches of 25 (DynamoDB limit)
        for (int i = 0; i < putRequests.size(); i += 25) {
            List<WriteRequest> batch = putRequests.subList(i, Math.min(i + 25, putRequests.size()));
            dynamoDb.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                    System.getenv("MAPPED_THERAPISTS_TABLE"), batch
                ))
                .build());
        }
    }
}
