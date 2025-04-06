package com.example.handlers.therapists;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ListTherapistsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> queryParams = (Map<String, String>) input.get("queryStringParameters");
            String location = queryParams != null ? queryParams.get("location") : null;
            String expertise = queryParams != null ? queryParams.get("expertise") : null;

            if (location != null && !location.isEmpty()) {
                return handleLocationQuery(location, expertise);
            } else {
                return handleScan(expertise);
            }
        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, 
                "Error fetching therapists: " + e.getMessage());
        }
    }

    private Map<String, Object> handleLocationQuery(String location, String expertise) {
        Map<String, AttributeValue> attrValues = new HashMap<>();
        Map<String, String> attrNames = new HashMap<>();
        
        // Use expression attribute names for reserved keywords
        attrNames.put("#loc", "location");
        attrValues.put(":loc", AttributeValue.builder().s(location).build());
        
        String keyCondition = "#loc = :loc";
        if (expertise != null && !expertise.isEmpty()) {
            attrNames.put("#exp", "expertise");
            attrValues.put(":exp", AttributeValue.builder().s(expertise).build());
            keyCondition += " AND #exp = :exp";
        }

        QueryRequest request = QueryRequest.builder()
            .tableName(System.getenv("THERAPISTS_TABLE"))
            .indexName("locationExpertiseIndex")
            .keyConditionExpression(keyCondition)
            .expressionAttributeNames(attrNames)
            .expressionAttributeValues(attrValues)
            .build();

        QueryResponse response = dynamoDb.query(request);
        return formatResponse(response.items());
    }

    private Map<String, Object> handleScan(String expertise) {
        ScanRequest.Builder scanBuilder = ScanRequest.builder()
            .tableName(System.getenv("THERAPISTS_TABLE"));

        if (expertise != null && !expertise.isEmpty()) {
            scanBuilder
                .filterExpression("#exp = :exp")
                .expressionAttributeNames(Map.of("#exp", "expertise"))
                .expressionAttributeValues(
                    Map.of(":exp", AttributeValue.builder().s(expertise).build())
                );
        }

        ScanResponse response = dynamoDb.scan(scanBuilder.build());
        return formatResponse(response.items());
    }

    private Map<String, Object> formatResponse(List<Map<String, AttributeValue>> items) {
        List<Map<String, String>> therapists = items.stream()
            .map(item -> Map.of(
                "therapistId", item.get("TherapistId").s(),
                "name", item.get("name").s(),
                "email", item.get("email").s(),
                "location", item.get("location").s(),
                "expertise", item.get("expertise").s()
            ))
            .collect(Collectors.toList());

        return ResponseUtils.successResponse(200, therapists);
    }
}
