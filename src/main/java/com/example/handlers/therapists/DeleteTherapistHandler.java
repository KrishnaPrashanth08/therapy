package com.example.handlers.therapists;

import com.example.util.ResponseUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.util.Collections;
import java.util.Map;

public class DeleteTherapistHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            // Extract path parameters
            Map<String, String> pathParams = (Map<String, String>) input.get("pathParameters");
            String therapistId = pathParams.get("therapistId");

            // Validate therapistId
            if (therapistId == null || therapistId.isEmpty()) {
                return ResponseUtils.errorResponse(400, "Missing therapistId in path");
            }

            // Build DeleteItem request with conditional check
            DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(System.getenv("THERAPISTS_TABLE"))
                .key(Collections.singletonMap(
                    "TherapistId", 
                    AttributeValue.builder().s(therapistId).build()
                ))
                .conditionExpression("attribute_exists(TherapistId)")
                .build();

            // Execute delete operation
            dynamoDb.deleteItem(request);
            
            // Return 204 No Content
            return ResponseUtils.successResponse(204, "");

        } catch (DynamoDbException e) {
            context.getLogger().log("DynamoDB Error: " + e.awsErrorDetails().errorCode());
            
            if ("ConditionalCheckFailedException".equals(e.awsErrorDetails().errorCode())) {
                return ResponseUtils.errorResponse(404, "Therapist not found. Verify ID.");
            }
            return ResponseUtils.errorResponse(500, "Database operation failed");
        } catch (Exception e) {
            context.getLogger().log("Unexpected Error: " + e.toString());
            return ResponseUtils.errorResponse(500, "Internal server error");
        }
    }
}
