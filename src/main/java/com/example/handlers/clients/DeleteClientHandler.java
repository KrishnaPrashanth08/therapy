package com.example.handlers.clients;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;

public class DeleteClientHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");

            // 2. Validate clientId
            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }

            // 3. Prepare and execute delete operation
            dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(System.getenv("CLIENTS_TABLE"))
                .key(Map.of("ClientId", AttributeValue.builder().s(clientId).build()))
                .build());

            // 4. Return success response (204 No Content)
            return ResponseUtils.noContentResponse();

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error deleting client: " + e.getMessage());
        }
    }
}
