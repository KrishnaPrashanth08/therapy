package com.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static Map<String, Object> successResponse(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", Map.of("Content-Type", "application/json"));
        try {
            response.put("body", MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            response.put("body", "{}");
        }
        return response;
    }
    
    public static Map<String, Object> parseBody(Map<String, Object> input) throws IOException {
        String requestBody = (String) input.get("body");
        return MAPPER.readValue(requestBody, Map.class);
    }
    
 // Add this method to ResponseUtils
    public static Map<String, Object> noContentResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 204);
        response.put("body", "");
        return response;
    }


    public static Map<String, Object> errorResponse(int statusCode, String message) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", message);
        
        try {
            return successResponse(statusCode, errorBody);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("statusCode", 500);
            fallback.put("body", "{\"error\":\"Critical serialization failure\"}");
            return fallback;
        }
    }
}
