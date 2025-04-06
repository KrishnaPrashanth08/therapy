package com.example.handlers.messages;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class ListMessagesHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> queryParams = (Map<String, String>) input.get("queryStringParameters");
            String senderId = queryParams.get("senderId");
            String recipientId = queryParams.get("recipientId");

            QueryRequest queryRequest = buildQueryRequest(senderId, recipientId);
            QueryResponse response = dynamoDb.query(queryRequest);

            List<Map<String, Object>> messages = response.items().stream()
                .map(this::convertItemToMessageMap)
                .collect(Collectors.toList());

            return ResponseUtils.successResponse(200, messages);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error retrieving messages: " + e.getMessage());
        }
    }

    private QueryRequest buildQueryRequest(String senderId, String recipientId) {
        if (senderId != null && recipientId != null) {
            return QueryRequest.builder()
                .tableName(System.getenv("MESSAGES_TABLE"))
                .indexName("ConversationIndex")
                .keyConditionExpression("senderId = :senderId AND recipientId = :recipientId")
                .expressionAttributeValues(Map.of(
                    ":senderId", AttributeValue.builder().s(senderId).build(),
                    ":recipientId", AttributeValue.builder().s(recipientId).build()
                ))
                .build();
        } else if (senderId != null) {
            return QueryRequest.builder()
                .tableName(System.getenv("MESSAGES_TABLE"))
                .indexName("SenderIndex")
                .keyConditionExpression("senderId = :senderId")
                .expressionAttributeValues(Map.of(
                    ":senderId", AttributeValue.builder().s(senderId).build()
                ))
                .build();
        } else if (recipientId != null) {
            return QueryRequest.builder()
                .tableName(System.getenv("MESSAGES_TABLE"))
                .indexName("RecipientIndex")
                .keyConditionExpression("recipientId = :recipientId")
                .expressionAttributeValues(Map.of(
                    ":recipientId", AttributeValue.builder().s(recipientId).build()
                ))
                .build();
        } else {
            throw new IllegalArgumentException("Must provide senderId or recipientId");
        }
    }

    private Map<String, Object> convertItemToMessageMap(Map<String, AttributeValue> item) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", item.get("messageId").s());
        messageMap.put("senderId", item.get("senderId").s());
        messageMap.put("recipientId", item.get("recipientId").s());
        messageMap.put("content", item.get("content").s());
        messageMap.put("timestamp", item.get("timestamp").s());
        messageMap.put("status", item.get("status").s());
        return messageMap;
    }
}
