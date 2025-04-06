package com.example.handlers.therapists;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class UpdateTherapistHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId in path");
            }

            // 2. Parse request body
            String requestBody = (String) input.get("body");
            Map<String, Object> data = objectMapper.readValue(requestBody, Map.class);

            // 3. Prepare update components
            Map<String, String> attrNames = new HashMap<>();
            Map<String, AttributeValue> attrValues = new HashMap<>();
            List<String> updateExpressions = new ArrayList<>();

            data.forEach((key, value) -> {
                switch (key) {
                    case "email":
                    case "name":
                    case "location":
                    case "expertise":
                        attrNames.put("#" + key, key);
                        attrValues.put(":" + key, AttributeValue.builder().s(value.toString()).build());
                        updateExpressions.add("#" + key + " = :" + key);
                        break;
                    case "mappedClientsIds":
                        List<String> clients = (List<String>) value;
                        String clientsStr = String.join(",", clients);
                        attrNames.put("#mapped", "mappedClientsIds");
                        attrValues.put(":mapped", AttributeValue.builder().s(clientsStr).build());
                        updateExpressions.add("#mapped = :mapped");
                        break;
                }
            });

            if (updateExpressions.isEmpty()) {
                return ResponseUtils.errorResponse(400, "No valid fields provided for update");
            }

            // 4. Build DynamoDB request
            UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(System.getenv("THERAPISTS_TABLE"))
                .key(Collections.singletonMap(
                    "TherapistId", 
                    AttributeValue.builder().s(therapistId).build()
                ))
                .updateExpression("SET " + String.join(", ", updateExpressions))
                .expressionAttributeNames(attrNames)
                .expressionAttributeValues(attrValues)
                .conditionExpression("attribute_exists(TherapistId)")
                .returnValues(ReturnValue.ALL_NEW)
                .build();

            // 5. Execute update
            UpdateItemResponse response = dynamoDb.updateItem(request);
            Map<String, AttributeValue> updatedItem = response.attributes();

            // 6. Format response
            return ResponseUtils.successResponse(200, formatResponse(updatedItem));

        } catch (DynamoDbException e) {
            context.getLogger().log("DynamoDB Error: " + e.awsErrorDetails().errorCode());
            
            if ("ConditionalCheckFailedException".equals(e.awsErrorDetails().errorCode())) {
                return ResponseUtils.errorResponse(404, "Therapist not found. Verify ID.");
            } else {
                return ResponseUtils.errorResponse(500, "Database error: " + e.getMessage());
            }
        } catch (Exception e) {
            context.getLogger().log("General Error: " + e.getMessage());
            return ResponseUtils.errorResponse(500, "Internal server error.");
        }

    }

    private Map<String, Object> formatResponse(Map<String, AttributeValue> item) {
        Map<String, Object> response = new HashMap<>();
        response.put("therapistId", item.get("TherapistId").s());
        response.put("email", item.get("email").s());
        response.put("name", item.get("name").s());
        response.put("location", item.get("location").s());
        response.put("expertise", item.get("expertise").s());
        
        if (item.containsKey("mappedClientsIds")) {
            response.put("mappedClientsIds", item.get("mappedClientsIds").s().split(","));
        } else {
            response.put("mappedClientsIds", new String[0]);
        }
        return response;
    }
}
