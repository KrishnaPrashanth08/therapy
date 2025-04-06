package com.example.handlers.appointments;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class RequestAppointmentHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // 1. Extract and validate parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId in path");
            }

            Map<String, Object> data = ResponseUtils.parseBody(input);
            String clientId = data.get("clientId").toString();
            String slotId = data.get("slotId").toString();

            // 2. Verify slot availability
            GetItemResponse slot = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(System.getenv("SESSION_SLOTS_TABLE"))
                .key(Map.of(
                    "TherapistId", AttributeValue.builder().s(therapistId).build(),
                    "SlotId", AttributeValue.builder().s(slotId).build()
                ))
                .build());

            if (!slot.hasItem() || !"Available".equals(slot.item().get("status").s())) {
                return ResponseUtils.errorResponse(400, "Slot not available for booking");
            }

            // 3. Create appointment request
            String appointmentRequestId = UUID.randomUUID().toString();
            Map<String, AttributeValue> requestItem = Map.of(
                "TherapistId", AttributeValue.builder().s(therapistId).build(),
                "AppointmentRequestId", AttributeValue.builder().s(appointmentRequestId).build(),
                "ClientId", AttributeValue.builder().s(clientId).build(),
                "SlotId", AttributeValue.builder().s(slotId).build(),
                "status", AttributeValue.builder().s("Pending").build(), // Initial status
                "CreatedAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                "Date", slot.item().get("date"),  // Copy from slot
                "StartTime", slot.item().get("startTime"),  // Copy from slot
                "EndTime", slot.item().get("endTime")  // Copy from slot
            );

            // 4. Save to AppointmentRequestsTable
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(System.getenv("APPOINTMENT_REQUESTS_TABLE"))
                .item(requestItem)
                .build());

            // 5. Return response without modifying session slot
            return ResponseUtils.successResponse(201, Map.of(
                "appointmentRequestId", appointmentRequestId,
                "therapistId", therapistId,
                "clientId", clientId,
                "slotId", slotId,
                "date", slot.item().get("date").s(),
                "startTime", slot.item().get("startTime").s(),
                "endTime", slot.item().get("endTime").s(),
                "status", "Pending"
            ));

        } catch (Exception e) {
            return ResponseUtils.errorResponse(500, "Error creating appointment request: " + e.getMessage());
        }
    }
}
