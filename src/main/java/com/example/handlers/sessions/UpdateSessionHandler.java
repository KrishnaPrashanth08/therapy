package com.example.handlers.sessions;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

public class UpdateSessionHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String sessionId = pathParams.get("sessionId");

            if (therapistId == null || therapistId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId or sessionId in path");
            }

            // 2. Parse request body
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            
            // 3. Build update expression
            Map<String, String> attributeNames = new HashMap<>();
            Map<String, AttributeValue> attributeValues = new HashMap<>();
            StringBuilder updateExpression = new StringBuilder("SET ");

            int counter = 1;
            for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
                String attribute = entry.getKey();
                String placeholder = ":val" + counter;
                
                attributeNames.put("#" + attribute, attribute);
                attributeValues.put(placeholder, AttributeValue.builder().s(entry.getValue().toString()).build());
                
                updateExpression.append("#").append(attribute).append(" = ").append(placeholder);
                if (counter < requestBody.size()) updateExpression.append(", ");
                counter++;
            }

            // 4. Perform update
            UpdateItemResponse response = dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(System.getenv("SESSIONS_TABLE"))
                .key(Map.of(
                    "TherapistId", AttributeValue.builder().s(therapistId).build(),
                    "SessionId", AttributeValue.builder().s(sessionId).build()
                ))
                .updateExpression(updateExpression.toString())
                .expressionAttributeNames(attributeNames)
                .expressionAttributeValues(attributeValues)
                .returnValues(ReturnValue.ALL_NEW)
                .build());

            // 5. Return updated session
            Map<String, String> session = Map.of(
                "therapistId", response.attributes().get("TherapistId").s(),
                "sessionId", response.attributes().get("SessionId").s(),
                "clientId", response.attributes().get("clientId").s(),
                "date", response.attributes().get("date").s(),
                "startTime", response.attributes().get("startTime").s(),
                "endTime", response.attributes().get("endTime").s(),
                "status", response.attributes().get("status").s(),
                "sharedNotes", response.attributes().get("sharedNotes").s(),
                "privateNotes", response.attributes().get("privateNotes").s()
            );

            return ResponseUtils.successResponse(200, session);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error updating session: " + e.getMessage());
        }
    }
}
