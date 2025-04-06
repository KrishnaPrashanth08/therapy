package com.example.handlers.clients;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

public class UpdateClientHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);

            // 2. Validate input
            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }
            
            String email = (String) requestBody.get("email");
            String name = (String) requestBody.get("name");
            List<String> mappedTherapistsIds = (List<String>) requestBody.get("mappedTherapistsIds");
            
            if (email == null || name == null) {
                return ResponseUtils.errorResponse(400, "Email and name are required fields");
            }

            // 3. Build update expression
            Map<String, String> expressionNames = new HashMap<>();
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            StringBuilder updateExpression = new StringBuilder("SET ");
            
            expressionNames.put("#email", "email");
            expressionValues.put(":email", AttributeValue.builder().s(email).build());
            updateExpression.append("#email = :email, ");
            
            expressionNames.put("#name", "name");
            expressionValues.put(":name", AttributeValue.builder().s(name).build());
            updateExpression.append("#name = :name");
            
            // Handle mapped therapists
            if (mappedTherapistsIds != null) {
                expressionNames.put("#mappedTherapists", "mappedTherapistsIds");
                expressionValues.put(":therapists", 
                    AttributeValue.builder().ss(mappedTherapistsIds).build());
                updateExpression.append(", #mappedTherapists = :therapists");
            }

            // 4. Execute update
            UpdateItemResponse response = dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(System.getenv("CLIENTS_TABLE"))
                .key(Map.of("ClientId", AttributeValue.builder().s(clientId).build()))
                .updateExpression(updateExpression.toString())
                .expressionAttributeNames(expressionNames)
                .expressionAttributeValues(expressionValues)
                .returnValues(ReturnValue.ALL_NEW)
                .build());

            // 5. Format response
            return ResponseUtils.successResponse(200, Map.of(
                "clientId", response.attributes().get("ClientId").s(),
                "email", response.attributes().get("email").s(),
                "name", response.attributes().get("name").s(),
                "mappedTherapistsIds", response.attributes().containsKey("mappedTherapistsIds") ?
                    response.attributes().get("mappedTherapistsIds").ss() :
                    Collections.emptyList()
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error updating client: " + e.getMessage());
        }
    }
}
