package com.example.handlers.sessions;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApproveAppointmentHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract parameters from path and body
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String appointmentRequestId = pathParams.get("appointmentRequestId");

            if (therapistId == null || therapistId.isEmpty() || appointmentRequestId == null || appointmentRequestId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing required path parameters: therapistId or appointmentRequestId");
            }

            Map<String, Object> requestBody = ResponseUtils.parseBody(input);
            String action = requestBody.get("action").toString();  // "approve" or "reject"
            String therapistNotes = requestBody.get("notes").toString();

            // 2. Get appointment request
            GetItemResponse appointmentRequest = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(System.getenv("APPOINTMENT_REQUESTS_TABLE"))
                .key(Map.of(
                    "TherapistId", AttributeValue.builder().s(therapistId).build(),
                    "AppointmentRequestId", AttributeValue.builder().s(appointmentRequestId).build()
                ))
                .build());

            if (!appointmentRequest.hasItem() || !"Pending".equals(appointmentRequest.item().get("status").s())) {
                return ResponseUtils.errorResponse(400, "Invalid or already processed request");
            }

            // 3. Update appointment status
            String newStatus = "approve".equalsIgnoreCase(action) ? "Approved" : "Rejected";
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(System.getenv("APPOINTMENT_REQUESTS_TABLE"))
                .key(Map.of(
                    "TherapistId", AttributeValue.builder().s(therapistId).build(),
                    "AppointmentRequestId", AttributeValue.builder().s(appointmentRequestId).build()
                ))
                .updateExpression("SET #status = :newStatus")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(
                    ":newStatus", AttributeValue.builder().s(newStatus).build()
                ))
                .build();

            dynamoDb.updateItem(updateRequest);

            // Declare sessionId outside the if block
            String sessionId = null;

            // 4. Create session if approved
            if ("approve".equalsIgnoreCase(action)) {
                sessionId = UUID.randomUUID().toString();
                
                Map<String, AttributeValue> sessionItem = new HashMap<>();
                sessionItem.put("TherapistId", AttributeValue.builder().s(therapistId).build());
                sessionItem.put("SessionId", AttributeValue.builder().s(sessionId).build());
                sessionItem.put("clientId", appointmentRequest.item().get("ClientId")); // Lower case 'c' to match schema
                sessionItem.put("date", appointmentRequest.item().get("Date")); 
                sessionItem.put("startTime", appointmentRequest.item().get("StartTime"));
                sessionItem.put("endTime", appointmentRequest.item().get("EndTime"));
                sessionItem.put("status", AttributeValue.builder().s("Scheduled").build());
                
                // Add sharedNotes and privateNotes explicitly
                sessionItem.put("sharedNotes", AttributeValue.builder().s("").build()); // Initialize empty
                sessionItem.put("privateNotes", AttributeValue.builder().s(therapistNotes).build()); // From request
                
                dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(System.getenv("SESSIONS_TABLE"))
                    .item(sessionItem)
                    .build());
            }

            return ResponseUtils.successResponse(200, Map.of(
                "appointmentRequestId", appointmentRequestId,
                "status", newStatus,
                "sessionId", sessionId != null ? sessionId : "N/A"
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error processing request: " + e.getMessage());
        }
    }
}
