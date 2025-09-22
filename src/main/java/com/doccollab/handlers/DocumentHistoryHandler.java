package com.doccollab.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.doccollab.utils.ResponseUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentHistoryHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    private final String historyTable = System.getenv("DOCUMENT_HISTORY_TABLE");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            // Get documentId from path parameters
            String documentId = request.getPathParameters().get("documentId");

            if (documentId == null || documentId.isEmpty()) {
                return ResponseUtil.createErrorResponse(400, "Document ID is required");
            }

            // Query all history records for this document
            QueryResponse queryResponse = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(historyTable)
                    .keyConditionExpression("documentId = :docId")
                    .expressionAttributeValues(Map.of(
                            ":docId", AttributeValue.builder().s(documentId).build()
                    ))
                    .scanIndexForward(true) // Sort by editNumber ascending
                    .build());

            // Convert DynamoDB items to response format
            List<Map<String, Object>> historyList = new ArrayList<>();

            for (Map<String, AttributeValue> item : queryResponse.items()) {
                Map<String, Object> historyItem = new HashMap<>();
                historyItem.put("editNumber", Integer.parseInt(item.get("editNumber").n()));
                historyItem.put("editedAt", item.get("editedAt").s());
                historyItem.put("editedBy", item.get("editedBy").s());
                historyItem.put("changeDescription", item.get("changeDescription").s());
                historyItem.put("filePath", item.get("filePath").s());

                historyList.add(historyItem);
            }

            // Create response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("documentId", documentId);
            responseBody.put("totalEdits", historyList.size());
            responseBody.put("history", historyList);

            return ResponseUtil.createResponse(200, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ResponseUtil.createErrorResponse(500, e.getMessage());
        }
    }
}