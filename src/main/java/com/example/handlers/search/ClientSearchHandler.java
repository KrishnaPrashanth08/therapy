package com.example.handlers.search;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientSearchHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    
    private final String therapistsTable = System.getenv("THERAPISTS_TABLE");
    private final String journalEntriesTable = System.getenv("JOURNAL_ENTRIES_TABLE");
    private final String sessionsTable = System.getenv("SESSIONS_TABLE");
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String clientId = pathParams.get("clientId");
            
            Map<String, String> queryParams = (Map<String, String>) input.get("queryStringParameters");
            String query = queryParams != null ? queryParams.get("query").toLowerCase() : "";

            // Validate input
            if (clientId == null || clientId.isEmpty() || query.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Client ID and query parameters are required");
            }

            // Execute searches
            List<Map<String, String>> therapists = searchTherapists(query);
            List<Map<String, String>> journals = searchJournals(clientId, query);
            List<Map<String, String>> notes = searchSessionNotes(clientId, query);

            return ResponseUtils.successResponse(200, Map.of(
                "therapists", therapists,
                "journals", journals,
                "notes", notes
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Search failed: " + e.getMessage());
        }
    }

    private List<Map<String, String>> searchTherapists(String query) {
        try {
            // Search location index
            QueryResponse locationResponse = dynamoDb.query(QueryRequest.builder()
                .tableName(therapistsTable)
                .indexName("locationIndex")
                .keyConditionExpression("#loc = :loc")
                .expressionAttributeNames(Map.of("#loc", "location"))
                .expressionAttributeValues(Map.of(":loc", AttributeValue.builder().s(query).build()))
                .build());

            // Search expertise index
            QueryResponse expertiseResponse = dynamoDb.query(QueryRequest.builder()
                .tableName(therapistsTable)
                .indexName("ExpertiseIndex")
                .keyConditionExpression("#exp = :exp")
                .expressionAttributeNames(Map.of("#exp", "expertise"))
                .expressionAttributeValues(Map.of(":exp", AttributeValue.builder().s(query).build()))
                .build());

            return Stream.concat(
                locationResponse.items().stream(),
                expertiseResponse.items().stream()
            ).map(this::convertTherapist).collect(Collectors.toList());
            
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error searching therapists: " + e.getMessage());
        }
    }

    private List<Map<String, String>> searchJournals(String clientId, String query) {
        try {
            QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(journalEntriesTable)
                .keyConditionExpression("ClientId = :cid")
                .filterExpression("contains(content, :query) OR contains(feeling, :query)")
                .expressionAttributeValues(Map.of(
                    ":cid", AttributeValue.builder().s(clientId).build(),
                    ":query", AttributeValue.builder().s(query).build()
                ))
                .build());

            return response.items().stream()
                .map(this::convertJournal)
                .collect(Collectors.toList());
            
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error searching journals: " + e.getMessage());
        }
    }

    private List<Map<String, String>> searchSessionNotes(String clientId, String query) {
        try {
            QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(sessionsTable)
                .indexName("ClientSessionsIndex")
                .keyConditionExpression("clientId = :cid")
                .filterExpression("contains(sharedNotes, :query) OR contains(privateNotes, :query)")
                .expressionAttributeValues(Map.of(
                    ":cid", AttributeValue.builder().s(clientId).build(),
                    ":query", AttributeValue.builder().s(query).build()
                ))
                .build());

            return response.items().stream()
                .map(this::convertNote)
                .collect(Collectors.toList());
            
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error searching notes: " + e.getMessage());
        }
    }

    // Conversion helpers
    private Map<String, String> convertTherapist(Map<String, AttributeValue> item) {
        return Map.of(
            "therapistId", getStringValue(item, "TherapistId"),
            "name", getStringValue(item, "name"),
            "location", getStringValue(item, "location"),
            "expertise", getStringValue(item, "expertise")
        );
    }

    private Map<String, String> convertJournal(Map<String, AttributeValue> item) {
        return Map.of(
            "journalEntryId", getStringValue(item, "JournalEntryId"),
            "clientId", getStringValue(item, "ClientId"),
            "content", getStringValue(item, "content"),
            "timestamp", getStringValue(item, "timestamp"),
            "feeling", getStringValue(item, "feeling"),
            "intensity", getNumberValue(item, "intensity")
        );
    }

    private Map<String, String> convertNote(Map<String, AttributeValue> item) {
        return Map.of(
            "sessionId", getStringValue(item, "SessionId"),
            "date", getStringValue(item, "date"),
            "sharedNotes", getStringValue(item, "sharedNotes"),
            "privateNotes", getStringValue(item, "privateNotes")
        );
    }

    // Helper methods for safe value extraction
    private String getStringValue(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : "";
    }

    private String getNumberValue(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).n() : "0";
    }
}
