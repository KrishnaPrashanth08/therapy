package com.example.handlers.messages;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import java.util.Map;

public class DeleteMessageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String messageId = pathParams.get("messageId");

            dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(System.getenv("MESSAGES_TABLE"))
                .key(Map.of("messageId", AttributeValue.builder().s(messageId).build()))
                .build());

            return ResponseUtils.noContentResponse();

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error deleting message: " + e.getMessage());
        }
    }
}
