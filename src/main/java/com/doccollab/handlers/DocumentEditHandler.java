package com.doccollab.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.doccollab.utils.ResponseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DocumentEditHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final S3Client s3Client = S3Client.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String documentsTable = System.getenv("DOCUMENTS_TABLE");
    private final String historyTable = System.getenv("DOCUMENT_HISTORY_TABLE");
    private final String s3Bucket = System.getenv("S3_BUCKET");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            // Get documentId from path parameters
            String documentId = request.getPathParameters().get("documentId");

            if (documentId == null || documentId.isEmpty()) {
                return ResponseUtil.createErrorResponse(400, "Document ID is required");
            }

            // Parse request body
            JsonNode requestBody = objectMapper.readTree(request.getBody());
            String newContent = requestBody.get("content").asText();
            String editedBy = requestBody.has("editedBy") ?
                    requestBody.get("editedBy").asText() : "anonymous";
            String changeDescription = requestBody.has("changeDescription") ?
                    requestBody.get("changeDescription").asText() : "Document updated";

            // Get current document from DynamoDB
            GetItemResponse getResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(documentsTable)
                    .key(Map.of("documentId", AttributeValue.builder().s(documentId).build()))
                    .build());

            if (!getResponse.hasItem()) {
                return ResponseUtil.createErrorResponse(404, "Document not found");
            }

            Map<String, AttributeValue> currentDoc = getResponse.item();
            int currentEditNumber = Integer.parseInt(currentDoc.get("currentEditNumber").n());
            int newEditNumber = currentEditNumber + 1;
            String timestamp = Instant.now().toString();

            // Save new version to S3
            String newVersionKey = "documents/" + documentId + "-v" + newEditNumber + ".txt";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(newVersionKey)
                            .build(),
                    RequestBody.fromString(newContent)
            );

            // Update document record in DynamoDB
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(documentsTable)
                    .key(Map.of("documentId", AttributeValue.builder().s(documentId).build()))
                    .updateExpression("SET currentFile = :newFile, currentEditNumber = :newEditNum, lastModified = :timestamp")
                    .expressionAttributeValues(Map.of(
                            ":newFile", AttributeValue.builder().s(newVersionKey).build(),
                            ":newEditNum", AttributeValue.builder().n(String.valueOf(newEditNumber)).build(),
                            ":timestamp", AttributeValue.builder().s(timestamp).build()
                    ))
                    .build());

            // Create history record
            Map<String, AttributeValue> historyItem = new HashMap<>();
            historyItem.put("documentId", AttributeValue.builder().s(documentId).build());
            historyItem.put("editNumber", AttributeValue.builder().n(String.valueOf(newEditNumber)).build());
            historyItem.put("filePath", AttributeValue.builder().s(newVersionKey).build());
            historyItem.put("editedAt", AttributeValue.builder().s(timestamp).build());
            historyItem.put("editedBy", AttributeValue.builder().s(editedBy).build());
            historyItem.put("changeDescription", AttributeValue.builder().s(changeDescription).build());

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(historyTable)
                    .item(historyItem)
                    .build());

            // Create success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("documentId", documentId);
            responseBody.put("message", "Document updated successfully");
            responseBody.put("editNumber", newEditNumber);
            responseBody.put("editedBy", editedBy);
            responseBody.put("editedAt", timestamp);

            return ResponseUtil.createResponse(200, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ResponseUtil.createErrorResponse(500, e.getMessage());
        }
    }
}