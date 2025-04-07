package com.example.handlers.messages;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import java.util.Map;

public class GetMessageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String messageId = pathParams.get("messageId");

            var response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(System.getenv("MESSAGES_TABLE"))
                .key(Map.of("messageId", AttributeValue.builder().s(messageId).build()))
                .build());

            if (!response.hasItem()) {
                return ResponseUtils.errorResponse(404, "Message not found");
            }

            return ResponseUtils.successResponse(200, Map.of(
                "messageId", response.item().get("messageId").s(),
                "senderId", response.item().get("senderId").s(),
                "recipientId", response.item().get("recipientId").s(),
                "content", response.item().get("content").s(),
                "timestamp", response.item().get("timestamp").s(),
                "status", response.item().get("status").s()
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error retrieving message: " + e.getMessage());
        }
    }
}
