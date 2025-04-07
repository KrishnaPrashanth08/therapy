package com.example.handlers.appointments;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListAppointmentsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract and validate therapistId
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId in path");
            }

            // 2. Build query request for AppointmentRequestsTable
            QueryRequest request = QueryRequest.builder()
                .tableName(System.getenv("APPOINTMENT_REQUESTS_TABLE"))
                .keyConditionExpression("TherapistId = :therapistIdVal")
                .expressionAttributeValues(Map.of(
                    ":therapistIdVal", AttributeValue.builder().s(therapistId).build()
                ))
                .build();

            QueryResponse response = dynamoDb.query(request);

            // 3. Format response with appointment request details
            List<Map<String, String>> appointments = response.items().stream()
                .map(item -> Map.of(
                    "appointmentRequestId", item.get("AppointmentRequestId").s(),
                    "clientId", item.get("ClientId").s(),
                    "slotId", item.get("SlotId").s(),
                    "requestStatus", item.get("status").s(),
                    "createdAt", item.get("CreatedAt").s(),
                    "date", item.get("Date").s(),
                    "startTime", item.get("StartTime").s(),
                    "endTime", item.get("EndTime").s()
                ))
                .collect(Collectors.toList());

            return ResponseUtils.successResponse(200, appointments);

        } catch (DynamoDbException e) {
            return ResponseUtils.errorResponse(500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Server error: " + e.getMessage());
        }
    }
}
