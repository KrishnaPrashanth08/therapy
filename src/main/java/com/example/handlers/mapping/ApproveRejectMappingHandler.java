package com.example.handlers.mapping;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;

public class ApproveRejectMappingHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            String therapistId = pathParams.get("therapistId");

            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            String status = ((String) requestBody.get("status")).toLowerCase();

            // 2. Validate input
            if (clientId == null || therapistId == null || !status.matches("approved|rejected")) {
                return ResponseUtils.errorResponse(400, "Invalid request parameters");
            }

            // 3. Update mapping request status
            dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(System.getenv("MAPPING_REQUESTS_TABLE"))
                .key(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "mappingRequestId", AttributeValue.builder().s("REQUEST_ID_PLACEHOLDER").build()
                ))
                .updateExpression("SET #status = :status")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(
                    ":status", AttributeValue.builder().s(status).build()
                ))
                .build());

            // 4. If approved, add to MappedTherapists
            if (status.equals("approved")) {
                dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(System.getenv("MAPPED_THERAPISTS_TABLE"))
                    .item(Map.of(
                        "ClientId", AttributeValue.builder().s(clientId).build(),
                        "TherapistId", AttributeValue.builder().s(therapistId).build(),
                        "mappedAt", AttributeValue.builder().s(Instant.now().toString()).build()
                    ))
                    .build());
            }

            return ResponseUtils.successResponse(200, Map.of(
                "message", "Mapping request " + status,
                "clientId", clientId,
                "therapistId", therapistId
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error processing mapping request: " + e.getMessage());
        }
    }
}
