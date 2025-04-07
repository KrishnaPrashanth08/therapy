package com.example.handlers.search;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class TherapistSearchHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    
   
    
    private final String clientsTable = System.getenv("CLIENTS_TABLE");
    private final String therapistsTable = System.getenv("THERAPISTS_TABLE");
    private final String journalEntriesTable = System.getenv("JOURNAL_ENTRIES_TABLE");
    private final String sessionsTable = System.getenv("SESSIONS_TABLE");
    private final String mappedTherapistsTable = System.getenv("MAPPED_THERAPISTS_TABLE");
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String query = ((Map<String, String>) input.get("queryStringParameters")).get("query").toLowerCase();

            // 1. Search Clients
            List<Map<String, String>> clients = searchClients(query);

            // 2. Search Session Notes
            List<Map<String, String>> notes = searchSessionNotes(therapistId, query);

            // 3. Search Journals (only for mapped clients)
            List<Map<String, String>> journals = searchJournals(therapistId, query);

            return ResponseUtils.successResponse(200, Map.of(
                "clients", clients,
                "notes", notes,
                "journals", journals
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Search failed: " + e.getMessage());
        }
    }
    
    private Map<String, String> convertNote(Map<String, AttributeValue> item) {
        return Map.of(
            "sessionId", item.get("SessionId").s(),
            "date", item.get("date").s(),
            "sharedNotes", item.get("sharedNotes") != null ? item.get("sharedNotes").s() : "",
            "privateNotes", item.get("privateNotes") != null ? item.get("privateNotes").s() : ""
        );
    }
    private Map<String, Object> convertJournal(Map<String, AttributeValue> item) {
        return Map.of(
            "journalEntryId", item.get("JournalEntryId").s(),
            "clientId", item.get("ClientId").s(),
            "content", item.get("content").s(),
            "timestamp", item.get("timestamp").s(),
            "feeling", item.get("feeling") != null ? item.get("feeling").s() : "",
            "intensity", item.get("intensity") != null ? item.get("intensity").n() : "0"
        );
    }

    private List<Map<String, String>> searchClients(String query) {
        ScanResponse response = dynamoDb.scan(ScanRequest.builder()
            .tableName(clientsTable)
            .filterExpression("contains(#n, :query) OR contains(#e, :query)")
            .expressionAttributeNames(Map.of(
                "#n", "name",
                "#e", "email"
            ))
            .expressionAttributeValues(Map.of(":query", AttributeValue.builder().s(query).build()))
            .build());
        return response.items().stream()
            .map(this::convertClient)
            .collect(Collectors.toList());
    }

    private List<Map<String, String>> searchSessionNotes(String therapistId, String query) {
        QueryResponse response = dynamoDb.query(QueryRequest.builder()
            .tableName(sessionsTable)
            .keyConditionExpression("TherapistId = :tid")
            .filterExpression("contains(sharedNotes, :query) OR contains(privateNotes, :query)")
            .expressionAttributeValues(Map.of(
                ":tid", AttributeValue.builder().s(therapistId).build(),
                ":query", AttributeValue.builder().s(query).build()
            ))
            .build());

        return response.items().stream()
            .map(this::convertNote)
            .collect(Collectors.toList());
    }

    private List<Map<String, String>> searchJournals(String therapistId, String query) {
        // Get mapped clients
        QueryResponse mappingResponse = dynamoDb.query(QueryRequest.builder()
            .tableName(mappedTherapistsTable)
            .indexName("TherapistClientIndex")
            .keyConditionExpression("TherapistId = :tid")
            .expressionAttributeValues(Map.of(":tid", AttributeValue.builder().s(therapistId).build()))
            .build());

        List<String> clientIds = mappingResponse.items().stream()
            .map(item -> item.get("ClientId").s())
            .collect(Collectors.toList());

        if (clientIds.isEmpty()) return Collections.emptyList();

        // Batch get journal entries
        List<Map<String, String>> journals = new ArrayList<>();
        for (String clientId : clientIds) {
            QueryResponse journalResponse = dynamoDb.query(QueryRequest.builder()
                .tableName(journalEntriesTable)
                .keyConditionExpression("ClientId = :cid")
                .filterExpression("contains(content, :query)")
                .expressionAttributeValues(Map.of(
                    ":cid", AttributeValue.builder().s(clientId).build(),
                    ":query", AttributeValue.builder().s(query).build()
                ))
                .build());
            
            journals.addAll(journalResponse.items().stream()
            	    .map(item -> {
            	        Map<String, String> journalMap = new HashMap<>();
            	        journalMap.put("journalEntryId", item.get("JournalEntryId").s());
            	        journalMap.put("content", item.get("content").s());
            	        journalMap.put("timestamp", item.get("timestamp").s());
            	        // Convert numbers to strings if needed
            	        if (item.get("intensity") != null) {
            	            journalMap.put("intensity", item.get("intensity").n());
            	        }
            	        return journalMap;
            	    })
            	    .collect(Collectors.toList()));
        }

        return journals;
    }

    // Conversion helpers
    private Map<String, String> convertClient(Map<String, AttributeValue> item) {
        return Map.of(
            "clientId", item.get("ClientId").s(),
            "name", item.get("name").s(),
            "email", item.get("email").s()
        );
    }
}
