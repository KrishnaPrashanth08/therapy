package com.example.handlers.journals;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.Map;

public class ApproveJournalAccessHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");

            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            String therapistId = (String) requestBody.get("therapistId");
            String status = ((String) requestBody.get("status")).toLowerCase();

            // 2. Validate input
            if (clientId == null || therapistId == null || !status.matches("approved|rejected")) {
                return ResponseUtils.errorResponse(400, "Invalid request parameters");
            }

            // 3. Query the JournalAccessRequests table to find the access request
            QueryResponse queryResponse = dynamoDb.query(QueryRequest.builder()
                .tableName(System.getenv("JOURNAL_ACCESS_REQUESTS_TABLE"))
                .indexName("TherapistsRequestIndex")
                .keyConditionExpression("TherapistId = :therapistId")
                .filterExpression("ClientId = :clientId")
                .expressionAttributeValues(Map.of(
                    ":therapistId", AttributeValue.builder().s(therapistId).build(),
                    ":clientId", AttributeValue.builder().s(clientId).build()
                ))
                .build());

            if (queryResponse.items().isEmpty()) {
                return ResponseUtils.errorResponse(404, "Access request not found");
            }

            // 4. Update the status in JournalAccessRequests table
            Map<String, AttributeValue> item = queryResponse.items().get(0);
            String requestId = item.get("JournalAccessRequestId").s();

            dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(System.getenv("JOURNAL_ACCESS_REQUESTS_TABLE"))
                .key(Map.of(
                    "ClientId", AttributeValue.builder().s(clientId).build(),
                    "JournalAccessRequestId", AttributeValue.builder().s(requestId).build()
                ))
                .updateExpression("SET #status = :status")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(
                    ":status", AttributeValue.builder().s(status).build()
                ))
                .build());

            // 5. If approved, add to MappedTherapists table
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

            // 6. Return success response
            return ResponseUtils.successResponse(200, Map.of(
                "message", "Journal access request " + status,
                "clientId", clientId,
                "therapistId", therapistId,
                "requestId", requestId
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error processing request: " + e.getMessage());
        }
    }
}
