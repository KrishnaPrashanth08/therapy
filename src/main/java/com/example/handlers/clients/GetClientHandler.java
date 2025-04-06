package com.example.handlers.clients;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GetClientHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");

            if (clientId == null || clientId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID is required");
            }

            Map<String, AttributeValue> item = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(System.getenv("CLIENTS_TABLE"))
                .key(Map.of("ClientId", AttributeValue.builder().s(clientId).build()))
                .build()).item();

            if (item == null || item.isEmpty()) {
                return ResponseUtils.errorResponse(404, "Client not found");
            }

            return ResponseUtils.successResponse(200, Map.of(
                "clientId", getStringValue(item, "ClientId"),
                "email", getStringValue(item, "email"),
                "name", getStringValue(item, "name"),
                "mappedTherapistsIds", getMappedTherapists(item)
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error retrieving client: " + e.getMessage());
        }
    }

    private String getStringValue(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : "";
    }

    private List<String> getMappedTherapists(Map<String, AttributeValue> item) {
        if (!item.containsKey("mappedTherapistsIds")) return Collections.emptyList();
        String ids = item.get("mappedTherapistsIds").s();
        return ids.isEmpty() ? Collections.emptyList() : List.of(ids.split(","));
    }
}
