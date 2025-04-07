package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListJournalAccessRequestsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");

            // 2. Validate input
            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }

            // 3. Build query request
            QueryRequest queryRequest = QueryRequest.builder()
                .tableName(System.getenv("JOURNAL_ACCESS_REQUESTS_TABLE"))
                .keyConditionExpression("ClientId = :clientId")
                .filterExpression("#status = :pending")
                .expressionAttributeValues(Map.of(
                    ":clientId", AttributeValue.builder().s(clientId).build(),
                    ":pending", AttributeValue.builder().s("Pending").build()
                ))
                .expressionAttributeNames(Map.of("#status", "status"))
                .build();

            // 4. Execute query
            QueryResponse response = dynamoDb.query(queryRequest);

            // 5. Process results
            List<Map<String, String>> requests = response.items().stream()
                .map(item -> Map.of(
                    "requestId", item.get("JournalAccessRequestId").s(),
                    "therapistId", item.get("TherapistId").s(),
                    "clientId", item.get("ClientId").s(),
                    "status", item.get("status").s(),
                    "createdAt", item.get("createdAt").s()
                ))
                .collect(Collectors.toList());

            // 6. Return response
            return ResponseUtils.successResponse(200, requests);

        } catch (DynamoDbException e) {
            return ResponseUtils.errorResponse(500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Server error: " + e.getMessage());
        }
    }
}
