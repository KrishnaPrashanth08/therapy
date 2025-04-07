package com.example.handlers.clients;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.*;

public class CreateClientHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Parse request body
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            
            // 2. Extract and validate parameters
            String email = (String) requestBody.get("email");
            String name = (String) requestBody.get("name");
            List<String> mappedTherapistsIds = (List<String>) requestBody.get("mappedTherapistsIds");
            
            if (email == null || email.isEmpty() || name == null || name.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Email and name are required fields");
            }

            // 3. Generate client ID
            String clientId = UUID.randomUUID().toString();

            // 4. Prepare DynamoDB item (using String Set for mappedTherapistsIds)
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("ClientId", AttributeValue.builder().s(clientId).build());
            item.put("email", AttributeValue.builder().s(email).build());
            item.put("name", AttributeValue.builder().s(name).build());
            
            if (mappedTherapistsIds != null && !mappedTherapistsIds.isEmpty()) {
                item.put("mappedTherapistsIds", AttributeValue.builder().ss(mappedTherapistsIds).build());
            }

            // 5. Save to DynamoDB
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("CLIENTS_TABLE"))
                .item(item)
                .build());

            // 6. Return standardized response
            return ResponseUtils.successResponse(201, Map.of(
                "clientId", clientId,
                "message", "Client created successfully"
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Client creation failed: " + e.getMessage());
        }
    }
}
