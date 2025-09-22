package com.doccollab.utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(headers);

        try {
            response.setBody(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            response.setBody("{\"error\": \"Failed to serialize response\"}");
        }

        return response;
    }

    public static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("success", false);
        errorBody.put("error", message);
        return createResponse(statusCode, errorBody);
    }
}
