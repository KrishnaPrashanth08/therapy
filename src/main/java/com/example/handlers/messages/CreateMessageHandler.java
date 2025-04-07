package com.example.handlers.messages;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class CreateMessageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            
            String messageId = UUID.randomUUID().toString();
            String timestamp = Instant.now().toString();
            
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("MESSAGES_TABLE"))
                .item(Map.of(
                    "messageId", AttributeValue.builder().s(messageId).build(),
                    "senderId", AttributeValue.builder().s((String) requestBody.get("senderId")).build(),
                    "recipientId", AttributeValue.builder().s((String) requestBody.get("recipientId")).build(),
                    "content", AttributeValue.builder().s((String) requestBody.get("content")).build(),
                    "timestamp", AttributeValue.builder().s(timestamp).build(),
                    "status", AttributeValue.builder().s("sent").build()
                ))
                .build());

            return ResponseUtils.successResponse(200, Map.of(
                "messageId", messageId,
                "status", "sent",
                "timestamp", timestamp
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error sending message: " + e.getMessage());
        }
    }
}
