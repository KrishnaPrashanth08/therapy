package com.example.handlers.clients;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListClientsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Scan the ClientsTable
            ScanResponse scanResponse = dynamoDb.scan(ScanRequest.builder()
                .tableName(System.getenv("CLIENTS_TABLE"))
                .build());

            // 2. Map DynamoDB items to client objects
            List<Map<String, String>> clients = scanResponse.items().stream()
                .map(this::convertClient)
                .collect(Collectors.toList());

            // 3. Return standardized response
            return ResponseUtils.successResponse(200, clients);

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Failed to list clients: " + e.getMessage());
        }
    }

    private Map<String, String> convertClient(Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item) {
        return Map.of(
            "clientId", getStringValue(item, "ClientId"),
            "email", getStringValue(item, "email"),
            "name", getStringValue(item, "name"),
            "mappedTherapistsIds", getStringValue(item, "mappedTherapistsIds")
        );
    }

    private String getStringValue(Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : "";
    }
}
