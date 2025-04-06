package com.example.handlers.messages;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.Map;

public class UpdateMessageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String messageId = pathParams.get("messageId");
            
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            String newContent = (String) requestBody.get("content");

            UpdateItemResponse response = dynamoDb.updateItem(UpdateItemRequest.builder()
                    .tableName(System.getenv("MESSAGES_TABLE"))
                    .key(Map.of("messageId", AttributeValue.builder().s(messageId).build()))
                    .updateExpression("SET content = :content, #ts = :timestamp") // Use alias
                    .expressionAttributeNames(Map.of("#ts", "timestamp")) // Define alias
                    .expressionAttributeValues(Map.of(
                        ":content", AttributeValue.builder().s(newContent).build(),
                        ":timestamp", AttributeValue.builder().s(Instant.now().toString()).build()
                    ))
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());

                return ResponseUtils.successResponse(200, Map.of(
                    "messageId", messageId,
                    "content", response.attributes().get("content").s(),
                    "timestamp", response.attributes().get("timestamp").s()
                ));

            } catch (Exception e) {
                return ResponseUtils.errorResponse(500, "Error updating message: " + e.getMessage());
    }
    }
}
        
